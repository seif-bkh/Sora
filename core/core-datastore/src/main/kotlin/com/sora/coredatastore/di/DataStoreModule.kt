package com.sora.coredatastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.sora.corecommon.dispatchers.ApplicationScope
import com.sora.coredatastore.AuthTokens
import com.sora.coredatastore.AuthTokensSerializer
import com.sora.coredatastore.UserSettings
import com.sora.coredatastore.UserSettingsSerializer
import com.sora.coredatastore.crypto.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesCryptoManager(
        @ApplicationContext context: Context,
    ): CryptoManager = CryptoManager(context)

    @Provides
    @Singleton
    fun providesUserSettingsDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        serializer: UserSettingsSerializer,
    ): DataStore<UserSettings> = DataStoreFactory.create(
        serializer = serializer,
        scope = scope,
        produceFile = { context.dataStoreFile(USER_SETTINGS_FILE) },
    )

    /**
     * Separate store from settings so Tink encryption applies only to the
     * secrets - settings reads stay cheap and unencrypted.
     */
    @Provides
    @Singleton
    fun providesAuthTokensDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        serializer: AuthTokensSerializer,
    ): DataStore<AuthTokens> = DataStoreFactory.create(
        serializer = serializer,
        scope = scope,
        produceFile = { context.dataStoreFile(AUTH_TOKENS_FILE) },
    )

    private const val USER_SETTINGS_FILE = "sora_user_settings.pb"
    private const val AUTH_TOKENS_FILE = "sora_auth_tokens.pb"
}
