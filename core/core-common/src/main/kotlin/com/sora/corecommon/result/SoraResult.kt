package com.sora.corecommon.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * The app's result wrapper.
 *
 * Named [SoraResult] rather than `Result` to avoid colliding with Kotlin's
 * built-in `kotlin.Result`, which cannot be used as a return type in many
 * positions and does not model a loading state.
 */
sealed interface SoraResult<out T> {
    data class Success<T>(val data: T) : SoraResult<T>
    data class Error(val cause: SoraError) : SoraResult<Nothing>
    data object Loading : SoraResult<Nothing>
}

/**
 * Domain error type.
 *
 * Modelled as a sealed hierarchy rather than a raw [Throwable] so the UI can
 * branch on *why* something failed without string-matching exceptions. This
 * matters for the brief's offline requirement: [Network] with cached data
 * available is a recoverable, silent condition, whereas the same exception
 * with no cache is a visible error state.
 */
sealed class SoraError(
    open val message: String?,
    open val throwable: Throwable? = null,
) {
    /** No connectivity, timeout, DNS failure, etc. */
    data class Network(
        override val message: String? = null,
        override val throwable: Throwable? = null,
    ) : SoraError(message, throwable)

    /** AniList returned 429. [retryAfterSeconds] comes from the Retry-After header. */
    data class RateLimited(
        val retryAfterSeconds: Int?,
        override val message: String? = null,
    ) : SoraError(message)

    /** Token missing, expired or rejected - the user must sign in again. */
    data class Unauthorized(
        override val message: String? = null,
    ) : SoraError(message)

    /** Local or server file could not be read. */
    data class Io(
        override val message: String? = null,
        override val throwable: Throwable? = null,
    ) : SoraError(message, throwable)

    /** A GraphQL response carried errors, or a payload failed to parse. */
    data class Api(
        override val message: String? = null,
        override val throwable: Throwable? = null,
    ) : SoraError(message, throwable)

    data class Unknown(
        override val message: String? = null,
        override val throwable: Throwable? = null,
    ) : SoraError(message, throwable)
}

/**
 * Wraps a Flow so it emits [SoraResult.Loading] first and converts thrown
 * exceptions into [SoraResult.Error] instead of cancelling the collector.
 */
fun <T> Flow<T>.asSoraResult(
    errorMapper: (Throwable) -> SoraError = { SoraError.Unknown(it.message, it) },
): Flow<SoraResult<T>> = map<T, SoraResult<T>> { SoraResult.Success(it) }
    .onStart { emit(SoraResult.Loading) }
    .catch { emit(SoraResult.Error(errorMapper(it))) }
