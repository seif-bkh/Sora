package com.sora.coredatabase.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.sora.coremodel.MatchStatus
import com.sora.coremodel.MediaType
import com.sora.coredatabase.entity.LibraryEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Queries the library grid observes are Flow-returning, per the brief, so the
 * UI updates automatically as a background scan inserts rows.
 */
@Dao
interface LibraryEntryDao {

    @Query("SELECT * FROM library_entries ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<LibraryEntryEntity>>

    @Query(
        "SELECT * FROM library_entries WHERE type = :type " +
            "ORDER BY title COLLATE NOCASE ASC",
    )
    fun observeByType(type: MediaType): Flow<List<LibraryEntryEntity>>

    /** Backs the "needs review" section of the library screen. */
    @Query(
        "SELECT * FROM library_entries WHERE matchStatus != :confirmed " +
            "ORDER BY title COLLATE NOCASE ASC",
    )
    fun observeNeedingReview(
        confirmed: MatchStatus = MatchStatus.CONFIRMED,
    ): Flow<List<LibraryEntryEntity>>

    @Query("SELECT * FROM library_entries WHERE id = :id")
    fun observeById(id: String): Flow<LibraryEntryEntity?>

    @Query("SELECT * FROM library_entries WHERE id = :id")
    suspend fun getById(id: String): LibraryEntryEntity?

    /** Used by incremental re-scans to skip folders already in the library. */
    @Query("SELECT * FROM library_entries WHERE rootPath = :rootPath")
    suspend fun getByRootPath(rootPath: String): LibraryEntryEntity?

    @Query("SELECT rootPath FROM library_entries")
    suspend fun getAllRootPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: LibraryEntryEntity): Long

    @Upsert
    suspend fun upsertAll(entries: List<LibraryEntryEntity>)

    /**
     * Applied when the user confirms a match. Sets CONFIRMED so later re-scans
     * leave the entry alone (brief: "re-scans must skip already-confirmed
     * entries").
     */
    @Query(
        "UPDATE library_entries SET anilistId = :anilistId, coverUrl = :coverUrl, " +
            "matchStatus = :status WHERE id = :id",
    )
    suspend fun applyMatch(
        id: String,
        anilistId: Int,
        coverUrl: String?,
        status: MatchStatus = MatchStatus.CONFIRMED,
    )

    /** Reference point for estimating the next volume's end chapter. */
    @Query("UPDATE library_entries SET lastConfirmedChapter = :chapter WHERE id = :id")
    suspend fun updateLastConfirmedChapter(id: String, chapter: Float)

    @Query("DELETE FROM library_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
