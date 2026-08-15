package com.sora.coredatabase.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sora.coredatabase.entity.MediaUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaUnitDao {

    /** Episode/chapter list on the detail screen. */
    @Query(
        "SELECT * FROM media_units WHERE libraryEntryId = :libraryEntryId " +
            "ORDER BY number ASC",
    )
    fun observeForEntry(libraryEntryId: String): Flow<List<MediaUnitEntity>>

    @Query("SELECT * FROM media_units WHERE id = :id")
    fun observeById(id: String): Flow<MediaUnitEntity?>

    @Query("SELECT * FROM media_units WHERE id = :id")
    suspend fun getById(id: String): MediaUnitEntity?

    /**
     * Next unit in reading/watching order, for the player's "next episode"
     * control and the reader's chapter advance.
     */
    @Query(
        "SELECT * FROM media_units WHERE libraryEntryId = :libraryEntryId " +
            "AND number > :afterNumber ORDER BY number ASC LIMIT 1",
    )
    suspend fun getNextUnit(libraryEntryId: String, afterNumber: Float): MediaUnitEntity?

    @Query(
        "SELECT * FROM media_units WHERE libraryEntryId = :libraryEntryId " +
            "AND number < :beforeNumber ORDER BY number DESC LIMIT 1",
    )
    suspend fun getPreviousUnit(libraryEntryId: String, beforeNumber: Float): MediaUnitEntity?

    /** Incremental re-scan: which files are already known. */
    @Query("SELECT path FROM media_units WHERE libraryEntryId = :libraryEntryId")
    suspend fun getKnownPaths(libraryEntryId: String): List<String>

    @Upsert
    suspend fun upsertAll(units: List<MediaUnitEntity>)

    /** Video progress: called periodically during playback. */
    @Query(
        "UPDATE media_units SET lastPositionMs = :positionMs, " +
            "progressPercent = :progressPercent, isWatchedOrRead = :watched WHERE id = :id",
    )
    suspend fun updatePlaybackProgress(
        id: String,
        positionMs: Long,
        progressPercent: Float,
        watched: Boolean,
    )

    /**
     * Page progress: critical for VOLUME units, which span multiple sessions
     * and must resume exactly where the reader left off.
     */
    @Query(
        "UPDATE media_units SET currentPage = :currentPage, totalPages = :totalPages, " +
            "progressPercent = :progressPercent, isWatchedOrRead = :read WHERE id = :id",
    )
    suspend fun updateReadProgress(
        id: String,
        currentPage: Int,
        totalPages: Int,
        progressPercent: Float,
        read: Boolean,
    )

    /** Persists a user-confirmed chapter range after a volume completion. */
    @Query(
        "UPDATE media_units SET chapterRangeStart = :start, chapterRangeEnd = :end " +
            "WHERE id = :id",
    )
    suspend fun updateChapterRange(id: String, start: Float?, end: Float?)

    @Query("DELETE FROM media_units WHERE libraryEntryId = :libraryEntryId")
    suspend fun deleteForEntry(libraryEntryId: String)
}
