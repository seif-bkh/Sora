package com.sora.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the accessibility clamp in AmbientColor.kt.
 *
 * DESIGN.md §6 makes one promise that cannot be verified by eye: whatever
 * colour a cover yields, the derived accent stays readable on the near-black
 * base. Cover art is arbitrary user content, so the only way to keep that
 * promise is to assert it over hostile inputs — pure white, pure black,
 * saturated primaries and flat greys.
 *
 * These call the production clamps directly (they are `internal` for exactly
 * this reason) so the assertions cannot drift away from the real arithmetic.
 */
class AmbientColorTest {

    // WCAG AA for large text and UI components. The accent carries progress
    // lines, small emphasis and the odd label, so this is the right bar.
    private val minimumContrast = 3.0f

    @Test
    fun `accent stays readable for hostile cover colours`() {
        val hostileColours = mapOf(
            "pure white" to Color.White,
            "pure black" to Color.Black,
            "near black" to Color(0xFF010203),
            "saturated red" to Color(0xFFFF0000),
            "saturated blue" to Color(0xFF0000FF),
            "saturated green" to Color(0xFF00FF00),
            "dark navy" to Color(0xFF06080F),
            "mid grey" to Color(0xFF808080),
            "cream" to Color(0xFFFFF8E1),
        )

        hostileColours.forEach { (name, raw) ->
            val ratio = contrastRatio(raw.clampForAccent(), Ink)
            assertTrue(
                "accent derived from $name has contrast $ratio against Ink, " +
                    "below the $minimumContrast floor",
                ratio >= minimumContrast,
            )
        }
    }

    @Test
    fun `accent luminance is forced into the readable band`() {
        // Below the floor: must be lifted or it disappears into the base.
        val fromDark = Color(0xFF101010).clampForAccent()
        assertTrue(
            "dark input produced luminance ${fromDark.luminance()}",
            fromDark.luminance() >= ACCENT_MIN_LUMINANCE - TOLERANCE,
        )

        // Above the ceiling: must be pulled down, or it glares on near-black.
        val fromWhite = Color.White.clampForAccent()
        assertTrue(
            "white input produced luminance ${fromWhite.luminance()}",
            fromWhite.luminance() <= ACCENT_MAX_LUMINANCE + TOLERANCE,
        )
    }

    @Test
    fun `clamp preserves hue rather than washing colour out`() {
        // The whole point of ambient colour is that a red cover feels red. A
        // clamp that flattened everything to the brand blue would pass every
        // contrast assertion above while destroying the feature.
        val fromRed = Color(0xFFCC2200).clampForAccent()
        assertTrue(
            "red input lost its dominant channel: $fromRed",
            fromRed.red > fromRed.blue && fromRed.red > fromRed.green,
        )

        val fromBlue = Color(0xFF1133CC).clampForAccent()
        assertTrue(
            "blue input lost its dominant channel: $fromBlue",
            fromBlue.blue > fromBlue.red,
        )
    }

    @Test
    fun `pure black falls back to the brand accent`() {
        // Black has no hue to preserve, and a zero luminance that would make
        // the scaling factor infinite.
        assertEquals(Accent, Color.Black.clampForAccent())
        assertEquals(Accent, Color.Black.adjustToLuminance(0.5f))
    }

    @Test
    fun `glow stays dark enough not to compete with artwork`() {
        val glow = Color.White.clampForGlow()
        assertTrue(
            "glow luminance ${glow.luminance()} exceeds the ceiling",
            glow.luminance() <= GLOW_MAX_LUMINANCE + TOLERANCE,
        )
        assertEquals("glow must stay translucent", GLOW_ALPHA, glow.alpha, TOLERANCE)
    }

    @Test
    fun `glow is lifted off the floor for near-black covers`() {
        // A glow that vanishes is as bad as one that glares: the hero would
        // lose its ambient halo entirely on dark covers.
        val glow = Color(0xFF010101).clampForGlow()
        assertTrue(
            "glow luminance ${glow.luminance()} is below the floor",
            glow.luminance() >= GLOW_MIN_LUMINANCE - TOLERANCE,
        )
    }

    @Test
    fun `contrast ratio helper matches known WCAG values`() {
        // Sanity-check the helper itself: white on black is exactly 21:1.
        assertEquals(21f, contrastRatio(Color.White, Color.Black), 0.01f)
        assertEquals(1f, contrastRatio(Color.White, Color.White), 0.01f)
        // Order must not matter.
        assertEquals(
            contrastRatio(Color.White, Ink),
            contrastRatio(Ink, Color.White),
            0.001f,
        )
    }

    @Test
    fun `brand fallback palette is readable`() {
        val ratio = contrastRatio(AmbientColors.Fallback.accent, Ink)
        assertTrue("brand fallback accent contrast is $ratio", ratio >= minimumContrast)
    }

    @Test
    fun `paper on ink clears the body-text bar`() {
        // The static pair every screen relies on; 4.5:1 is the AA bar for
        // normal-size text.
        val ratio = contrastRatio(Paper, Ink)
        assertTrue("Paper on Ink is only $ratio:1", ratio >= 4.5f)
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
