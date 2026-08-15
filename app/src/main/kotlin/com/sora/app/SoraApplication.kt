package com.sora.app

import android.app.Application
import android.os.Build
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point and Hilt graph root.
 */
@HiltAndroidApp
class SoraApplication : Application() {

    override fun onCreate() {
        // StrictMode must be installed before anything else touches disk or
        // network, otherwise early violations are missed.
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        super.onCreate()
    }

    /**
     * Enforces the project brief's hard rule that no network or disk I/O ever
     * happens on the main thread. Debug builds only - penalties are noisy by
     * design so violations surface during development rather than as jank in
     * the field.
     *
     * Disk violations are logged, not fatal: several framework and AndroidX
     * initialisers do benign main-thread reads we do not control (resource
     * loading, WebView init), and killing the process on those would make the
     * app undebuggable. They still appear in logcat, so our own violations are
     * visible. Main-thread *network* access is never acceptable and is fatal
     * via penaltyDeathOnNetwork().
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyDeathOnNetwork()
                .build(),
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        detectContentUriWithoutPermission()
                    }
                }
                .penaltyLog()
                .build(),
        )
    }
}
