package com.rainbowcockroach.lifelog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rainbowcockroach.lifelog.R

/**
 * Typography for the "Novel" look. Everything is set in **EB Garamond**, a classic
 * book serif (SIL Open Font License — bundled in res/font, license in
 * assets/fonts/EBGaramond-OFL.txt), so the app reads like a printed page fully offline.
 *
 * The bundled files are variable fonts (weight axis). Registering the same file at
 * several weights lets Compose drive the `wght` axis per style — supported on API 26+,
 * which matches our minSdk.
 */
private val EbGaramond = FontFamily(
    Font(R.font.eb_garamond, FontWeight.Normal),
    Font(R.font.eb_garamond, FontWeight.Medium),
    Font(R.font.eb_garamond, FontWeight.SemiBold),
    Font(R.font.eb_garamond, FontWeight.Bold),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.eb_garamond_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.eb_garamond_italic, FontWeight.Bold, FontStyle.Italic),
)

private val base = Typography()

// Take Material 3's tuned type scale (sizes, weights, line heights) and swap the family.
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = EbGaramond),
    displayMedium = base.displayMedium.copy(fontFamily = EbGaramond),
    displaySmall = base.displaySmall.copy(fontFamily = EbGaramond),
    headlineLarge = base.headlineLarge.copy(fontFamily = EbGaramond),
    headlineMedium = base.headlineMedium.copy(fontFamily = EbGaramond),
    headlineSmall = base.headlineSmall.copy(fontFamily = EbGaramond),
    titleLarge = base.titleLarge.copy(fontFamily = EbGaramond),
    titleMedium = base.titleMedium.copy(fontFamily = EbGaramond),
    titleSmall = base.titleSmall.copy(fontFamily = EbGaramond),
    bodyLarge = base.bodyLarge.copy(fontFamily = EbGaramond),
    bodyMedium = base.bodyMedium.copy(fontFamily = EbGaramond),
    bodySmall = base.bodySmall.copy(fontFamily = EbGaramond),
    labelLarge = base.labelLarge.copy(fontFamily = EbGaramond),
    labelMedium = base.labelMedium.copy(fontFamily = EbGaramond),
    labelSmall = base.labelSmall.copy(fontFamily = EbGaramond),
)
