package com.securenotes.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.securenotes.app.domain.model.AccentColor
import com.securenotes.app.domain.model.AppLockTimeout
import com.securenotes.app.domain.model.AppSettings
import com.securenotes.app.domain.model.FontScale
import com.securenotes.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "securenotes_settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val SECURITY_SETUP_DONE = booleanPreferencesKey("security_setup_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT = stringPreferencesKey("accent_color")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val COMPACT = booleanPreferencesKey("compact_list")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        val VAULT = booleanPreferencesKey("vault_enabled")
        val AUTO_PREVIEW = booleanPreferencesKey("auto_link_previews")
        val SPELL_CHECK = booleanPreferencesKey("spell_check")
        val MARKDOWN = booleanPreferencesKey("markdown_assist")
        val SYNC = booleanPreferencesKey("sync_enabled")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
            securitySetupDone = prefs[Keys.SECURITY_SETUP_DONE] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            accentColor = AccentColor.fromName(prefs[Keys.ACCENT]),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT],
            fontScale = runCatching { FontScale.valueOf(prefs[Keys.FONT_SCALE] ?: "") }
                .getOrDefault(FontScale.MEDIUM),
            compactList = prefs[Keys.COMPACT] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            appLockTimeout = runCatching { AppLockTimeout.valueOf(prefs[Keys.LOCK_TIMEOUT] ?: "") }
                .getOrDefault(AppLockTimeout.IMMEDIATELY),
            vaultEnabled = prefs[Keys.VAULT] ?: false,
            autoLinkPreviews = prefs[Keys.AUTO_PREVIEW] ?: true,
            spellCheck = prefs[Keys.SPELL_CHECK] ?: true,
            markdownAssist = prefs[Keys.MARKDOWN] ?: true,
            syncEnabled = prefs[Keys.SYNC] ?: false,
            lastSyncAt = prefs[Keys.LAST_SYNC] ?: 0L,
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) = edit { it[Keys.ONBOARDING] = value }
    suspend fun setSecuritySetupDone(value: Boolean) = edit { it[Keys.SECURITY_SETUP_DONE] = value }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setAccentColor(accent: AccentColor) = edit {
        it[Keys.ACCENT] = accent.name
        it.remove(Keys.CUSTOM_ACCENT)
    }

    suspend fun setCustomAccent(argb: Long) = edit { it[Keys.CUSTOM_ACCENT] = argb }
    suspend fun setFontScale(scale: FontScale) = edit { it[Keys.FONT_SCALE] = scale.name }
    suspend fun setCompactList(value: Boolean) = edit { it[Keys.COMPACT] = value }
    suspend fun setAppLockEnabled(value: Boolean) = edit { it[Keys.APP_LOCK] = value }
    suspend fun setBiometricEnabled(value: Boolean) = edit { it[Keys.BIOMETRIC] = value }
    suspend fun setLockTimeout(value: AppLockTimeout) = edit { it[Keys.LOCK_TIMEOUT] = value.name }
    suspend fun setVaultEnabled(value: Boolean) = edit { it[Keys.VAULT] = value }
    suspend fun setAutoLinkPreviews(value: Boolean) = edit { it[Keys.AUTO_PREVIEW] = value }
    suspend fun setSpellCheck(value: Boolean) = edit { it[Keys.SPELL_CHECK] = value }
    suspend fun setMarkdownAssist(value: Boolean) = edit { it[Keys.MARKDOWN] = value }
    suspend fun setSyncEnabled(value: Boolean) = edit { it[Keys.SYNC] = value }
    suspend fun setLastSync(value: Long) = edit { it[Keys.LAST_SYNC] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
