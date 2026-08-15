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

    /**
     * MigrationTestHelper loads schema JSON from the test *assets*, not from
     * the source tree, so the exported schema directory has to be registered
     * as an asset source. Without this the test fails with
     * FileNotFoundException even though the JSON is committed.
     *
     * The Room Gradle plugin writes into variant subfolders
     * (schemas/<variant>/<database>/<version>.json). Mapping the *variant*
     * folder as the assets root means the helper finds
     * `<database>/<version>.json` where it looks by default.
     */
    sourceSets {
        getByName("test") {
            assets.srcDirs(files("$projectDir/schemas/debug"))
        }
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas/debug"))
        }
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
