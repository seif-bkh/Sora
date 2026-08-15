// build-logic is an *included build*: it compiles the convention plugins that
// the main build then applies. It needs its own settings file and its own
// view of the version catalog.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
