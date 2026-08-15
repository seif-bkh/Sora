import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import com.sora.buildlogic.libs

/**
 * Hilt + KSP. Applied by any module that contributes to or consumes the DI
 * graph.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        // Matches the plugin id declared in libs.versions.toml. The root build
        // declares it with `apply false`, which puts it on the classpath so it
        // can be applied by id here.
        pluginManager.apply("com.google.dagger.hilt.android")

        dependencies {
            add("implementation", libs.findLibrary("hilt-android").get())
            add("ksp", libs.findLibrary("hilt-compiler").get())
        }
    }
}
