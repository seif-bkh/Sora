package com.sora.coredatabase.di

import android.content.Context
import androidx.room.Room
import com.sora.coredatabase.SoraDatabase
import com.sora.coredatabase.dao.AniListCacheDao
import com.sora.coredatabase.dao.LibraryEntryDao
import com.sora.coredatabase.dao.MatchCandidateDao
import com.sora.coredatabase.dao.MediaUnitDao
import com.sora.coredatabase.migration.SoraMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesSoraDatabase(
        @ApplicationContext context: Context,
    ): SoraDatabase = Room.databaseBuilder(
        context,
        SoraDatabase::class.java,
        SoraDatabase.NAME,
    )
        .addMigrations(*SoraMigrations.ALL)
        // No fallbackToDestructiveMigration: the library, confirmed matches
        // and read positions are user-generated state that must never be
        // silently discarded. A missing migration should fail loudly.
        .build()

    @Provides
    fun providesLibraryEntryDao(database: SoraDatabase): LibraryEntryDao =
        database.libraryEntryDao()

    @Provides
    fun providesMediaUnitDao(database: SoraDatabase): MediaUnitDao =
        database.mediaUnitDao()

    @Provides
    fun providesAniListCacheDao(database: SoraDatabase): AniListCacheDao =
        database.aniListCacheDao()

    @Provides
    fun providesMatchCandidateDao(database: SoraDatabase): MatchCandidateDao =
        database.matchCandidateDao()
}
