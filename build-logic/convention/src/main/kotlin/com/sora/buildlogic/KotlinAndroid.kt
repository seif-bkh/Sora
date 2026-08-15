package com.sora.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Shorthand for reading the shared version catalog from a convention plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

/**
 * Android + Kotlin configuration shared by every module in the project.
 *
 * Centralised here so SDK levels, Java version and compiler flags cannot drift
 * between the 13 modules.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = libs.version("compileSdk").toInt()

        defaultConfig {
            minSdk = libs.version("minSdk").toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            // Core library desugaring is off: nothing currently needs the
            // java.time backport on API 26. Enable (plus the
            // coreLibraryDesugaring dependency) if that changes.
            isCoreLibraryDesugaringEnabled = false
        }
    }

    extensions.configure(KotlinAndroidProjectExtension::class.java) { kotlin ->
        kotlin.compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                // Coroutines/Flow APIs used across the codebase are opt-in.
                "-opt-in=kotlin.RequiresOptIn",
            )
        }
    }
}
