package com.securenotes.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.securenotes.app.domain.model.AppSettings
import com.securenotes.app.domain.model.FontScale
import com.securenotes.app.domain.model.ThemeMode

/** Exposes the live settings to any composable that needs them (font scale, compact list…). */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/**
 * Material 3 shapes — deliberately extra-rounded to match the soft,
 * friendly "cute" design language.
 */
private val SecureNotesShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

@Composable
fun SecureNotesTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme: ColorScheme = when {
        settings.dynamicColor && supportsDynamic ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> {
            val seed = settings.customAccentArgb?.let { Color(it.toInt()) }
                ?: Color(settings.accentColor.seed.toInt())
            buildSchemeFromSeed(seed, darkTheme)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = secureNotesTypography(settings.fontScale),
        shapes = SecureNotesShapes,
        content = content,
    )
}

/**
 * Derives a full tonal-ish Material 3 scheme from a single seed colour.
 * Keeps the app dependency-free while still giving a cohesive palette for
 * every preset/custom accent.
 */
private fun buildSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    fun Color.lighten(fraction: Float) = Color(
        red = red + (1f - red) * fraction,
        green = green + (1f - green) * fraction,
        blue = blue + (1f - blue) * fraction,
        alpha = alpha,
    )

    fun Color.darken(fraction: Float) = Color(
        red = red * (1f - fraction),
        green = green * (1f - fraction),
        blue = blue * (1f - fraction),
        alpha = alpha,
    )

    fun Color.onColor(): Color = if (luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White

    return if (dark) {
        val primary = seed.lighten(0.45f)
        val secondary = seed.lighten(0.3f)
        val tertiary = seed.lighten(0.2f)
        // Dark mode intentionally avoids pure black: elevated, slightly tinted surfaces.
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF1B1B1F),
            primaryContainer = seed.darken(0.25f),
            onPrimaryContainer = primary.lighten(0.5f),
            secondary = secondary,
            onSecondary = Color(0xFF1B1B1F),
            secondaryContainer = seed.darken(0.4f),
            onSecondaryContainer = secondary.lighten(0.6f),
            tertiary = tertiary,
            onTertiary = Color(0xFF1B1B1F),
            tertiaryContainer = seed.darken(0.35f),
            onTertiaryContainer = tertiary.lighten(0.55f),
            background = Color(0xFF131218),
            onBackground = Color(0xFFE6E1E9),
            surface = Color(0xFF131218),
            onSurface = Color(0xFFE6E1E9),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            surfaceContainerLowest = Color(0xFF0E0D13),
            surfaceContainerLow = Color(0xFF1B1A20),
            surfaceContainer = Color(0xFF201F25),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B),
            outline = Color(0xFF938F99),
            outlineVariant = Color(0xFF49454F),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
        )
    } else {
        val primary = seed
        val secondary = seed.lighten(0.15f).darken(0.05f)
        val tertiary = seed.lighten(0.25f).darken(0.1f)
        lightColorScheme(
            primary = primary,
            onPrimary = primary.onColor(),
            primaryContainer = seed.lighten(0.82f),
            onPrimaryContainer = seed.darken(0.55f),
            secondary = secondary,
            onSecondary = secondary.onColor(),
            secondaryContainer = seed.lighten(0.88f),
            onSecondaryContainer = seed.darken(0.5f),
            tertiary = tertiary,
            onTertiary = tertiary.onColor(),
            tertiaryContainer = seed.lighten(0.85f),
            onTertiaryContainer = seed.darken(0.5f),
            background = Color(0xFFFDFBFF),
            onBackground = Color(0xFF1B1B1F),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFE7E0EB),
            onSurfaceVariant = Color(0xFF49454F),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
            outline = Color(0xFF79747E),
            outlineVariant = Color(0xFFCAC4D0),
            error = Color(0xFFB3261E),
            onError = Color.White,
        )
    }
}

/**
 * System font family only (Roboto / Google Sans on most devices) — nothing is
 * bundled, so the APK stays small and text matches the user's device.
 */
private fun secureNotesTypography(fontScale: FontScale): Typography {
    val s = fontScale.scale
    fun style(
        size: Float,
        lineHeight: Float,
        weight: FontWeight,
        letterSpacing: Float = 0f,
    ) = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = weight,
        fontSize = (size * s).sp,
        lineHeight = (lineHeight * s).sp,
        letterSpacing = letterSpacing.sp,
    )

    return Typography(
        displayLarge = style(57f, 64f, FontWeight.Bold, (-0.25f)),
        displayMedium = style(45f, 52f, FontWeight.Bold),
        displaySmall = style(36f, 44f, FontWeight.Bold),
        headlineLarge = style(32f, 40f, FontWeight.Bold),
        headlineMedium = style(28f, 36f, FontWeight.Bold),
        headlineSmall = style(24f, 32f, FontWeight.SemiBold),
        titleLarge = style(22f, 28f, FontWeight.SemiBold),
        titleMedium = style(16f, 24f, FontWeight.SemiBold, 0.15f),
        titleSmall = style(14f, 20f, FontWeight.Medium, 0.1f),
        bodyLarge = style(16f, 26f, FontWeight.Normal, 0.15f),
        bodyMedium = style(14f, 22f, FontWeight.Normal, 0.25f),
        bodySmall = style(12f, 18f, FontWeight.Normal, 0.4f),
        labelLarge = style(14f, 20f, FontWeight.SemiBold, 0.1f),
        labelMedium = style(12f, 16f, FontWeight.Medium, 0.5f),
        labelSmall = style(11f, 16f, FontWeight.Medium, 0.5f),
    )
}
