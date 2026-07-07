package com.rainbowcockroach.lifelog.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Novel" palette — a book-on-paper look inspired by the Ghostty/iTerm2 "Novel" theme
 * (background #dfdbc3, ink #3b2322). The light scheme is aged cream paper with sepia ink;
 * the dark scheme is a warm "night reading" variant (dark cocoa paper, warm cream ink)
 * rather than a cold black, so it still reads like a printed page after dark.
 */

// --- Light (paper) -------------------------------------------------------------
val PaperBackground = Color(0xFFDFDBC3) // aged cream — the Novel background
val PaperSurface = Color(0xFFE7E3CE)    // a shade lighter for cards/sheets/toolbars
val PaperSurfaceVariant = Color(0xFFD2CDB2)
val PaperInk = Color(0xFF3B2322)        // dark sepia-brown — the Novel foreground
val PaperInkMuted = Color(0xFF6E5A4E)   // secondary text / captions
val InkBrown = Color(0xFF6F4E37)        // coffee-brown accent (buttons, cursor, links)
val InkBrownOn = Color(0xFFF4F0DD)      // text/icons drawn on InkBrown
val PaperOutline = Color(0xFFA4A390)    // Novel selection grey — hairlines & borders
val NovelRed = Color(0xFFCC0000)        // Novel palette red — errors

// --- Dark (night reading) ------------------------------------------------------
val NightBackground = Color(0xFF1E1A17) // deep warm cocoa
val NightSurface = Color(0xFF272220)
val NightSurfaceVariant = Color(0xFF342D28)
val NightInk = Color(0xFFDCD5C1)        // warm cream ink
val NightInkMuted = Color(0xFFA89A85)
val NightAccent = Color(0xFFC7A66B)     // warm candlelit tan
val NightAccentOn = Color(0xFF2A241E)
val NightOutline = Color(0xFF4C443A)
val NightRed = Color(0xFFE08585)
