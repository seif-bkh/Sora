import java.util.Properties

plugins {
    // Compose is applied by the application convention plugin.
    alias(libs.plugins.sora.android.application)
    alias(libs.plugins.sora.android.hilt)
}

/**
 * AniList OAuth client id.
 *
 * NEVER hardcode this (project brief, Non-Functional Requirements). It is read
 * from `local.properties`, which is git-ignored, and falls back to "0" so the
 * project still builds for a developer who has not registered a client yet.
 * See README.md for how to supply your own.
 */
val anilistClientId: String = run {
    val properties = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(properties::load)
    }
    properties.getProperty("anilist.clientId")
        ?: System.getenv("ANILIST_CLIENT_ID")
        ?: "0"
}

android {
    namespace = "com.sora.app"

    defaultConfig {
        applicationId = "com.sora.app"
        versionCode = 1
        versionName = "0.1.0-phase1a"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ANILIST_CLIENT_ID", "\"$anilistClientId\"")
        // The OAuth redirect that Custom Tabs sends the token back to
        // (Phase 2). Declared here so the manifest placeholder and the code
        // that parses the redirect cannot drift apart.
        buildConfigField("String", "ANILIST_REDIRECT_URI", "\"sora://auth\"")
        manifestPlaceholders["anilistRedirectScheme"] = "sora"
    }

    buildFeatures {
        buildConfig = true
    }

    // Robolectric needs merged Android resources for the Hilt graph test.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // R8 full mode and the real shrinker config land in Phase 7.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // App is the composition root: it may see every module, and is the only
    // place allowed to bind source-* concretes into the Hilt graph.
    implementation(projects.core.coreModel)
    implementation(projects.core.coreCommon)
    implementation(projects.core.coreDatabase)
    implementation(projects.core.coreDatastore)
    implementation(projects.core.coreNetwork)

    implementation(projects.feature.featureAuth)
    implementation(projects.feature.featureLibrary)
    implementation(projects.feature.featureDiscovery)
    implementation(projects.feature.featureDetails)
    implementation(projects.feature.featurePlayer)
    implementation(projects.feature.featureReader)
    implementation(projects.feature.featureSettings)

    implementation(projects.sources.sourceLocal)
    implementation(projects.sources.sourceServer)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    // Adaptive layout: WindowSizeClass drives the compact pager vs the
    // medium/expanded two-pane + icon rail (DESIGN.md §3).
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.window)

    // HorizontalPager (foundation) is the shell's primary navigation, and
    // SharedTransitionLayout (animation) carries covers into the detail
    // screen. Both come via the BOM but are declared explicitly because the
    // code calls them directly.
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)

    // Ambient colour extraction from cover art.
    implementation(libs.androidx.palette)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    // Compose UI tests run under Robolectric so the shell's pager is covered
    // by `./gradlew test` in CI, where no emulator is available.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
