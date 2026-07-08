package com.rainbowcockroach.lifelog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable theme preference, persisted in [SettingsStore]. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

private val ClassicLightColors = lightColorScheme(
    primary = ClassicAccent,
    onPrimary = ClassicAccentOn,
    primaryContainer = ClassicSurfaceVariant,
    onPrimaryContainer = ClassicText,
    secondary = ClassicTextMuted,
    onSecondary = ClassicAccentOn,
    secondaryContainer = ClassicSurfaceVariant,
    onSecondaryContainer = ClassicText,
    tertiary = ClassicAccent,
    onTertiary = ClassicAccentOn,
    tertiaryContainer = ClassicSurfaceVariant,
    onTertiaryContainer = ClassicText,
    background = ClassicBackground,
    onBackground = ClassicText,
    surface = ClassicBackground,
    onSurface = ClassicText,
    surfaceVariant = ClassicSurfaceVariant,
    onSurfaceVariant = ClassicTextMuted,
    surfaceContainerLowest = ClassicBackground,
    surfaceContainerLow = ClassicBackground,
    surfaceContainer = ClassicSurfaceVariant,
    surfaceContainerHigh = ClassicSurfaceVariant,
    surfaceContainerHighest = ClassicSurfaceVariant,
    outline = ClassicBorder,
    outlineVariant = ClassicBorder,
    error = ClassicError,
    onError = ClassicAccentOn,
)

private val ClassicDarkColors = darkColorScheme(
    primary = ClassicDarkAccent,
    onPrimary = ClassicDarkAccentOn,
    primaryContainer = ClassicDarkSurfaceVariant,
    onPrimaryContainer = ClassicDarkText,
    secondary = ClassicDarkTextMuted,
    onSecondary = ClassicDarkAccentOn,
    secondaryContainer = ClassicDarkSurfaceVariant,
    onSecondaryContainer = ClassicDarkText,
    tertiary = ClassicDarkAccent,
    onTertiary = ClassicDarkAccentOn,
    tertiaryContainer = ClassicDarkSurfaceVariant,
    onTertiaryContainer = ClassicDarkText,
    background = ClassicDarkBackground,
    onBackground = ClassicDarkText,
    surface = ClassicDarkBackground,
    onSurface = ClassicDarkText,
    surfaceVariant = ClassicDarkSurfaceVariant,
    onSurfaceVariant = ClassicDarkTextMuted,
    surfaceContainerLowest = ClassicDarkBackground,
    surfaceContainerLow = ClassicDarkBackground,
    surfaceContainer = ClassicDarkSurfaceVariant,
    surfaceContainerHigh = ClassicDarkSurfaceVariant,
    surfaceContainerHighest = ClassicDarkSurfaceVariant,
    outline = ClassicDarkBorder,
    outlineVariant = ClassicDarkBorder,
    error = ClassicDarkError,
    onError = ClassicDarkAccentOn,
)

@Composable
fun LifeLogTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) ClassicDarkColors else ClassicLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
