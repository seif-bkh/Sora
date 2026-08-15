plugins {
    alias(libs.plugins.sora.android.library)
    alias(libs.plugins.sora.android.hilt)
}

android {
    namespace = "com.sora.corecommon"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
