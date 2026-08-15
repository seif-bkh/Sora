package com.sora.coredatabase.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sora.coredatabase.entity.MatchCandidateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchCandidateDao {

    /** Highest-confidence candidates first, as shown in the review picker. */
    @Query(
        "SELECT * FROM match_candidates WHERE libraryEntryId = :libraryEntryId " +
            "ORDER BY confidenceScore DESC",
    )
    fun observeForEntry(libraryEntryId: String): Flow<List<MatchCandidateEntity>>

    @Query(
        "SELECT * FROM match_candidates WHERE libraryEntryId = :libraryEntryId " +
            "ORDER BY confidenceScore DESC",
    )
    suspend fun getForEntry(libraryEntryId: String): List<MatchCandidateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candidates: List<MatchCandidateEntity>)

    /** Cleared once the user confirms a match - the choice is no longer open. */
    @Query("DELETE FROM match_candidates WHERE libraryEntryId = :libraryEntryId")
    suspend fun deleteForEntry(libraryEntryId: String)
}
