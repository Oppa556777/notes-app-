@file:OptIn(ExperimentalLayoutApi::class)

package com.securenotes.app.presentation.screens.settings

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenotes.app.BuildConfig
import com.securenotes.app.data.sync.SyncState
import com.securenotes.app.domain.model.AccentColor
import com.securenotes.app.domain.model.AppLockTimeout
import com.securenotes.app.domain.model.FontScale
import com.securenotes.app.domain.model.ThemeMode
import com.securenotes.app.presentation.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onChangePin: () -> Unit,
    onSetupVault: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings ⚙️",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(bottom = 150.dp),
        ) {
            // ------------------------------------------------------ Appearance ---
            SectionHeader("🎨 Appearance")
            SettingsCard {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            label = { Text("${mode.emoji} ${mode.label.substringBefore(' ')}") },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SwitchRow(
                    title = "Use dynamic colour",
                    subtitle = "Material You — match your wallpaper (Android 12+)",
                    checked = settings.dynamicColor,
                    onChange = viewModel::setDynamicColor,
                )

                if (!settings.dynamicColor) {
                    Spacer(Modifier.height(14.dp))
                    Text("Accent colour", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentColor.entries.forEach { accent ->
                            val selected = settings.customAccentArgb == null &&
                                settings.accentColor == accent
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.seed.toInt()))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    )
                                    .clickable { viewModel.setAccent(accent) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Custom colour",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    CustomColorRow(
                        selected = settings.customAccentArgb,
                        onPick = viewModel::setCustomAccent,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Text size", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontScale.entries.forEach { scale ->
                        FilterChip(
                            selected = settings.fontScale == scale,
                            onClick = { viewModel.setFontScale(scale) },
                            label = { Text(scale.label) },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    title = "Compact list",
                    subtitle = "Hide note previews to fit more on screen",
                    checked = settings.compactList,
                    onChange = viewModel::setCompactList,
                )
            }

            // -------------------------------------------------------- Security ---
            SectionHeader("🔒 Security")
            SettingsCard {
                SwitchRow(
                    title = "App lock",
                    subtitle = "Require a PIN every time you open SecureNotes",
                    checked = settings.appLockEnabled,
                    onChange = { enabled ->
                        if (enabled) onChangePin() else viewModel.setAppLock(false)
                    },
                )
                if (settings.appLockEnabled) {
                    Spacer(Modifier.height(10.dp))
                    SwitchRow(
                        title = "Biometric unlock",
                        subtitle = "Use fingerprint or face instead of the PIN",
                        checked = settings.biometricEnabled,
                        onChange = viewModel::setBiometric,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Auto-lock", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLockTimeout.entries.forEach { timeout ->
                            FilterChip(
                                selected = settings.appLockTimeout == timeout,
                                onClick = { viewModel.setLockTimeout(timeout) },
                                label = { Text(timeout.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onChangePin) { Text("🔑 Change PIN") }
                }

                Spacer(Modifier.height(10.dp))
                SwitchRow(
                    title = "Private Vault",
                    subtitle = "A second password for your most sensitive notes",
                    checked = settings.vaultEnabled,
                    onChange = { enabled ->
                        if (enabled) onSetupVault() else viewModel.disableVault()
                    },
                )

                Spacer(Modifier.height(10.dp))
                InfoRow(
                    "🛡️ Encryption",
                    "AES-256 SQLCipher database + Android Keystore master key. " +
                        "Your notes never leave the device unencrypted.",
                )
            }

            // ---------------------------------------------------------- Editor ---
            SectionHeader("✏️ Editor")
            SettingsCard {
                SwitchRow(
                    title = "Auto link previews",
                    subtitle = "Fetch a preview card whenever you paste a URL",
                    checked = settings.autoLinkPreviews,
                    onChange = viewModel::setAutoLinkPreviews,
                )
                Spacer(Modifier.height(10.dp))
                SwitchRow(
                    title = "Spell check",
                    subtitle = "Underline misspelled words while typing",
                    checked = settings.spellCheck,
                    onChange = viewModel::setSpellCheck,
                )
                Spacer(Modifier.height(10.dp))
                SwitchRow(
                    title = "Markdown assist",
                    subtitle = "Continue lists and checkboxes automatically",
                    checked = settings.markdownAssist,
                    onChange = viewModel::setMarkdownAssist,
                )
            }

            // --------------------------------------------------- Data and sync ---
            SectionHeader("🗂️ Data & Sync")
            SettingsCard {
                SwitchRow(
                    title = "Cloud sync",
                    subtitle = "End-to-end encrypted sync across your devices",
                    checked = settings.syncEnabled,
                    onChange = viewModel::setSyncEnabled,
                )
                if (settings.syncEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = viewModel::syncNow) { Text("🔄 Sync now") }
                        Text(
                            text = when (val s = syncState) {
                                is SyncState.Syncing -> "Syncing…"
                                is SyncState.Success -> "Last synced just now ✅"
                                is SyncState.Failed -> "Failed: ${s.reason}"
                                SyncState.Idle -> "Not synced yet"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    InfoRow(
                        "ℹ️ Preview",
                        "Sync is architecturally wired but runs against a local stub in this build.",
                    )
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = viewModel::emptyTrash) { Text("🗑️ Empty trash") }
            }

            // ----------------------------------------------------------- About ---
            SectionHeader("ℹ️ About")
            SettingsCard {
                InfoRow("SecureNotes 📒", "Version ${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    "Privacy first",
                    "No analytics, no trackers, no account required. Offline-first by design.",
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("Made with", "Jetpack Compose + Material 3 ✨")
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A small hand-picked palette for the "custom colour" option. */
@Composable
private fun CustomColorRow(selected: Long?, onPick: (Long) -> Unit) {
    val swatches = listOf(
        0xFF1B998B, 0xFFED217C, 0xFF2D3047, 0xFFFF9B71, 0xFF7D5BA6,
        0xFF3A86FF, 0xFFFB5607, 0xFF06D6A0, 0xFFEF476F, 0xFF118AB2,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { argb ->
            val isSelected = selected == argb
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(argb.toInt()))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    )
                    .clickable { onPick(argb) },
            )
        }
    }
}
