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

private val NovelLightColors = lightColorScheme(
    primary = InkBrown,
    onPrimary = InkBrownOn,
    secondary = PaperInkMuted,
    onSecondary = InkBrownOn,
    tertiary = InkBrown,
    onTertiary = InkBrownOn,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperBackground,
    onSurface = PaperInk,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperInkMuted,
    surfaceContainerLowest = PaperBackground,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurface,
    surfaceContainerHighest = PaperSurfaceVariant,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    error = NovelRed,
    onError = InkBrownOn,
)

private val NovelDarkColors = darkColorScheme(
    primary = NightAccent,
    onPrimary = NightAccentOn,
    secondary = NightInkMuted,
    onSecondary = NightAccentOn,
    tertiary = NightAccent,
    onTertiary = NightAccentOn,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightBackground,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightInkMuted,
    surfaceContainerLowest = NightBackground,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceVariant,
    surfaceContainerHighest = NightSurfaceVariant,
    outline = NightOutline,
    outlineVariant = NightOutline,
    error = NightRed,
    onError = NightAccentOn,
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
    val colorScheme = if (darkTheme) NovelDarkColors else NovelLightColors

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
