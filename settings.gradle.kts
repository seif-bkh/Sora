pluginManagement {
    // build-logic is an included build supplying the `sora.*` convention
    // plugins used by every module.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sora"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// --- app -----------------------------------------------------------------
include(":app")

// --- core ----------------------------------------------------------------
include(":core:core-common")
include(":core:core-model")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-network")

// --- features ------------------------------------------------------------
include(":feature:feature-auth")
include(":feature:feature-library")
include(":feature:feature-discovery")
include(":feature:feature-details")
include(":feature:feature-player")
include(":feature:feature-reader")
include(":feature:feature-settings")

// --- sources -------------------------------------------------------------
include(":sources:source-local")
include(":sources:source-server")

// NOTE: `:benchmark` (Macrobenchmark + Baseline Profiles) is introduced in
// Phase 7 per the project brief, not stubbed here.
