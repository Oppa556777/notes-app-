# SecureNotes 📒✨

A privacy-first, offline-first Android note-taking app inspired by Notesnook — built with
Jetpack Compose, Material 3 (Material You), and an encrypted-at-rest database.

> **Everything is encrypted on-device.** Notes live in a SQLCipher (AES-256) database whose
> passphrase is generated randomly and sealed by the Android Keystore. No accounts, no
> analytics, no trackers.

---

## ✨ Features

### 🔒 Security & privacy
- **AES-256 encrypted database** (SQLCipher) — the whole `.db` file is ciphertext at rest.
- **Android Keystore–backed master key** via `EncryptedSharedPreferences`.
- **App lock** with a 4-digit PIN hashed using PBKDF2-HMAC-SHA256 (120k iterations, per-PIN salt)
  and compared in constant time.
- **Biometric unlock** (fingerprint / face / device credential).
- **Auto-lock** — immediately, or after 30s / 1min / 5min in the background.
- **Private Vault** — a *second* PIN gating your most sensitive notes. Vault notes are excluded
  from search and lists until the vault is unlocked.
- Backups disabled (`allowBackup=false`) plus explicit data-extraction exclusion rules.

### ✏️ Rich editor
Markdown-powered editor with a scrollable rounded toolbar:
headings, **bold**, *italic*, __underline__, bullet & numbered lists, ☑️ checklists,
💻 fenced code blocks, 📊 tables, ∑ math blocks, quotes, dividers and 🔗 links.
Live **preview mode** renders all of it with a hand-written renderer (fast, no heavy deps).
Debounced autosave means you never lose a keystroke.

### 🔗 Link previews
- URLs are **auto-detected** as you type or paste.
- Metadata (OpenGraph / Twitter card / favicon) is fetched with Jsoup and **cached in the
  encrypted DB**, so previews render offline without re-fetching.
- Compact, extra-rounded preview cards: favicon, title, description, domain.
- Tap → in-app Custom Tab. Long-press → Copy link / Refresh / Remove preview / Share.
- Toggle **Settings → Editor → Auto link previews**; when off you get a
  "Show preview for this link?" snackbar instead.
- Note cards in the list show a `🔗 domain` hint.

### 🗂️ Organisation
Notebooks (with emoji icons), tags, 8 colour shortcuts, favourites ⭐, pins 📌,
reminders ⏰ (exact alarms + notifications), archive 🗄️ and trash 🗑️ with restore.

### 🎨 Design
- **Material 3 / Material You** with `dynamicLightColorScheme` / `dynamicDarkColorScheme`.
- **Light / Dark / System** theme modes. Dark mode is never pure black — tinted elevated surfaces.
- **Custom accent colours** — 8 presets + 10 custom swatches when dynamic colour is off.
  A full tonal scheme is derived from any seed colour.
- **Floating, blurry, semi-transparent navigation bar** — inset from the edges, extra-rounded,
  gradient "glass" fill with a highlight border and coloured shadow. Content scrolls behind it.
  Tablets get a matching **floating glass navigation rail**.
- **Onboarding** — 5 animated pages with gradient blobs, orbiting emoji accents and a
  morphing page indicator.
- Extra-rounded shapes, spring animations, haptics, emoji-led section headers and empty states.
- **System fonts only** (no bundled fonts) with Small / Medium / Large scaling.

### 🏗️ Architecture
MVVM + Clean Architecture, split into `data` / `domain` / `presentation`:

```
core/crypto      CryptoManager — DB passphrase, PIN hashing (PBKDF2)
core/security    AppLockManager, BiometricHelper
core/reminder    ReminderScheduler + BroadcastReceiver
data/local       Room entities, DAOs, converters, SQLCipher database
data/repository  NoteRepositoryImpl, NotebookRepositoryImpl, TagRepositoryImpl
data/link        LinkPreviewRepositoryImpl (Jsoup) + LinkDetector
data/preferences SettingsRepository (DataStore)
data/sync        SyncManager — E2EE cloud sync stub
domain/model     Note, Notebook, Tag, LinkPreview, AppSettings…
domain/repository  Repository interfaces
presentation/    Compose UI, ViewModels, theme, navigation
```

