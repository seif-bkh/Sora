package com.sora.coredatastore

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to user settings.
 *
 * Exposes a Flow so the theme, reading mode and sync preferences propagate
 * reactively - changing the theme in settings recomposes the whole app with
 * no manual plumbing.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<UserSettings>,
) {

    val userSettings: Flow<UserSettings> = dataStore.data

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { it.toBuilder().setThemeMode(mode).build() }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setUseDynamicColor(enabled).build() }
    }

    suspend fun setDefaultReadingMode(mode: ReadingMode) {
        dataStore.updateData { it.toBuilder().setDefaultReadingMode(mode).build() }
    }

    suspend fun setPlaybackSyncThreshold(threshold: Float) {
        dataStore.updateData {
            it.toBuilder().setPlaybackSyncThreshold(threshold.coerceIn(0f, 1f)).build()
        }
    }

    suspend fun setSyncProgressToAniList(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setSyncProgressToAnilist(enabled).build() }
    }

    /**
     * Records a SAF tree the user granted. Persisted so a scan can re-open the
     * folder after process death without re-prompting.
     */
    suspend fun addLocalLibraryTreeUri(uri: String) {
        dataStore.updateData { current ->
            if (current.localLibraryTreeUrisList.contains(uri)) {
                current
            } else {
                current.toBuilder().addLocalLibraryTreeUris(uri).build()
            }
        }
    }

    suspend fun removeLocalLibraryTreeUri(uri: String) {
        dataStore.updateData { current ->
            val remaining = current.localLibraryTreeUrisList.filterNot { it == uri }
            current.toBuilder()
                .clearLocalLibraryTreeUris()
                .addAllLocalLibraryTreeUris(remaining)
                .build()
        }
    }

    /** Note: the server password is a credential and lives in [AuthRepository]. */
    suspend fun setServerConfig(baseUrl: String, username: String, enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setServerConfig(
                    ServerConfig.newBuilder()
                        .setBaseUrl(baseUrl)
                        .setUsername(username)
                        .setEnabled(enabled)
                        .build(),
                )
                .build()
        }
    }
}
