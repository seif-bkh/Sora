package com.sora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.sora.app.R

/**
 * Sora's type system.
 *
 * Deliberate two-family contrast (DESIGN.md §5): single-family Material type
 * is part of the generic look this redesign exists to escape.
 *
 *  * [DisplayFamily] — Cormorant Garamond Light. Bundled, 47 KB: the upstream
 *    variable font is instanced to wght=300 and subset to Latin by
 *    `tools/fonts/build_fonts.py`. Bundling (rather than relying on the
 *    platform serif) guarantees identical rendering across OEMs.
 *
 *    It contains NO CJK. The 空 in the wordmark resolves from the platform
 *    serif through Android's font-fallback chain automatically — this is
 *    intended, not an oversight. Bundling a CJK serif would cost megabytes.
 *
 *  * [BodyFamily] — the platform sans. Small, quiet, and free.
 *
 *  * [NumeralFamily] — monospace for figures. This is functional, not
 *    decorative: proportional digits make a progress counter like "142/310"
 *    visibly jitter as the number changes, which is unacceptable in a
 *    progress-first UI. Monospace gives tabular figures for free.
 */

val DisplayFamily = FontFamily(
    Font(R.font.cormorant_garamond_light, FontWeight.Light),
)

val BodyFamily = FontFamily.SansSerif

/**
 * `FontFamily.Monospace` maps to the platform monospace face, which has
 * tabular figures by definition. Avoids bundling a second font purely for
 * digits.
 */
val NumeralFamily = FontFamily.Monospace

/**
 * Trims the extra leading Compose adds above the first line and below the
 * last. Without this, the large display sizes below sit visually low inside
 * their own bounds and the tight layouts in the spec do not line up.
 */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

val SoraTypography = Typography(
    // --- Display: series titles, hero headings ---------------------------
    // Large and light. A series title at this size is a deliberate statement
    // that content outranks chrome.
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = TrimmedLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 45.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.25).sp,
        lineHeightStyle = TrimmedLineHeight,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        lineHeightStyle = TrimmedLineHeight,
    ),

    // --- Headline: section headings ("Continue watching") ----------------
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        lineHeightStyle = TrimmedLineHeight,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        lineHeightStyle = TrimmedLineHeight,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        lineHeightStyle = TrimmedLineHeight,
    ),

    // --- Title: card titles, list rows -----------------------------------
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // --- Body -------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // --- Label: buttons and chips ----------------------------------------
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Micro-label: uppercase, widely tracked, quiet. The showcase's signature
 * detail — "CONTINUE WATCHING", "SEASON 2". Callers must uppercase the text
 * themselves; Compose has no text-transform.
 */
val MicroLabel = TextStyle(
    fontFamily = BodyFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    // ~0.22em at 10sp, matching the showcase's tracking.
    letterSpacing = 2.2.sp,
)

/**
 * Numerals: episode numbers, "142/310", timestamps. Tabular figures stop the
 * layout shifting as digits change.
 */
val NumeralStyle = TextStyle(
    fontFamily = NumeralFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
)
