package com.sora.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose setup shared by every UI-bearing module.
 *
 * Note: from Kotlin 2.0 the Compose compiler is applied as the separate
 * `org.jetbrains.kotlin.plugin.compose` plugin (done by the calling convention
 * plugin) rather than through a `composeOptions { kotlinCompilerExtension }`
 * block, which no longer exists.
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        // platform() comes from org.gradle.kotlin.dsl's DependencyHandler
        // extensions, in scope inside this dependencies { } block.
        val bom = platform(libs.findLibrary("androidx-compose-bom").get())
        add("implementation", bom)
        add("androidTestImplementation", bom)

        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("androidx-compose-material3").get())

        // Tooling is debug-only so previews/inspector never ship in release.
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())

        add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
    }
}
