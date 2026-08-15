package com.sora.coredatabase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.sora.coredatabase.entity.LibraryEntryEntity
import com.sora.coredatabase.entity.MatchCandidateEntity
import com.sora.coredatabase.entity.MediaUnitEntity
import com.sora.coremodel.MatchStatus
import com.sora.coremodel.MediaType
import com.sora.coremodel.SourceType
import com.sora.coremodel.UnitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * DAO behaviour tests against an in-memory Room database.
 *
 * Flow-returning queries are verified with Turbine (the brief's Flow-testing
 * choice) to prove the UI actually receives updates when the data changes -
 * a query that returns correct data once but never re-emits would pass a
 * naive test and leave the library grid stale after a scan.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SoraDaoTest {

    private lateinit var db: SoraDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SoraDatabase::class.java,
        )
            // Room enables PRAGMA foreign_keys itself, so the CASCADE deletes
            // declared on the entities are enforced here.
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun libraryEntries_observeAll_emitsOnInsert() = runTest {
        val dao = db.libraryEntryDao()

        dao.observeAll().test {
            assertEquals(emptyList<LibraryEntryEntity>(), awaitItem())

            dao.upsertAll(listOf(entry(id = "a", title = "Bocchi the Rock")))

            val afterInsert = awaitItem()
            assertEquals(1, afterInsert.size)
            assertEquals("Bocchi the Rock", afterInsert.first().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun libraryEntries_observeAll_sortsByTitleCaseInsensitively() = runTest {
        val dao = db.libraryEntryDao()
        dao.upsertAll(
            listOf(
                entry(id = "1", title = "zombie land saga", rootPath = "/1"),
                entry(id = "2", title = "Akira", rootPath = "/2"),
                entry(id = "3", title = "berserk", rootPath = "/3"),
            ),
        )

        val titles = dao.observeAll().first().map(LibraryEntryEntity::title)

        // COLLATE NOCASE: lowercase "berserk" must sort before "zombie...",
        // not after every capitalised title as binary collation would do.
        assertEquals(listOf("Akira", "berserk", "zombie land saga"), titles)
    }

    @Test
    fun libraryEntries_observeNeedingReview_excludesConfirmed() = runTest {
        val dao = db.libraryEntryDao()
        dao.upsertAll(
            listOf(
                entry(id = "1", rootPath = "/1", matchStatus = MatchStatus.CONFIRMED),
                entry(id = "2", rootPath = "/2", matchStatus = MatchStatus.UNMATCHED),
                entry(id = "3", rootPath = "/3", matchStatus = MatchStatus.AUTO_MATCHED),
            ),
        )

        val needingReview = dao.observeNeedingReview().first().map(LibraryEntryEntity::id)

        assertEquals(setOf("2", "3"), needingReview.toSet())
    }

    @Test
    fun libraryEntries_applyMatch_setsConfirmedSoRescansSkipIt() = runTest {
        val dao = db.libraryEntryDao()
        dao.upsertAll(listOf(entry(id = "1", matchStatus = MatchStatus.UNMATCHED)))

        dao.applyMatch(id = "1", anilistId = 999, coverUrl = "https://cover")

        val updated = dao.getById("1")
        assertNotNull(updated)
        assertEquals(999, updated!!.anilistId)
        assertEquals("https://cover", updated.coverUrl)
        assertEquals(MatchStatus.CONFIRMED, updated.matchStatus)
    }

    @Test
    fun mediaUnits_observeForEntry_ordersByNumberIncludingDecimals() = runTest {
        val entryDao = db.libraryEntryDao()
        val unitDao = db.mediaUnitDao()
        entryDao.upsertAll(listOf(entry(id = "e1")))

        unitDao.upsertAll(
            listOf(
                unit(id = "u3", entryId = "e1", number = 13f, path = "/13"),
                unit(id = "u1", entryId = "e1", number = 12f, path = "/12"),
                // Float numbering exists precisely for specials like 12.5.
                unit(id = "u2", entryId = "e1", number = 12.5f, path = "/12.5"),
            ),
        )

        val numbers = unitDao.observeForEntry("e1").first().map(MediaUnitEntity::number)

        assertEquals(listOf(12f, 12.5f, 13f), numbers)
    }

    @Test
    fun mediaUnits_updateReadProgress_persistsPagePositionForResume() = runTest {
        val entryDao = db.libraryEntryDao()
        val unitDao = db.mediaUnitDao()
        entryDao.upsertAll(listOf(entry(id = "e1", type = MediaType.MANGA)))
        unitDao.upsertAll(
            listOf(
                unit(
                    id = "vol3",
                    entryId = "e1",
                    number = 3f,
                    path = "/vol3.cbz",
                    unitType = UnitType.VOLUME,
                    totalPages = 310,
                ),
            ),
        )

        // Mid-volume: a VOLUME unit spans sessions, so this must survive.
        unitDao.updateReadProgress(
            id = "vol3",
            currentPage = 142,
            totalPages = 310,
            progressPercent = 142f / 310f,
            read = false,
        )

        val resumed = unitDao.getById("vol3")!!
        assertEquals(142, resumed.currentPage)
        assertEquals(310, resumed.totalPages)
        assertTrue(!resumed.isWatchedOrRead)
    }

    @Test
    fun mediaUnits_getNextUnit_returnsFollowingNumber() = runTest {
        val entryDao = db.libraryEntryDao()
        val unitDao = db.mediaUnitDao()
        entryDao.upsertAll(listOf(entry(id = "e1")))
        unitDao.upsertAll(
            listOf(
                unit(id = "u1", entryId = "e1", number = 1f, path = "/1"),
                unit(id = "u2", entryId = "e1", number = 2f, path = "/2"),
            ),
        )

        assertEquals("u2", unitDao.getNextUnit("e1", 1f)?.id)
        assertNull("no unit after the last one", unitDao.getNextUnit("e1", 2f))
        assertEquals("u1", unitDao.getPreviousUnit("e1", 2f)?.id)
    }

    @Test
    fun deletingEntry_cascadesToUnitsAndCandidates() = runTest {
        val entryDao = db.libraryEntryDao()
        val unitDao = db.mediaUnitDao()
        val candidateDao = db.matchCandidateDao()

        entryDao.upsertAll(listOf(entry(id = "e1")))
        unitDao.upsertAll(listOf(unit(id = "u1", entryId = "e1", number = 1f, path = "/1")))
        candidateDao.insertAll(
            listOf(MatchCandidateEntity(libraryEntryId = "e1", anilistId = 1, confidenceScore = 0.7f)),
        )

        entryDao.deleteById("e1")

        // Without ON DELETE CASCADE these rows would be orphaned silently.
        assertNull(unitDao.getById("u1"))
        assertEquals(emptyList<MatchCandidateEntity>(), candidateDao.getForEntry("e1"))
    }

    @Test
    fun aniListCache_getIfFresh_respectsTtlBoundary() = runTest {
        val dao = db.aniListCacheDao()
        val now = 1_000_000L
        dao.upsert(
            com.sora.coredatabase.entity.AniListCacheEntity(
                anilistId = 42,
                rawMetadataJson = """{"id":42}""",
                lastFetchedEpochMs = now,
            ),
        )

        // Fetched at or after the cutoff -> cache hit.
        assertNotNull(dao.getIfFresh(42, minEpochMs = now))
        assertNotNull(dao.getIfFresh(42, minEpochMs = now - 1))
        // Older than the cutoff -> treated as a miss so the caller refetches.
        assertNull(dao.getIfFresh(42, minEpochMs = now + 1))
    }

    @Test
    fun matchCandidates_observeForEntry_ordersByConfidenceDescending() = runTest {
        val entryDao = db.libraryEntryDao()
        val candidateDao = db.matchCandidateDao()
        entryDao.upsertAll(listOf(entry(id = "e1")))

        candidateDao.insertAll(
            listOf(
                MatchCandidateEntity(libraryEntryId = "e1", anilistId = 1, confidenceScore = 0.55f),
                MatchCandidateEntity(libraryEntryId = "e1", anilistId = 2, confidenceScore = 0.91f),
                MatchCandidateEntity(libraryEntryId = "e1", anilistId = 3, confidenceScore = 0.73f),
            ),
        )

        val ordered = candidateDao.observeForEntry("e1").first().map(MatchCandidateEntity::anilistId)

        // Best guess first - this is the order the review picker shows.
        assertEquals(listOf(2, 3, 1), ordered)
    }

    // --- fixtures ---------------------------------------------------------

    private fun entry(
        id: String,
        title: String = "Title $id",
        rootPath: String = "/root/$id",
        type: MediaType = MediaType.ANIME,
        matchStatus: MatchStatus = MatchStatus.UNMATCHED,
    ) = LibraryEntryEntity(
        id = id,
        anilistId = null,
        type = type,
        title = title,
        coverUrl = null,
        sourceType = SourceType.LOCAL,
        rootPath = rootPath,
        matchStatus = matchStatus,
    )

    private fun unit(
        id: String,
        entryId: String,
        number: Float,
        path: String,
        unitType: UnitType = UnitType.EPISODE,
        totalPages: Int? = null,
    ) = MediaUnitEntity(
        id = id,
        libraryEntryId = entryId,
        unitType = unitType,
        number = number,
        chapterRangeStart = null,
        chapterRangeEnd = null,
        title = null,
        path = path,
        totalPages = totalPages,
        currentPage = null,
        isWatchedOrRead = false,
        progressPercent = 0f,
        lastPositionMs = null,
    )
}
