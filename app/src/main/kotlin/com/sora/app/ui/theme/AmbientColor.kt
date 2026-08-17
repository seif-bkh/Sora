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
    val clamped = clampLuminance(GLOW_MIN_LUMINANCE, GLOW_MAX_LUMINANCE)
    return clamped.copy(alpha = GLOW_ALPHA)
}

internal fun Color.clampForAccent(): Color =
    // An accent carries text and thin progress lines against [Ink], so it must
    // stay bright enough to hold contrast.
    clampLuminance(ACCENT_MIN_LUMINANCE, ACCENT_MAX_LUMINANCE)

/**
 * Brings luminance inside [[min], [max]], leaving colours already in range
 * untouched.
 *
 * Which bound was crossed decides which way the result must be rounded, and
 * that matters: sRGB [Color] is quantised to 8 bits per channel, so one step
 * is worth roughly 0.004 luminance — larger than any tolerance worth
 * asserting. Landing on the wrong side of a bound is what made the first fix
 * fail CI (see [adjustToLuminance]).
 */
private fun Color.clampLuminance(min: Float, max: Float): Color {
    val current = luminance()
    return when {
        current < min -> adjustToLuminance(min, atLeast = true)
        current > max -> adjustToLuminance(max, atLeast = false)
        else -> this
    }
}

/**
 * Drives a colour to a target relative luminance, preserving hue where it can.
 *
 * The obvious implementation — scale every channel by `target / current` and
 * clip to 1.0 — is wrong in two ways that only show up on real cover art, and
 * both were caught by AmbientColorTest rather than by eye:
 *
 *  1. **Clipping destroys hue.** A near-black cover (say `#010203`) needs a
 *     factor of ~460 to reach the floor; every channel clips to 1.0 and the
 *     "ambient" colour comes out pure white. The whole feature silently dies
 *     on exactly the dark covers where the glow matters most.
 *
 *  2. **Some hues cannot reach the target at all.** Luminance is weighted
 *     0.2126/0.7152/0.0722, so fully saturated blue peaks at **0.0722** — far
 *     below the 0.26 accent floor. No amount of scaling gets there, and the
 *     contrast guarantee quietly fails.
 *
 * So: binary-search the scale factor up to the point where the brightest
 * channel hits 1.0 (the most luminous version of this hue). If even that is
 * too dark, the hue physically cannot carry the target, and the only remedy is
 * to desaturate toward white — done by a second search so it desaturates as
 * little as the target demands, keeping as much of the hue as possible.
 *
 * A third subtlety, which also cost a CI round trip: sRGB [Color] holds 8 bits
 * per channel, so the search cannot land exactly on [target] — one channel
 * step is ~0.004 luminance. Returning the midpoint of the final interval can
 * therefore quantise to just the wrong side of the bound. [atLeast] says which
 * side is safe: the search maintains `lo` below the target and `hi` at or
 * above it, so returning the correct endpoint (rather than the midpoint) is
 * what makes the guarantee hold rather than nearly hold.
 *
 * ~40 iterations of cheap float maths, called once per cover and cached, which
 * is nothing next to the Palette pass that produced the input.
 *
 * @param atLeast true when raising to a floor (result must be >= [target]),
 *   false when lowering to a ceiling (result must be <= [target]).
 */
internal fun Color.adjustToLuminance(target: Float, atLeast: Boolean = true): Color {
    val current = luminance()
    val peak = max(red, max(green, blue))
    if (current <= 0f || peak <= 0f) {
        // Pure black carries no hue to preserve; fall back to the brand accent.
        return Accent
    }

    // Brightest this hue gets before any channel clips.
    val maxFactor = 1f / peak
    val brightest = scaleBy(maxFactor)

    return if (brightest.luminance() >= target) {
        // Reachable without clipping: find the factor. `lo` stays below the
        // target and `hi` at or above it, so the endpoint picked by [atLeast]
        // is guaranteed to be on the safe side of the bound.
        var lo = 0f
        var hi = maxFactor
        repeat(SEARCH_ITERATIONS) {
            val mid = (lo + hi) / 2f
            if (scaleBy(mid).luminance() < target) lo = mid else hi = mid
        }
        scaleBy(if (atLeast) hi else lo)
    } else {
        // Hue is too dark to reach the target at full brightness. Blend toward
        // white by the smallest amount that gets there. This branch only ever
        // runs to raise luminance, so `hi` is always the safe endpoint.
        var lo = 0f
        var hi = 1f
        repeat(SEARCH_ITERATIONS) {
            val mid = (lo + hi) / 2f
            if (brightest.blendToWhite(mid).luminance() < target) lo = mid else hi = mid
        }
        brightest.blendToWhite(hi)
    }
}

private fun Color.scaleBy(factor: Float): Color = Color(
    red = min(1f, red * factor),
    green = min(1f, green * factor),
    blue = min(1f, blue * factor),
    alpha = alpha,
)

private fun Color.blendToWhite(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

/** 20 halvings resolves the factor far below one 8-bit step. */
private const val SEARCH_ITERATIONS = 20

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
