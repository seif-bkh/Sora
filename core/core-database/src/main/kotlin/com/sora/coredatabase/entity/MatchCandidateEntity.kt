package com.sora.coredatabase.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A possible AniList match for an unmatched library entry, awaiting user
 * review.
 *
 * Rows are produced by the matching pipeline (Phase 4) when the fuzzy scorer's
 * confidence falls below the auto-confirm threshold, and are shown in the
 * review UI as a horizontal poster picker.
 */
@Entity(
    tableName = "match_candidates",
    foreignKeys = [
        ForeignKey(
            entity = LibraryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["libraryEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["libraryEntryId"])],
)
data class MatchCandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val libraryEntryId: String,
    val anilistId: Int,

    /** Jaro-Winkler / normalised Levenshtein score, 0f..1f. */
    val confidenceScore: Float,

    /**
     * Denormalised so the review UI can render posters without a second
     * AniList round-trip per candidate - five candidates per unmatched series
     * would otherwise blow the rate limit during a large first scan.
     */
    val candidateTitle: String? = null,
    val candidateCoverUrl: String? = null,
)
