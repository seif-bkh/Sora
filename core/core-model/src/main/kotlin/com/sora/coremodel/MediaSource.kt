package com.sora.coremodel

import android.net.Uri

/**
 * The single abstraction over "where content comes from".
 *
 * MODULE BOUNDARY (project brief): `feature-player` and `feature-reader`
 * depend on this interface only - never on `source-local` or `source-server`
 * concretes. Implementations are bound into the Hilt graph by `:app`. That is
 * what allows a new source (including, hypothetically, a torrent source -
 * explicitly out of scope for this build) to be added without touching the
 * player, reader or UI layers.
 *
 * Every method is `suspend`: implementations perform disk or network I/O and
 * must never run on the main thread (enforced by StrictMode in debug builds).
 */
interface MediaSource {

    /** Identifies which concrete implementation this is. */
    val sourceType: SourceType

    /**
     * Enumerates candidate series under this source's configured roots -
     * typically one per folder.
     */
    suspend fun listSeries(): List<DiscoveredSeries>

    /**
     * Lists the playable/readable units within a series.
     *
     * @param seriesPath as reported by [DiscoveredSeries.path].
     */
    suspend fun listUnits(seriesPath: String): List<DiscoveredUnit>

    /**
     * Resolves a unit into something the player or reader can consume.
     *
     * For manga this may mean opening a CBZ archive and enumerating its
     * entries, so it can be expensive - call it off the main thread and cache
     * the result for the lifetime of a reading session.
     */
    suspend fun resolve(unitPath: String): PlayableOrReadable
}

/**
 * The result of resolving a unit.
 *
 * Note this is intentionally *not* split by MediaType: a video is a stream, a
 * manga unit is a page sequence, and the reader treats folder-of-images,
 * chapter-CBZ and volume-CBZ identically as "a sequence of pages".
 */
sealed class PlayableOrReadable {

    /** A single video file, optionally with a known MIME type. */
    data class VideoStream(
        val uri: Uri,
        val mimeType: String?,
    ) : PlayableOrReadable()

    /**
     * An ordered sequence of page images.
     *
     * Pages are pre-sorted in reading order by the implementation. Callers
     * must not assume the URIs point at real files: for a CBZ they may be
     * content URIs served by an in-process provider or extracted cache
     * entries.
     */
    data class ImagePages(
        val pageUris: List<Uri>,
    ) : PlayableOrReadable()
}

/**
 * A series discovered by a scan, before any AniList matching.
 *
 * This is raw scanner output: [title] is a best-effort parse from the folder
 * or file name and is expected to be noisy.
 */
data class DiscoveredSeries(
    val path: String,
    val title: String,
    val mediaType: MediaType,
    val unitCount: Int,
)

/**
 * A unit discovered by a scan, before persistence.
 *
 * [chapterRangeStart] / [chapterRangeEnd] are populated only for [UnitType]
 * VOLUME, and only when the filename or metadata makes the range knowable.
 * They stay null when undetermined - the app must not guess silently, and
 * asks the user to confirm the chapter number on volume completion instead.
 */
data class DiscoveredUnit(
    val path: String,
    val unitType: UnitType,
    val number: Float,
    val title: String?,
    val chapterRangeStart: Float? = null,
    val chapterRangeEnd: Float? = null,
)
