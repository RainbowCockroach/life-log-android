package com.rainbowcockroach.lifelog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rainbowcockroach.lifelog.R

/**
 * Typography for the "Classic" look. The web Classic theme sets everything in
 * **Georgia** (`Georgia, 'Times New Roman', serif`). Georgia is a proprietary
 * Microsoft font we can't ship, so we bundle **Gelasio** — Google's
 * metric-compatible, glyph-for-glyph replacement for Georgia (SIL Open Font
 * License — files in res/font, license in assets/fonts/Gelasio-OFL.txt). Same
 * shapes and metrics as Georgia, so the app reads like the web Classic theme
 * fully offline.
 *
 * The bundled files are variable fonts (weight axis). Registering the same file at
 * several weights lets Compose drive the `wght` axis per style — supported on API 26+,
 * which matches our minSdk.
 */
private val Gelasio = FontFamily(
    Font(R.font.gelasio, FontWeight.Normal),
    Font(R.font.gelasio, FontWeight.Medium),
    Font(R.font.gelasio, FontWeight.SemiBold),
    Font(R.font.gelasio, FontWeight.Bold),
    Font(R.font.gelasio_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.gelasio_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.gelasio_italic, FontWeight.Bold, FontStyle.Italic),
)

private val base = Typography()

// Take Material 3's tuned type scale (sizes, weights, line heights) and swap the family.
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Gelasio),
    displayMedium = base.displayMedium.copy(fontFamily = Gelasio),
    displaySmall = base.displaySmall.copy(fontFamily = Gelasio),
    headlineLarge = base.headlineLarge.copy(fontFamily = Gelasio),
    headlineMedium = base.headlineMedium.copy(fontFamily = Gelasio),
    headlineSmall = base.headlineSmall.copy(fontFamily = Gelasio),
    titleLarge = base.titleLarge.copy(fontFamily = Gelasio),
    titleMedium = base.titleMedium.copy(fontFamily = Gelasio),
    titleSmall = base.titleSmall.copy(fontFamily = Gelasio),
    bodyLarge = base.bodyLarge.copy(fontFamily = Gelasio),
    bodyMedium = base.bodyMedium.copy(fontFamily = Gelasio),
    bodySmall = base.bodySmall.copy(fontFamily = Gelasio),
    labelLarge = base.labelLarge.copy(fontFamily = Gelasio),
    labelMedium = base.labelMedium.copy(fontFamily = Gelasio),
    labelSmall = base.labelSmall.copy(fontFamily = Gelasio),
)
