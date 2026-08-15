package com.sora.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations, i.e. the ones reachable from the bottom bar / nav
 * rail. Detail, player and reader screens are NOT here: they are pushed onto
 * the back stack and hide the navigation chrome.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LIBRARY(
        route = SoraRoutes.LIBRARY,
        label = "Library",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
    ),
    DISCOVERY(
        route = SoraRoutes.DISCOVERY,
        label = "Discover",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
    ),
    SETTINGS(
        route = SoraRoutes.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
}

/**
 * Route constants.
 *
 * Kept as plain strings for Phase 1a. Type-safe navigation (Kotlin
 * serialization routes) is a natural upgrade once real arguments exist -
 * noted in DECISIONS.md.
 */
object SoraRoutes {
    const val LIBRARY = "library"
    const val DISCOVERY = "discovery"
    const val SETTINGS = "settings"
    const val AUTH = "auth"

    // Parameterised destinations, wired up in later phases.
    const val DETAILS = "details/{entryId}"
    const val PLAYER = "player/{unitId}"
    const val READER = "reader/{unitId}"

    fun details(entryId: String) = "details/$entryId"
    fun player(unitId: String) = "player/$unitId"
    fun reader(unitId: String) = "reader/$unitId"
}
