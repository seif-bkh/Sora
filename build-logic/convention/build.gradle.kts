plugins {
    `kotlin-dsl`
}

group = "com.sora.buildlogic"

// Convention plugins must target the same JVM as the main build (JDK 17,
// AGP 8.13's requirement).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    // Needed so AndroidRoomConventionPlugin can reference RoomExtension.
    compileOnly(libs.room.gradlePlugin)
}

// Register each convention plugin under the id used in libs.versions.toml.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "sora.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "sora.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "sora.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "sora.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "sora.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "sora.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "sora.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
