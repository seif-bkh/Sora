package com.sora.corecommon.dispatchers

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Dispatcher qualifiers.
 *
 * Injecting dispatchers rather than hardcoding `Dispatchers.IO` at call sites
 * is what makes the data layer testable: tests substitute a
 * `TestDispatcher` and get deterministic, virtual-time execution.
 */
@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val dispatcher: SoraDispatcher)

enum class SoraDispatcher {
    /** Disk and network work: Room, DataStore, OkHttp, CBZ extraction. */
    IO,

    /** CPU-bound work: filename parsing, fuzzy match scoring, image decode. */
    Default,
}

/**
 * Injectable scope for work that must outlive a ViewModel - e.g. flushing a
 * progress update to AniList as the user leaves the reader.
 */
@Qualifier
@Retention(RUNTIME)
annotation class ApplicationScope
