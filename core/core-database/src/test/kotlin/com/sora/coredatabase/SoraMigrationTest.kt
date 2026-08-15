package com.sora.coredatabase

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sora.coredatabase.migration.SoraMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Migration test required by the project brief.
 *
 * Runs under Robolectric so it executes as a fast JVM unit test in CI rather
 * than needing a connected device. MigrationTestHelper builds a v1 database
 * from the committed schema JSON, applies the migration, and validates the
 * result against the v2 schema generated at compile time - which is why
 * `core-database/schemas/` must stay in version control.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SoraMigrationTest {

    /**
     * Schema JSON is read from test assets. The build file maps
     * `schemas/debug` (the Room plugin's variant output folder) as an assets
     * source root, so the helper finds `<database-class>/<version>.json` at
     * the location it expects by default.
     */
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SoraDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsLastConfirmedChapter_andPreservesExistingRows() {
        // v1: insert a row using only columns that existed at version 1.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO library_entries
                    (id, anilistId, type, title, coverUrl, sourceType, rootPath, matchStatus)
                VALUES
                    ('entry-1', 12345, 'MANGA', 'Frieren', NULL, 'LOCAL', '/manga/frieren', 'CONFIRMED')
                """.trimIndent(),
            )
        }

        // Apply 1 -> 2 and validate the resulting schema against 2.json.
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            SoraMigrations.MIGRATION_1_2,
        )

        // The pre-existing row must survive with its data intact.
        db.query("SELECT id, title, anilistId, lastConfirmedChapter FROM library_entries")
            .use { cursor ->
                assertTrue("expected the v1 row to survive migration", cursor.moveToFirst())
                assertEquals("entry-1", cursor.getString(0))
                assertEquals("Frieren", cursor.getString(1))
                assertEquals(12345, cursor.getInt(2))
                // The new column must exist and default to NULL for old rows -
                // null means "user has not confirmed a chapter yet", which is
                // distinct from a real value of 0.
                assertTrue(
                    "lastConfirmedChapter should be NULL for pre-existing rows",
                    cursor.isNull(3),
                )
                assertEquals(1, cursor.count)
            }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
