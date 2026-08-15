import com.android.build.api.dsl.ApplicationExtension
import com.sora.buildlogic.configureAndroidCompose
import com.sora.buildlogic.configureKotlinAndroid
import com.sora.buildlogic.libs
import com.sora.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applied by `:app` only. Bundles Compose, since the single-Activity app
 * module is by definition a Compose host.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            configureAndroidCompose(this)
            defaultConfig.targetSdk = libs.version("targetSdk").toInt()
        }
    }
}
