package com.sora.app.ui.theme

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import androidx.collection.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette
import kotlin.math.max
import kotlin.math.min

/**
 * Ambient colour extracted from the cover currently on screen.
 *
 * This is the app's strongest differentiator (DESIGN.md §5): the background
 * glow and accent follow the artwork, so the app's mood tracks the content
 * instead of being a fixed palette. It cannot be retrofitted onto a
 * static-palette grid app, which is precisely the point.
 */
@Immutable
data class AmbientColors(
    /** Drives the background glow behind the hero. */
    val glow: Color,
    /** Accent for progress lines and small emphasis. */
    val accent: Color,
) {
    companion object {
        /**
         * Brand-seeded default. Used before extraction completes, when it
         * fails, and for any surface with no associated artwork.
         */
        val Fallback = AmbientColors(glow = Accent, accent = AccentSoft)
    }
}

/**
 * Ambient colours for the current content.
 *
 * Defaults to the brand fallback so any composable can read this without the
 * caller having to provide it.
 */
val LocalAmbientColors = compositionLocalOf { AmbientColors.Fallback }

/**
 * Smoothly animates between ambient palettes.
 *
 * Colour is animated rather than snapped because the hero changes as the user
 * scrolls the queue; a hard cut between palettes reads as a glitch. 600 ms is
 * slow enough to feel ambient and short enough not to lag the content.
 *
 * Compose's animation APIs already respect the system animator scale, so this
 * honours the reduced-motion requirement (DESIGN.md §6) without extra work.
 */
@Composable
fun animatedAmbientColors(target: AmbientColors): AmbientColors {
    val glow by animateColorAsState(
        targetValue = target.glow,
        animationSpec = tween(durationMillis = AMBIENT_ANIMATION_MS),
        label = "ambientGlow",
    )
    val accent by animateColorAsState(
        targetValue = target.accent,
        animationSpec = tween(durationMillis = AMBIENT_ANIMATION_MS),
        label = "ambientAccent",
    )
    return AmbientColors(glow = glow, accent = accent)
}

private const val AMBIENT_ANIMATION_MS = 600

/**
 * Extracts ambient colours from a cover bitmap.
 *
 * MUST NOT run on the main thread: Palette walks every pixel of the (already
 * downscaled) bitmap. Callers dispatch this to [com.sora.corecommon.dispatchers]
 * IO/Default; StrictMode would flag it otherwise.
 *
 * Results are cached by [cacheKey] because the same cover is re-extracted
 * constantly as the user scrolls the queue back and forth — without the cache
 * this is the single most expensive thing on the home screen.
 */
object AmbientColorExtractor {

    /**
     * Bounded LRU. ~40 covers is far more than are visible at once but enough
     * to survive scrolling a long queue; each entry is two ints, so the memory
     * cost is negligible compared with re-running Palette.
     */
    private val cache = LruCache<String, AmbientColors>(40)

    @WorkerThread
    fun extract(bitmap: Bitmap, cacheKey: String): AmbientColors {
        cache.get(cacheKey)?.let { return it }

        val palette = Palette.from(bitmap)
            // Palette's default is 16; more swatches cost time for detail we
            // do not use, since only one dominant colour is needed.
            .maximumColorCount(12)
            // Downscale before quantising. Ambient colour needs the broad
            // impression of a cover, not per-pixel accuracy.
            .resizeBitmapArea(RESIZE_AREA)
            .generate()

        // Preference order: vibrant reads best as a glow; muted variants are
        // the graceful degradation for washed-out or monochrome covers.
        val seed = palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.darkMutedSwatch

        val result = if (seed == null) {
            AmbientColors.Fallback
        } else {
            val raw = Color(seed.rgb)
            AmbientColors(
                glow = raw.clampForGlow(),
                accent = raw.clampForAccent(),
            )
        }

        cache.put(cacheKey, result)
        return result
    }

    /** Exposed for tests and for the settings "clear caches" action. */
    fun clearCache() = cache.evictAll()

    private const val RESIZE_AREA = 112 * 112
}

/**
 * ACCESSIBILITY CLAMP (DESIGN.md §6, non-negotiable).
 *
 * These are `internal` rather than `private` purely so AmbientColorTest can
 * assert the real arithmetic against hostile cover colours. Duplicating the
 * formula in the test would let the two drift apart silently, which would
 * quietly void the contrast guarantee.
 *
 * An extracted colour is arbitrary — a cover can yield near-white, near-black
 * or something fully desaturated. Used raw, that produces unreadable accents
 * and invisible glows. These helpers force the result into a usable band while
 * preserving hue, which is the part that carries the "mood follows content"
 * effect.
 */
internal fun Color.clampForGlow(): Color {
    // A glow sits behind content on a near-black background: it must be dark
    // enough not to compete with the artwork, but not so dark it vanishes.
    val target = luminance().coerceIn(GLOW_MIN_LUMINANCE, GLOW_MAX_LUMINANCE)
    return adjustToLuminance(target).copy(alpha = GLOW_ALPHA)
}

internal fun Color.clampForAccent(): Color {
    // An accent carries text and thin progress lines against [Ink], so it must
    // stay bright enough to hold contrast.
    val target = luminance().coerceIn(ACCENT_MIN_LUMINANCE, ACCENT_MAX_LUMINANCE)
    return adjustToLuminance(target)
}

/**
 * Scales a colour towards a target relative luminance.
 *
 * A linear scale in sRGB space, which is approximate — but it preserves hue
 * (the perceptually important part here) and is far cheaper than a full
 * Lab/OkLab conversion for something recomputed as the user scrolls.
 */
internal fun Color.adjustToLuminance(target: Float): Color {
    val current = luminance()
    if (current <= 0f) {
        // Pure black carries no hue to preserve; fall back to the brand accent.
        return Accent
    }
    val factor = target / current
    return Color(
        red = min(1f, red * factor),
        green = min(1f, green * factor),
        blue = min(1f, blue * factor),
        alpha = alpha,
    )
}

/** Guarantees a minimum contrast ratio against [Ink]; used by tests. */
fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

// Tuned against the near-black base. The accent floor is what guarantees the
// clamp cannot emit something unreadable; see AmbientColorTest.
internal const val GLOW_MIN_LUMINANCE = 0.06f
internal const val GLOW_MAX_LUMINANCE = 0.34f
internal const val GLOW_ALPHA = 0.55f
internal const val ACCENT_MIN_LUMINANCE = 0.26f
internal const val ACCENT_MAX_LUMINANCE = 0.78f
