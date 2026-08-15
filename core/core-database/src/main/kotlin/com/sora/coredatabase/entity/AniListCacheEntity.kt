package com.sora.coredatabase.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached AniList metadata.
 *
 * Stores the raw JSON payload rather than a normalised set of columns: the
 * app renders whatever AniList returns, and AniList's schema evolves. Keeping
 * it opaque means a new field appears without a Room migration. It is also
 * what makes the brief's offline requirement work - a cached entry renders
 * fully with no network.
 *
 * [lastFetchedEpochMs] backs TTL expiry so repeat views do not re-hit an API
 * that is currently rate-limited to 30 requests/minute.
 */
@Entity(tableName = "anilist_cache")
data class AniListCacheEntity(
    @PrimaryKey val anilistId: Int,
    val rawMetadataJson: String,
    val lastFetchedEpochMs: Long,
)
