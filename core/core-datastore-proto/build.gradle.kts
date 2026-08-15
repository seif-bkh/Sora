plugins {
    alias(libs.plugins.sora.jvm.library)
    alias(libs.plugins.protobuf)
}

/**
 * Protobuf schemas and their generated classes, isolated in their own module.
 *
 * WHY A SEPARATE MODULE (this is not gratuitous)
 *   When the protobuf plugin and KSP (Hilt/Room) run in the same module, KSP
 *   can execute before protoc has produced the generated classes. Hilt then
 *   sees `error.NonExistentClass` where `UserSettings` should be and fails:
 *
 *     [ksp] InjectProcessingStep was unable to process
 *     'SettingsRepository(DataStore<error.NonExistentClass>)'
 *
 *   Splitting codegen into a KSP-free module removes the ordering conflict:
 *   this module is a normal compiled dependency by the time `core-datastore`
 *   is processed. Same structure Now in Android uses.
 *
 * Pure JVM: generated protobuf-lite classes have no Android dependency.
 */
protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                // "java" is registered by default; reconfigure it for lite.
                named("java") {
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
    // api, not implementation: consumers reference the generated types
    // directly (UserSettings, AuthTokens), so the runtime must leak through.
    api(libs.protobuf.kotlin.lite)
}
