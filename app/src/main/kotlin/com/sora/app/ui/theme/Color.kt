package com.sora.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sora's colour tokens.
 *
 * Values come from the design showcase (`design/showcase/index.html`), which
 * is the canonical reference — see DESIGN.md §5. Two choices there are
 * deliberate and easy to "fix" by accident:
 *
 *  * [Paper] is a warm off-white, NOT pure #FFFFFF. Against near-black it
 *    reads as film rather than terminal, and lowers glare in a dark room.
 *  * [Ink] is near-black rather than a Material elevated grey, so cover art
 *    is the only light source on screen (and it is cheaper on OLED).
 */

// --- Surfaces --------------------------------------------------------------
/** App background. */
val Ink = Color(0xFF08090C)

/** Slightly raised surface (cards over the background). */
val Ink2 = Color(0xFF0B0D12)

/** Highest raised surface, used sparingly. */
val Ink3 = Color(0xFF11141B)

// --- Content ---------------------------------------------------------------
/** Primary text. Warm off-white — see the note above. */
val Paper = Color(0xFFE8E6DF)

/** Secondary text and inactive labels. */
val PaperMuted = Color(0xFFA8A69F)

// --- Accents ---------------------------------------------------------------
/** Brand seed, and the static fallback when ambient extraction is unavailable. */
val Accent = Color(0xFF4A90E2)

/** Lighter accent for links and small emphasis. */
val AccentSoft = Color(0xFF7FB0EE)

/**
 * Companion hue for ambient washes. Glows interpolate between [Accent] and
 * this rather than tinting a single hue, which is what stops extraction from
 * looking accidental.
 */
val AccentViolet = Color(0xFFB48CFF)

// --- Brand gradient (launcher icon, splash) --------------------------------
val SoraSkyTop = Color(0xFF5BA3D0)
val SoraSkyMid = Color(0xFF4A90E2)
val SoraSkyDeep = Color(0xFF2C5AA0)

// --- Semantic --------------------------------------------------------------
val ErrorRed = Color(0xFFFF6B6B)
val ErrorRedDark = Color(0xFF93000A)
val SuccessGreen = Color(0xFF5BD1A0)

// --- Hairlines -------------------------------------------------------------
/**
 * Separators are low-alpha white hairlines rather than Material elevation
 * shadows (DESIGN.md §5: "depth from glow and scrim").
 */
val Hairline = Color(0x14FFFFFF)      // ~8%
val HairlineStrong = Color(0x1AFFFFFF) // ~10%

// --- Light theme -----------------------------------------------------------
// The brief requires full light-theme support even though dark is the default.
// These are the inverse of the dark tokens rather than a separate design.
val LightSurface = Color(0xFFFDFCF9)
val LightSurfaceRaised = Color(0xFFF4F2EC)
val LightOnSurface = Color(0xFF1A1B1F)
val LightOnSurfaceMuted = Color(0xFF5A5B60)
val LightAccent = Color(0xFF2C5AA0)
val LightTertiary = Color(0xFF5B3E8F)
