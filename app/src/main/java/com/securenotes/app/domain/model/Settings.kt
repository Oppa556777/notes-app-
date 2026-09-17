package com.securenotes.app.domain.model

enum class ThemeMode(val label: String, val emoji: String) {
    SYSTEM("System default", "🌗"),
    LIGHT("Light", "☀️"),
    DARK("Dark", "🌙"),
}

enum class FontScale(val label: String, val scale: Float) {
    SMALL("Small", 0.9f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f),
}

/** Preset accent colours used when dynamic colour is disabled. */
enum class AccentColor(val label: String, val emoji: String, val seed: Long) {
    INDIGO("Indigo", "🔵", 0xFF5468FF),
    PURPLE("Purple", "🟣", 0xFF6750A4),
    PINK("Pink", "🌸", 0xFFB3306B),
    TEAL("Teal", "🩵", 0xFF00696E),
    GREEN("Green", "🟢", 0xFF2E6B33),
    ORANGE("Orange", "🟠", 0xFF8B5000),
    RED("Red", "🔴", 0xFFB3261E),
    BLUE("Blue", "💙", 0xFF00639B);

    companion object {
        fun fromName(value: String?): AccentColor =
            entries.firstOrNull { it.name == value } ?: PURPLE
    }
}

enum class AppLockTimeout(val label: String, val millis: Long) {
    IMMEDIATELY("Immediately", 0L),
    THIRTY_SECONDS("After 30 seconds", 30_000L),
    ONE_MINUTE("After 1 minute", 60_000L),
    FIVE_MINUTES("After 5 minutes", 300_000L),
}

data class AppSettings(
    val onboardingComplete: Boolean = false,
    /** True once the user has either created a PIN or explicitly skipped setup. */
    val securitySetupDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentColor: AccentColor = AccentColor.PURPLE,
    val customAccentArgb: Long? = null,
    val fontScale: FontScale = FontScale.MEDIUM,
    val compactList: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.IMMEDIATELY,
    val vaultEnabled: Boolean = false,
    val autoLinkPreviews: Boolean = true,
    val spellCheck: Boolean = true,
    val markdownAssist: Boolean = true,
    val syncEnabled: Boolean = false,
    val lastSyncAt: Long = 0L,
)