Hilt for DI · Coroutines + Flow throughout · Room · DataStore · Navigation Compose.

---

## 🛠️ Build & run

**Requirements:** JDK 17, Android SDK 35, internet access for Gradle/Maven.

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (signed)
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Or open the project in Android Studio (Ladybug or newer) and hit ▶.

- **minSdk 26** (Android 8.0) · **targetSdk / compileSdk 35** (Android 15)
- Kotlin 2.0.20 · AGP 8.5.2 · Compose BOM 2024.09.03

### 🔑 Signing a release APK

Without configuration, `assembleRelease` falls back to the debug keystore so the output is
always installable. To sign with your own key:

```bash
keytool -genkeypair -v \
  -keystore securenotes-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias securenotes \
  -storepass <STORE_PASSWORD> -keypass <KEY_PASSWORD> \
  -dname "CN=SecureNotes, O=SecureNotes, C=US"
```

Then create `keystore.properties` in the repo root:

```properties
storeFile=../securenotes-release.jks
storePassword=<STORE_PASSWORD>
keyAlias=securenotes
keyPassword=<KEY_PASSWORD>
```

`keystore.properties` and `*.jks` are gitignored — never commit them.

### ⚡ One-command build (recommended)

```bash
./build-apk.sh
```

This locates your Android SDK, generates a release keystore if you don't have one, writes
`keystore.properties`, and builds both APKs. Then install:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 🤖 Building the APK in CI (GitHub Actions)

A ready-to-use workflow lives at **`ci/build-apk.yml`**. It builds both APKs, generates a
keystore on the fly, and uploads them as downloadable artifacts.

> ⚠️ It is stored under `ci/` rather than `.github/workflows/` because the bot account that
> created this branch lacks the GitHub `workflows` permission. **To enable it, move the file
> and push** (one command, from your own account):
>
> ```bash
> mkdir -p .github/workflows
> git mv ci/build-apk.yml .github/workflows/build-apk.yml
> git commit -m "ci: enable APK build workflow"
> git push
> ```
>
> Then go to the **Actions** tab → *Build APK* → *Run workflow*, and download the
> `SecureNotes-release-apk` artifact when it finishes.

---

## 🎨 Theming & customisation notes

| What | Where |
|---|---|
| Colour schemes, seed→scheme derivation | `presentation/theme/Theme.kt` |
| Shapes (corner radii) | `SecureNotesShapes` in `Theme.kt` |
| Typography & font scaling | `secureNotesTypography()` in `Theme.kt` |
| Accent presets | `AccentColor` in `domain/model/Settings.kt` |
| Note colour shortcuts | `NoteColor` in `domain/model/Models.kt` |
| Floating nav bar / rail / glass effect | `presentation/components/FloatingNavigation.kt` |
| Onboarding pages & illustrations | `presentation/screens/onboarding/OnboardingScreen.kt` |

Note colours are intentionally independent of the app accent, so tinting a note never
clashes with your theme.

**Glass effect:** Compose's `Modifier.blur` only blurs a composable's *own* content rather than
the backdrop, so the nav bar uses a layered translucent gradient + highlight border + coloured
shadow. It reads as frosted glass, costs no GPU passes, and works identically on API 26–35
(it also opts into a higher alpha below API 31).

---

## 🔭 Cloud sync (stubbed)

`data/sync/SyncManager.kt` exposes a `SyncState` flow and a `syncNow()` entry point wired into
Settings. The transport is mocked — the real implementation would derive a sync key from the
user's password, encrypt each note locally, push ciphertext deltas, and merge remote changes
with last-write-wins plus conflict copies. Everything client-side is already in place.

---

## 📄 Licence

Built as a reference implementation — use it however you like.
