package com.sora.app.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sora.app.navigation.SoraNavHost
import com.sora.app.navigation.TopLevelDestination

/**
 * Root composable: adaptive navigation chrome wrapped around the NavHost.
 *
 * ADAPTIVE BEHAVIOUR (project brief: use WindowSizeClass from the start)
 *   Compact width  -> bottom navigation bar   (phones, portrait)
 *   Medium width   -> navigation rail         (small tablets, unfolded, landscape phones)
 *   Expanded width -> navigation rail         (large tablets)
 *
 * A permanent drawer on expanded widths is deliberately not used yet: with
 * only three top-level destinations it wastes horizontal space that the
 * library grid can use for more columns. Revisit in Phase 8.
 */
@Composable
fun SoraApp(
    windowSizeClass: WindowSizeClass,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = currentBackStackEntry?.destination

    // Immersive routes (player, reader, auth) hide the navigation chrome.
    val showNavigation = TopLevelDestination.entries.any { destination ->
        currentDestination.isRouteInHierarchy(destination.route)
    }

    val layoutType = when {
        !showNavigation -> NavigationSuiteType.None
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ->
            NavigationSuiteType.NavigationBar

        else -> NavigationSuiteType.NavigationRail
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isRouteInHierarchy(destination.route)
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        },
    ) {
        SoraNavHost(navController = navController)
    }
}

/**
 * Standard top-level navigation: single instance per destination, state saved
 * and restored, and the back stack popped to the graph's start so the back
 * button always exits from the start destination rather than unwinding every
 * tab visited.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.isRouteInHierarchy(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true
