package com.sora.app.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.sora.app.navigation.SoraNavHost

/**
 * Root composable.
 *
 * Previously this held a [androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold]
 * with a bottom bar / rail. DESIGN.md §3 removes that: the bottom bar was the
 * clearest tell of the Tachiyomi lineage, and with only two primary surfaces a
 * whole navigation component to switch between them is a poor trade for the
 * vertical space it eats.
 *
 * Navigation chrome now lives inside the shell
 * ([com.sora.app.ui.shell.SoraShell]), which is itself just one destination in
 * the graph, so immersive routes (player, reader) get a clean screen without
 * any chrome-hiding special case.
 */
@Composable
fun SoraApp(
    windowSizeClass: WindowSizeClass,
    navController: NavHostController = rememberNavController(),
) {
    SoraNavHost(
        navController = navController,
        windowSizeClass = windowSizeClass,
    )
}
