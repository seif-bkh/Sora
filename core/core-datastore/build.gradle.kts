plugins {
    alias(libs.plugins.sora.android.library)
    alias(libs.plugins.sora.android.hilt)
}

android {
    namespace = "com.sora.coredatastore"
}

dependencies {
    // Generated protobuf types live in their own KSP-free module; see that
    // module's build file for why.
    api(projects.core.coreDatastoreProto)
    api(libs.androidx.datastore)

    implementation(projects.core.coreCommon)
    implementation(libs.tink.android)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
