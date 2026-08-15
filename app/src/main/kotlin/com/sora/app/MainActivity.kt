package com.sora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sora.app.ui.SoraApp
import com.sora.app.ui.theme.SoraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's single Activity (project brief: single-Activity architecture).
 * Everything else is a Compose destination inside [SoraApp]'s NavHost.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() and setContent().
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Draw behind the system bars; Compose handles the insets.
        enableEdgeToEdge()

        setContent {
            SoraTheme {
                // Recomputed on configuration change (rotation, fold, resize),
                // so the nav chrome adapts live rather than at launch only.
                SoraApp(windowSizeClass = calculateWindowSizeClass(this))
            }
        }
    }
}
