package com.sora.coredatabase.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sora.coremodel.MatchStatus
import com.sora.coremodel.MediaType
import com.sora.coremodel.SourceType

/**
 * A series in the user's library: one folder of content, matched (or not yet)
 * to an AniList record.
 *
 * Indexed on `anilistId` because the detail screen and the sync path both look
 * entries up by it, and on `rootPath` because incremental re-scans check
 * "have I seen this folder before?" for every discovered directory - a table
 * scan per folder would make re-scanning O(n*m).
 */
@Entity(
    tableName = "library_entries",
    indices = [
        Index(value = ["anilistId"]),
        Index(value = ["rootPath"], unique = true),
    ],
)
data class LibraryEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "anilistId") val anilistId: Int?,
    val type: MediaType,
    val title: String,
    val coverUrl: String?,
    val sourceType: SourceType,
    @ColumnInfo(name = "rootPath") val rootPath: String,
    val matchStatus: MatchStatus,

    /**
     * Last chapter number the user confirmed for a completed VOLUME unit in
     * this series.
     *
     * Not in the brief's schema, added per its instruction to "persist
     * whatever chapter number the user confirms so future syncs for the same
     * series have a reference point". Lets the volume-completion dialog
     * pre-fill a sensible guess instead of asking cold every time.
     */
    val lastConfirmedChapter: Float? = null,
)
