package com.sora.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Shapes (DESIGN.md §5): cards 16dp, hero 24dp.
 */
val SoraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),      // cards
    extraLarge = RoundedCornerShape(24.dp), // hero
)

/**
 * Dark scheme — the app default.
 *
 * Every surface role maps to the same near-black [Ink] rather than Material's
 * ascending grey elevations. Depth comes from glow and hairline borders
 * (DESIGN.md §5), so tonal surfaces would only mute the artwork.
 */
private val SoraDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    primaryContainer = Ink3,
    onPrimaryContainer = Paper,

    secondary = AccentSoft,
    onSecondary = Ink,
    secondaryContainer = Ink3,
    onSecondaryContainer = Paper,

    tertiary = AccentViolet,
    onTertiary = Ink,
    tertiaryContainer = Ink3,
    onTertiaryContainer = Paper,

    background = Ink,
    onBackground = Paper,

    surface = Ink,
    onSurface = Paper,
    surfaceVariant = Ink2,
    onSurfaceVariant = PaperMuted,

    // Flat by design: see the note above.
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Ink,
    surfaceContainer = Ink2,
    surfaceContainerHigh = Ink2,
    surfaceContainerHighest = Ink3,

    outline = HairlineStrong,
    outlineVariant = Hairline,

    error = ErrorRed,
    onError = Ink,
    errorContainer = ErrorRedDark,
    onErrorContainer = Paper,
)

/**
 * Light scheme.
 *
 * Required by the brief even though dark is the default. Deliberately warm
 * (paper-like) rather than clinical white, so the two themes feel related.
 */
private val SoraLightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceRaised,
    onPrimaryContainer = LightOnSurface,

    secondary = LightAccent,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceRaised,
    onSecondaryContainer = LightOnSurface,

    tertiary = LightTertiary,
    onTertiary = LightSurface,

    background = LightSurface,
    onBackground = LightOnSurface,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightOnSurfaceMuted,

    outline = Color(0x22000000),
    outlineVariant = Color(0x14000000),
)

/**
 * Sora's Material 3 theme.
 *
 * @param darkTheme dark by default regardless of the system setting, per the
 *   brief. Phase 8 adds a system/light/dark preference that will drive this.
 * @param dynamicColor Material You. NOTE: applied to *system* surfaces only —
 *   content surfaces use ambient extraction (DESIGN.md §5) so the app is not
 *   at the mercy of the user's wallpaper. Dynamic is therefore only consulted
 *   in light mode; the dark scheme is the designed near-black one.
 * @param ambientColors overrides the ambient palette, normally supplied by the
 *   screen showing artwork.
 */
@Composable
fun SoraTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    ambientColors: AmbientColors = AmbientColors.Fallback,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else SoraLightColors
        }

        darkTheme -> SoraDarkColors
        else -> SoraLightColors
    }

    CompositionLocalProvider(
        LocalAmbientColors provides animatedAmbientColors(ambientColors),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoraTypography,
            shapes = SoraShapes,
            content = content,
        )
    }
}

/** Exposed so a future settings screen can offer "follow system". */
@Composable
fun systemPrefersDark(): Boolean = isSystemInDarkTheme()
