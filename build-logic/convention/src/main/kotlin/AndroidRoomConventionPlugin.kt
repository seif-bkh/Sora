import androidx.room.gradle.RoomExtension
import com.sora.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Room + KSP configuration.
 *
 * Uses the `androidx.room` Gradle plugin (Room 2.6+) rather than passing
 * `room.schemaLocation` as a raw KSP argument. The plugin declares the schema
 * directory as a proper task input/output, which keeps schema generation
 * reproducible and cacheable - a raw KSP arg is an opaque string that breaks
 * up-to-date checks and the Gradle build cache.
 *
 * Exported schema JSON is committed to the repository: `MigrationTestHelper`
 * reads it to build an old-version database, so migration tests cannot work
 * without it.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("androidx.room")
        pluginManager.apply("com.google.devtools.ksp")

        // Extension objects are fetched and configured directly rather than
        // via configuration lambdas; see KotlinAndroid.kt for the rationale.
        val room = extensions.getByType(RoomExtension::class.java)
        room.schemaDirectory("$projectDir/schemas")

        dependencies {
            add("implementation", libs.findLibrary("room-runtime").get())
            add("implementation", libs.findLibrary("room-ktx").get())
            add("ksp", libs.findLibrary("room-compiler").get())
            add("testImplementation", libs.findLibrary("room-testing").get())
        }
    }
}
