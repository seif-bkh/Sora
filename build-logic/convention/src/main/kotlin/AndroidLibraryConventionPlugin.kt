import com.android.build.api.dsl.LibraryExtension
import com.sora.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Base for every Android library module (core-* and sources-*). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // Library modules do not declare targetSdk; it is an application
            // level concern and AGP 8+ warns when a library sets it.
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }
    }
}
