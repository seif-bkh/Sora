package com.sora.coredatabase.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sora.coredatabase.entity.AniListCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AniListCacheDao {

    @Query("SELECT * FROM anilist_cache WHERE anilistId = :anilistId")
    suspend fun get(anilistId: Int): AniListCacheEntity?

    @Query("SELECT * FROM anilist_cache WHERE anilistId = :anilistId")
    fun observe(anilistId: Int): Flow<AniListCacheEntity?>

    /**
     * Fresh-entry lookup for TTL enforcement: returns the row only if it was
     * fetched at or after [minEpochMs]. Callers pass `now - ttl`, so a stale
     * row reads as a miss and triggers a refetch.
     */
    @Query(
        "SELECT * FROM anilist_cache WHERE anilistId = :anilistId " +
            "AND lastFetchedEpochMs >= :minEpochMs",
    )
    suspend fun getIfFresh(anilistId: Int, minEpochMs: Long): AniListCacheEntity?

    @Upsert
    suspend fun upsert(entry: AniListCacheEntity)

    @Upsert
    suspend fun upsertAll(entries: List<AniListCacheEntity>)

    /** Housekeeping for the periodic WorkManager cache-refresh job. */
    @Query("DELETE FROM anilist_cache WHERE lastFetchedEpochMs < :beforeEpochMs")
    suspend fun deleteStale(beforeEpochMs: Long)
}
