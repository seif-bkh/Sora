package com.sora.coredatastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * Proto DataStore serializer for [UserSettings].
 *
 * Plaintext: these are preferences, not secrets. Credentials live in the
 * separately-encrypted AuthTokens store.
 */
class UserSettingsSerializer @Inject constructor() : Serializer<UserSettings> {

    /**
     * Defaults applied on first run and whenever a field is unset.
     *
     * Dark theme and dynamic colour are on by default per the brief; the
     * playback sync threshold matches its suggested 0.9.
     */
    override val defaultValue: UserSettings = UserSettings.newBuilder()
        .setThemeMode(ThemeMode.THEME_MODE_DARK)
        .setUseDynamicColor(true)
        .setDefaultReadingMode(ReadingMode.READING_MODE_PAGED)
        .setPlaybackSyncThreshold(DEFAULT_PLAYBACK_SYNC_THRESHOLD)
        .setSyncProgressToAnilist(true)
        .build()

    override suspend fun readFrom(input: InputStream): UserSettings =
        try {
            UserSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            // DataStore catches CorruptionException and falls back to
            // defaultValue rather than crashing the app on a truncated file.
            throw CorruptionException("Unable to read UserSettings", exception)
        }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        t.writeTo(output)
    }

    private companion object {
        const val DEFAULT_PLAYBACK_SYNC_THRESHOLD = 0.9f
    }
}
