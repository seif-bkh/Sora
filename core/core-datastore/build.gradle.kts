import com.google.protobuf.gradle.ProtobufExtension

plugins {
    alias(libs.plugins.sora.android.library)
    alias(libs.plugins.sora.android.hilt)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.sora.coredatastore"
}

/**
 * Proto DataStore codegen.
 *
 * `javalite` is the Android-appropriate runtime: the full protobuf runtime
 * carries reflection and descriptor machinery that is dead weight in an APK
 * and hostile to R8. Lite drops it and generates smaller classes.
 */
extensions.configure<ProtobufExtension> {
    protoc {
        // Must be a "group:name:version" coordinate string. `.get().toString()`
        // on the Provider yields exactly that for a version-catalog entry.
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
                register("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(projects.core.coreCommon)

    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.tink.android)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
