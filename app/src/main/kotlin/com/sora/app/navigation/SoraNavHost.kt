package com.sora.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sora.app.ui.PlaceholderScreen

/**
 * The app's single navigation graph.
 *
 * Phase 1a wires placeholder screens so the whole graph is navigable end to
 * end. Each `composable` block is replaced by the real feature entry point in
 * its phase; the routes themselves do not change.
 */
@Composable
fun SoraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SoraRoutes.LIBRARY,
        modifier = modifier,
    ) {
        // --- Top-level destinations ---------------------------------------
        composable(SoraRoutes.LIBRARY) {
            // Phase 3: feature-library scan + grid.
            PlaceholderScreen(
                title = "Library",
                description = "Local and server content will appear here once " +
                    "a folder is added. Implemented in Phase 3.",
                onPrimaryAction = { navController.navigate(SoraRoutes.details("sample")) },
                primaryActionLabel = "Open sample detail",
            )
        }

        composable(SoraRoutes.DISCOVERY) {
            // Phase 2: AniList trending / seasonal.
            PlaceholderScreen(
                title = "Discover",
                description = "Trending and seasonal titles from AniList. " +
                    "Implemented in Phase 2.",
                onPrimaryAction = { navController.navigate(SoraRoutes.AUTH) },
                primaryActionLabel = "Sign in to AniList",
            )
        }

        composable(SoraRoutes.SETTINGS) {
            // Phase 8: settings, server config, account management.
            PlaceholderScreen(
                title = "Settings",
                description = "Server configuration, account and playback " +
                    "preferences. Implemented in Phase 8.",
            )
        }

        // --- Pushed destinations (no nav chrome) --------------------------
        composable(SoraRoutes.AUTH) {
            // Phase 2: AniList OAuth2 via Custom Tabs.
            PlaceholderScreen(
                title = "Sign in",
                description = "AniList OAuth2 login. Implemented in Phase 2.",
                onPrimaryAction = navController::popBackStack,
                primaryActionLabel = "Back",
            )
        }

        composable(SoraRoutes.DETAILS) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId").orEmpty()
            // Phase 2/4: AniList metadata + unit list.
            PlaceholderScreen(
                title = "Details",
                description = "Metadata and episode/chapter list for \"$entryId\". " +
                    "Implemented in Phase 2 and Phase 4.",
                onPrimaryAction = { navController.navigate(SoraRoutes.reader("sample")) },
                primaryActionLabel = "Open reader",
            )
        }

        composable(SoraRoutes.PLAYER) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getString("unitId").orEmpty()
            // Phase 5: Media3/ExoPlayer with custom Compose controls.
            PlaceholderScreen(
                title = "Player",
                description = "Video playback for unit \"$unitId\". " +
                    "Implemented in Phase 5.",
                onPrimaryAction = navController::popBackStack,
                primaryActionLabel = "Back",
            )
        }

        composable(SoraRoutes.READER) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getString("unitId").orEmpty()
            // Phase 6: paged + webtoon reader with subsampled rendering.
            PlaceholderScreen(
                title = "Reader",
                description = "Manga reader for unit \"$unitId\". " +
                    "Implemented in Phase 6.",
                onPrimaryAction = navController::popBackStack,
                primaryActionLabel = "Back",
            )
        }
    }
}
