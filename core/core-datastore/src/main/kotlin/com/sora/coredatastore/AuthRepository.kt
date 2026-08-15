package com.sora.coredatastore

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Access to encrypted credentials.
 *
 * Everything here is backed by the Tink-encrypted DataStore. Callers should
 * prefer [isSignedIn] over reading the token when they only need auth state,
 * so the secret is not passed around unnecessarily.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<AuthTokens>,
) {

    val authTokens: Flow<AuthTokens> = dataStore.data

    /**
     * True when a non-empty, unexpired token is stored.
     *
     * `expiresAtEpochMs == 0` means "no expiry recorded" and is treated as
     * valid: AniList implicit-grant tokens are long-lived, and locking a user
     * out because the expiry was never returned would be worse than letting a
     * 401 surface naturally.
     */
    val isSignedIn: Flow<Boolean> = dataStore.data.map { tokens ->
        tokens.accessToken.isNotEmpty() &&
            (tokens.expiresAtEpochMs == 0L || tokens.expiresAtEpochMs > System.currentTimeMillis())
    }

    val aniListUserId: Flow<Int?> = dataStore.data.map { tokens ->
        tokens.anilistUserId.takeIf { it != 0 }
    }

    suspend fun saveAniListToken(
        accessToken: String,
        expiresAtEpochMs: Long,
    ) {
        dataStore.updateData {
            it.toBuilder()
                .setAccessToken(accessToken)
                .setExpiresAtEpochMs(expiresAtEpochMs)
                .build()
        }
    }

    suspend fun saveAniListUser(userId: Int, userName: String) {
        dataStore.updateData {
            it.toBuilder()
                .setAnilistUserId(userId)
                .setAnilistUserName(userName)
                .build()
        }
    }

    suspend fun saveServerPassword(password: String) {
        dataStore.updateData { it.toBuilder().setServerPassword(password).build() }
    }

    /**
     * Clears AniList credentials on sign-out.
     *
     * Deliberately preserves the server password: signing out of AniList is
     * unrelated to the user's WebDAV server, and wiping it would silently
     * break their local library.
     */
    suspend fun clearAniListSession() {
        dataStore.updateData { current ->
            current.toBuilder()
                .clearAccessToken()
                .clearExpiresAtEpochMs()
                .clearAnilistUserId()
                .clearAnilistUserName()
                .build()
        }
    }
}
