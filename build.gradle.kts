// Root build file. Plugins are declared with `apply false` so that subprojects
// (via the sora.* convention plugins) can apply them without re-resolving
// versions.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Declared here so the convention plugins can apply them by id; without
    // this the plugin is absent from the build classpath and applying it
    // fails with "Plugin with id '...' not found".
    alias(libs.plugins.room) apply false
    alias(libs.plugins.protobuf) apply false
}
