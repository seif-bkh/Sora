// build-logic is an *included build*: it compiles the convention plugins that
// the main build then applies. It needs its own settings file and its own
// view of the version catalog.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Required: the KSP dependency in convention/build.gradle.kts is a
        // plugin *marker* artifact, which is published to the Gradle Plugin
        // Portal rather than to Maven Central.
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
