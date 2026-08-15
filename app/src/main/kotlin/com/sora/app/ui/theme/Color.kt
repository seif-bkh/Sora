package com.sora.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Static fallback palette, used on devices without Material You dynamic colour
 * (below Android 12) or when the user disables it.
 *
 * Derived from the Sora brand seed #4A90E2 - the mid-stop of the sky gradient
 * in the launcher icon. Tonal steps follow the Material 3 tonal-palette
 * convention (light scheme uses tone 40 for primary, dark uses tone 80).
 */

// --- Brand seed ------------------------------------------------------------
val SoraSkyTop = Color(0xFF5BA3D0)
val SoraSkyMid = Color(0xFF4A90E2) // seed
val SoraSkyDeep = Color(0xFF2C5AA0)

// --- Light scheme ----------------------------------------------------------
val LightPrimary = Color(0xFF2C5AA0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD6E3FF)
val LightOnPrimaryContainer = Color(0xFF001B3E)

val LightSecondary = Color(0xFF565E71)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDAE2F9)
val LightOnSecondaryContainer = Color(0xFF131C2C)

val LightTertiary = Color(0xFF6F5575)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF9D8FE)
val LightOnTertiaryContainer = Color(0xFF28132F)

val LightBackground = Color(0xFFFDFBFF)
val LightOnBackground = Color(0xFF1A1B1F)
val LightSurface = Color(0xFFFDFBFF)
val LightOnSurface = Color(0xFF1A1B1F)
val LightSurfaceVariant = Color(0xFFE0E2EC)
val LightOnSurfaceVariant = Color(0xFF44464F)
val LightOutline = Color(0xFF74777F)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

// --- Dark scheme (app default - see Theme.kt) ------------------------------
val DarkPrimary = Color(0xFFA9C7FF)
val DarkOnPrimary = Color(0xFF002F65)
val DarkPrimaryContainer = Color(0xFF00458E)
val DarkOnPrimaryContainer = Color(0xFFD6E3FF)

val DarkSecondary = Color(0xFFBEC6DC)
val DarkOnSecondary = Color(0xFF283041)
val DarkSecondaryContainer = Color(0xFF3E4759)
val DarkOnSecondaryContainer = Color(0xFFDAE2F9)

val DarkTertiary = Color(0xFFDCBCE1)
val DarkOnTertiary = Color(0xFF3F2845)
val DarkTertiaryContainer = Color(0xFF573E5C)
val DarkOnTertiaryContainer = Color(0xFFF9D8FE)

val DarkBackground = Color(0xFF111318)
val DarkOnBackground = Color(0xFFE3E2E6)
val DarkSurface = Color(0xFF111318)
val DarkOnSurface = Color(0xFFE3E2E6)
val DarkSurfaceVariant = Color(0xFF44464F)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
val DarkOutline = Color(0xFF8E9099)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
