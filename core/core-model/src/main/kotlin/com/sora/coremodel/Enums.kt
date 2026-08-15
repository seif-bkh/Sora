package com.sora.coremodel

/** Anime (video) vs manga (images). */
enum class MediaType {
    ANIME,
    MANGA,
}

/**
 * Where content physically lives.
 *
 * Deliberately open to extension: the brief forbids torrent support in this
 * build, but requires the abstraction to accommodate a new source later
 * without refactoring the player, reader or UI. Adding a value here plus a
 * [MediaSource] implementation is the whole extension surface.
 */
enum class SourceType {
    LOCAL,
    SERVER,
}

/** How confident we are that a library entry maps to an AniList record. */
enum class MatchStatus {
    /** No AniList match attempted or none found. */
    UNMATCHED,

    /** Matched by the scorer above the auto-confirm threshold. */
    AUTO_MATCHED,

    /** Explicitly confirmed by the user; re-scans must not overwrite this. */
    CONFIRMED,
}

/**
 * The granularity of a single playable/readable unit.
 *
 * CHAPTER and VOLUME both exist because manga is distributed both ways (see
 * DECISIONS.md and the reader notes): one CBZ may be a single chapter or a
 * whole volume bundling 8-12 chapters. The distinction matters only at the
 * AniList sync boundary, since AniList tracks progress by chapter number.
 */
enum class UnitType {
    EPISODE,
    CHAPTER,
    VOLUME,
}
