package com.sora.app.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sora.app.ui.PlaceholderScreen
import com.sora.app.ui.shell.SoraShell

/**
 * The app's single navigation graph.
 *
 * SHARED ELEMENTS
 *   The whole graph is wrapped in a [SharedTransitionLayout] so a cover can
 *   animate into the detail screen's key art (DESIGN.md §5). The API is
 *   experimental in the Compose 1.7 BOM we pin, so the opt-in is confined to
 *   this file rather than scattered across every screen that participates.
 *
 * TRANSITIONS
 *   Content cross-fades; chrome never slides (§5). Compose animation respects
 *   the system animator scale, so this satisfies reduced-motion for free.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SoraNavHost(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = SoraRoutes.SHELL,
            enterTransition = { fadeIn(tween(TRANSITION_MS)) },
            exitTransition = { fadeOut(tween(TRANSITION_MS)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
            popExitTransition = { fadeOut(tween(TRANSITION_MS)) },
        ) {
            // --- Shell: Home <-> Discover pager ---------------------------
            composable(SoraRoutes.SHELL) {
                SoraShell(
                    windowSizeClass = windowSizeClass,
                    onOpenSettings = { navController.navigate(SoraRoutes.SETTINGS) },
                    onOpenSearch = { navController.navigate(SoraRoutes.SEARCH) },
                    homeContent = {
                        // Phase 2+: the resume queue (DESIGN.md §4.1).
                        PlaceholderScreen(
                            title = "Continue",
                            description = "The resume queue lands here: hero card, " +
                                "continue-watching and continue-reading rails.",
                            microLabel = "HOME",
                            onPrimaryAction = {
                                navController.navigate(SoraRoutes.details("sample"))
                            },
                            primaryActionLabel = "Open a sample detail",
                        )
                    },
                    discoverContent = {
                        // Phase 2: AniList trending, Canvas layout (§4.3).
                        PlaceholderScreen(
                            title = "Discover",
                            description = "Full-bleed AniList posters: trending, " +
                                "seasonal and recommended.",
                            microLabel = "DISCOVER",
                            onPrimaryAction = { navController.navigate(SoraRoutes.AUTH) },
                            primaryActionLabel = "Sign in to AniList",
                        )
                    },
                )
            }

            // --- Pushed destinations --------------------------------------
            composable(SoraRoutes.AUTH) {
                PlaceholderScreen(
                    title = "Sign in",
                    description = "AniList OAuth2 via Custom Tabs. Phase 2.",
                    microLabel = "ACCOUNT",
                    onPrimaryAction = navController::popBackStack,
                    primaryActionLabel = "Back",
                )
            }

            composable(SoraRoutes.SETTINGS) {
                PlaceholderScreen(
                    title = "Settings",
                    description = "Server configuration, account and playback " +
                        "preferences. Phase 8.",
                    microLabel = "SETTINGS",
                    onPrimaryAction = navController::popBackStack,
                    primaryActionLabel = "Back",
                )
            }

            composable(SoraRoutes.SEARCH) {
                PlaceholderScreen(
                    title = "Search",
                    description = "Searches the local library and AniList. " +
                        "Reachable by pulling down on Home.",
                    microLabel = "SEARCH",
                    onPrimaryAction = navController::popBackStack,
                    primaryActionLabel = "Back",
                )
            }

            composable(SoraRoutes.DETAILS) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId").orEmpty()
                PlaceholderScreen(
                    title = "Details",
                    description = "Key art, metadata and the unit list for " +
                        "\"$entryId\". Phase 2 and Phase 4.",
                    microLabel = "SERIES",
                    onPrimaryAction = { navController.navigate(SoraRoutes.reader("sample")) },
                    primaryActionLabel = "Open reader",
                )
            }

            composable(SoraRoutes.PLAYER) { backStackEntry ->
                val unitId = backStackEntry.arguments?.getString("unitId").orEmpty()
                PlaceholderScreen(
                    title = "Player",
                    description = "Media3 playback for \"$unitId\". Phase 5.",
                    microLabel = "PLAYING",
                    onPrimaryAction = navController::popBackStack,
                    primaryActionLabel = "Back",
                )
            }

            composable(SoraRoutes.READER) { backStackEntry ->
                val unitId = backStackEntry.arguments?.getString("unitId").orEmpty()
                PlaceholderScreen(
                    title = "Reader",
                    description = "Paged and webtoon reader for \"$unitId\". Phase 6.",
                    microLabel = "READING",
                    onPrimaryAction = navController::popBackStack,
                    primaryActionLabel = "Back",
                )
            }
        }
    }
}

private const val TRANSITION_MS = 300
