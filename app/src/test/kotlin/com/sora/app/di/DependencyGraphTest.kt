package com.sora.app.di

import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sora.coredatabase.di.DatabaseModule
import com.sora.coredatastore.UserSettings
import com.sora.coredatastore.UserSettingsSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies the Hilt @Provides functions actually construct working objects.
 *
 * Deliberately calls the module functions directly rather than spinning up a
 * full Hilt test component: a full component needs a custom test runner and
 * an instrumented-style setup, which is heavier than the value it adds here.
 * Missing bindings are already a *compile-time* error in Hilt; what this test
 * catches is the runtime half - a provider that throws, a Room database that
 * will not open, or a DataStore whose serializer defaults are wrong.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DependencyGraphTest {

    @Test
    fun databaseModule_providesOpenableDatabaseAndDaos() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val database = DatabaseModule.providesSoraDatabase(context)
        try {
            // Providers returning non-null is not enough: opening the database
            // is what actually executes Room's generated schema creation.
            assertNotNull(database.openHelper.writableDatabase)

            assertNotNull(DatabaseModule.providesLibraryEntryDao(database))
            assertNotNull(DatabaseModule.providesMediaUnitDao(database))
            assertNotNull(DatabaseModule.providesAniListCacheDao(database))
            assertNotNull(DatabaseModule.providesMatchCandidateDao(database))
        } finally {
            database.close()
            context.deleteDatabase(com.sora.coredatabase.SoraDatabase.NAME)
        }
    }

    @Test
    fun userSettingsDataStore_appliesBriefDefaultsOnFirstRun() = runTest {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "sora-ds-${System.nanoTime()}")
        tempDir.mkdirs()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        val dataStore = DataStoreFactory.create(
            serializer = UserSettingsSerializer(),
            scope = scope,
            produceFile = { File(tempDir, "settings.pb") },
        )

        val settings: UserSettings = dataStore.data.first()

        // Dark by default and dynamic colour on, per the project brief.
        assertEquals(com.sora.coredatastore.ThemeMode.THEME_MODE_DARK, settings.themeMode)
        assertTrue(settings.useDynamicColor)
        assertEquals(
            com.sora.coredatastore.ReadingMode.READING_MODE_PAGED,
            settings.defaultReadingMode,
        )
        // Brief suggests syncing once ~90% of a video has been watched.
        assertEquals(0.9f, settings.playbackSyncThreshold, 0.0001f)
        assertTrue(settings.syncProgressToAnilist)

        tempDir.deleteRecursively()
    }
}
