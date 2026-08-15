package com.sora.coredatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sora.coredatabase.converter.SoraTypeConverters
import com.sora.coredatabase.dao.AniListCacheDao
import com.sora.coredatabase.dao.LibraryEntryDao
import com.sora.coredatabase.dao.MatchCandidateDao
import com.sora.coredatabase.dao.MediaUnitDao
import com.sora.coredatabase.entity.AniListCacheEntity
import com.sora.coredatabase.entity.LibraryEntryEntity
import com.sora.coredatabase.entity.MatchCandidateEntity
import com.sora.coredatabase.entity.MediaUnitEntity

/**
 * The app's Room database.
 *
 * `exportSchema = true` (the default) writes versioned JSON into
 * `core-database/schemas/`, which is committed. MigrationTestHelper builds an
 * old-version database from those files, so migration tests are impossible
 * without them.
 */
@Database(
    entities = [
        LibraryEntryEntity::class,
        MediaUnitEntity::class,
        AniListCacheEntity::class,
        MatchCandidateEntity::class,
    ],
    version = SoraDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(SoraTypeConverters::class)
abstract class SoraDatabase : RoomDatabase() {

    abstract fun libraryEntryDao(): LibraryEntryDao
    abstract fun mediaUnitDao(): MediaUnitDao
    abstract fun aniListCacheDao(): AniListCacheDao
    abstract fun matchCandidateDao(): MatchCandidateDao

    companion object {
        /**
         * Bump on every schema change and add a Migration in
         * [com.sora.coredatabase.migration.SoraMigrations].
         */
        const val VERSION = 2

        const val NAME = "sora.db"
    }
}
