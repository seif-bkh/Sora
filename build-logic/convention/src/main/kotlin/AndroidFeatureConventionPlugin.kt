import com.android.build.api.dsl.LibraryExtension
import com.sora.buildlogic.configureAndroidCompose
import com.sora.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * The standard shape of a `feature-*` module: Android library + Compose +
 * Hilt + navigation, plus the core modules every feature legitimately needs.
 *
 * MODULE BOUNDARY RULE (project brief): feature modules must not depend on
 * each other, and must never see source-local / source-server concretes. They
 * get `core-model` (which owns the MediaSource interface) and `core-common`;
 * concrete sources are bound into the graph by `:app` via Hilt. That rule is
 * enforced here by giving features a fixed dependency set rather than letting
 * each module's build file add whatever it likes.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("sora.android.library")
        pluginManager.apply("sora.android.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            configureAndroidCompose(this)
        }

        dependencies {
            add("implementation", project(":core:core-model"))
            add("implementation", project(":core:core-common"))

            add("implementation", libs.findLibrary("androidx-core-ktx").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())

            add("testImplementation", libs.findLibrary("junit4").get())
        }
    }
}
