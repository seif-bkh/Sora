plugins {
    alias(libs.plugins.sora.android.library)
    alias(libs.plugins.sora.android.hilt)
    alias(libs.plugins.sora.android.room)
}

android {
    namespace = "com.sora.coredatabase"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Room migration tests run on the JVM under Robolectric rather than on a
    // device, so CI can execute them in the standard unit-test job.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Entities reference the shared enums (MediaType, UnitType, ...).
    api(projects.core.coreModel)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
