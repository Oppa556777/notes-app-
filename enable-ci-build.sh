#!/usr/bin/env bash
#
# Enables the GitHub Actions workflow that builds the signed APK and publishes
# it as a GitHub Release (giving you a direct, shareable download link).
#
# Why this script exists: the bot account that created this branch does not have
# GitHub's "workflows" permission, so it is not allowed to push files into
# .github/workflows/. Running this from your own account does it in one step.
#
# Usage:  ./enable-ci-build.sh
#
set -euo pipefail

cd "$(dirname "$0")"

BRANCH="$(git rev-parse --abbrev-ref HEAD)"

if [[ ! -f ci/build-apk.yml ]]; then
  echo "❌ ci/build-apk.yml not found — are you in the repo root?" >&2
  exit 1
fi

mkdir -p .github/workflows
cp ci/build-apk.yml .github/workflows/build-apk.yml
git add .github/workflows/build-apk.yml

if git diff --cached --quiet; then
  echo "ℹ️  Workflow already enabled — nothing to commit."
else
  git commit -m "ci: enable APK build workflow"
fi

echo "⬆️  Pushing to $BRANCH…"
git push origin "$BRANCH"

REMOTE="$(git config --get remote.origin.url)"
SLUG="$(sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##' <<<"$REMOTE")"

cat <<EOF

✅ Workflow enabled and pushed.

The build starts automatically. Watch it here:
  https://github.com/$SLUG/actions

When it finishes (~5-10 min), your APK will be published as a Release with a
direct download link:
  https://github.com/$SLUG/releases/latest

Open that link on your Android phone and tap SecureNotes-release.apk to install.
EOF
