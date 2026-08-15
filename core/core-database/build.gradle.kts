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
     * MigrationTestHelper reads schema JSON from test *assets*, not from the
     * source tree, and it needs BOTH versions: the committed v1 (to build the
     * old database) and the v2 that Room generates at compile time (to
     * validate the migration result).
     *
     * The Room Gradle plugin writes to `schemas/<variant>/<db>/<n>.json`, so
     * the variant folder is mapped as the assets root and the helper finds
     * `<db>/<n>.json` at the path it expects.
     */
    sourceSets {
        getByName("test") {
            assets.srcDir(layout.projectDirectory.dir("schemas/debug"))
        }
        getByName("androidTest") {
            assets.srcDir(layout.projectDirectory.dir("schemas/debug"))
        }
    }

    testOptions {
        unitTests {
            // Robolectric needs merged Android resources and assets.
            isIncludeAndroidResources = true
        }
    }
}

/**
 * The unit-test assets must be packaged only after Room has exported the
 * current schema, otherwise the generated v2 JSON is missing from assets and
 * `runMigrationsAndValidate` fails with FileNotFoundException.
 */
tasks.matching { it.name == "generateDebugUnitTestAssets" || it.name == "mergeDebugUnitTestAssets" }
    .configureEach {
        dependsOn(tasks.matching { it.name == "copyRoomSchemas" })
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
