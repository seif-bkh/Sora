import com.android.build.api.dsl.LibraryExtension
import com.sora.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Adds Compose on top of `sora.android.library`, for library modules that
 * expose UI but are not full features (currently unused; feature modules use
 * `sora.android.feature`, which includes Compose).
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("sora.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            configureAndroidCompose(this)
        }
    }
}
