package com.rainbowcockroach.lifelog.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Classic" palette — a 1:1 port of the web app's Classic theme
 * (life-log-web/src/theming/themes/classic.ts). White paper, black ink and a
 * blue accent by day; GitHub-style warm-neutral dark by night. Keep these hex
 * values in sync with classic.ts — they are the single source of truth.
 */

// --- Light --------------------------------------------------------------------
val ClassicText = Color(0xFF000000)        // --color-text
val ClassicTextMuted = Color(0xFF777676)   // --color-text-muted
val ClassicTextFaint = Color(0xFFAAAAAA)   // --color-text-faint
val ClassicAccent = Color(0xFF3B82F6)      // --color-accent
val ClassicAccentOn = Color(0xFFFFFFFF)    // text/icons on the accent
val ClassicBackground = Color(0xFFFFFFFF)  // --color-background / --color-paper
val ClassicSurfaceVariant = Color(0xFFF2F2F2) // subtle fills: chips, selected rows
val ClassicBorder = Color(0xFFCCCCCC)      // --color-border
val ClassicError = Color(0xFFDC2626)       // --color-error

// --- Dark (GitHub-style) ------------------------------------------------------
val ClassicDarkText = Color(0xFFE6EDF3)
val ClassicDarkTextMuted = Color(0xFF8B949E)
val ClassicDarkTextFaint = Color(0xFF656D76)
val ClassicDarkAccent = Color(0xFF58A6FF)
val ClassicDarkAccentOn = Color(0xFF0D1117)
val ClassicDarkBackground = Color(0xFF0D1117) // --color-background / --color-paper
val ClassicDarkSurfaceVariant = Color(0xFF161B22) // subtle fills a hair above bg
val ClassicDarkBorder = Color(0xFF30363D)
val ClassicDarkError = Color(0xFFF85149)

/**
 * Parses a server-supplied hex color ("#RRGGBB" / "#AARRGGBB" / a named color) into a Compose
 * [Color], or null if it's blank/unparseable. Used to render tag chips in the tag's own colors.
 */
fun parseColorOrNull(value: String?): Color? {
    if (value.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(value.trim()))
    } catch (_: IllegalArgumentException) {
        null
    }
}
