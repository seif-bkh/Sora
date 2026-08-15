package com.sora.coredatabase.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sora.coremodel.UnitType

/**
 * One playable/readable unit: an episode, a chapter, or a whole volume.
 *
 * A foreign key with CASCADE delete means removing a library entry cleans up
 * its units automatically - without it, deleting a series would silently
 * orphan every row here.
 */
@Entity(
    tableName = "media_units",
    foreignKeys = [
        ForeignKey(
            entity = LibraryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["libraryEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Required by the FK, and by the "units for this series" query the
        // detail screen observes.
        Index(value = ["libraryEntryId"]),
        // Incremental re-scans look units up by path to skip known files.
        Index(value = ["path"], unique = true),
    ],
)
data class MediaUnitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "libraryEntryId") val libraryEntryId: String,
    val unitType: UnitType,

    /** Episode/chapter number, or volume number for VOLUME units. Float allows x.5. */
    val number: Float,

    /** VOLUME only: first chapter contained, null when undetermined. */
    val chapterRangeStart: Float?,

    /** VOLUME only: last chapter contained, null when undetermined. */
    val chapterRangeEnd: Float?,

    val title: String?,
    val path: String,

    /** Manga only: page count in the archive/folder. Null for anime. */
    val totalPages: Int?,

    /** Manga only: last-read page, for resume. Null for anime. */
    val currentPage: Int?,

    val isWatchedOrRead: Boolean,

    /** Video position % or page position %, 0f..1f. */
    val progressPercent: Float,

    /** Video resume position. Null for manga. */
    val lastPositionMs: Long?,
)
