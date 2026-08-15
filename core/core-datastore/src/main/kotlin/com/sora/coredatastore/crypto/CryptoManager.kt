package com.sora.coredatastore.crypto

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the AEAD primitive used to encrypt the auth-token DataStore.
 *
 * WHY TINK DIRECTLY
 *   * `androidx.security:security-crypto` (EncryptedFile /
 *     EncryptedSharedPreferences) was deprecated in April 2025 and is no
 *     longer maintained.
 *   * `androidx.datastore:datastore-tink` provides an official AeadSerializer
 *     but is alpha-only; the brief's stack calls for stable libraries.
 *   Using Tink directly is what both of those wrap anyway, and keeps the
 *   dependency stable. Documented in DECISIONS.md.
 *
 * KEY STORAGE
 *   The data-encryption key lives in a Tink keyset, and that keyset is itself
 *   encrypted by a master key held in the Android Keystore - hardware-backed
 *   where available. The app never sees raw key material, and the keyset is
 *   useless if copied off the device.
 */
@Singleton
class CryptoManager @Inject constructor(
    private val context: Context,
) {

    /**
     * Built lazily and cached: `AndroidKeysetManager` touches the Keystore and
     * reads a file, so it must not run on the main thread or be rebuilt per
     * read. Callers reach it from a DataStore serializer, which already
     * executes on a background dispatcher.
     */
    val aead: Aead by lazy {
        AeadConfig.register()

        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    private companion object {
        const val KEYSET_NAME = "sora_auth_keyset"
        const val PREF_FILE_NAME = "sora_auth_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://sora_auth_master_key"

        /**
         * AES256-GCM: authenticated encryption, so tampering with the file is
         * detected rather than silently decrypting to garbage.
         */
        const val KEY_TEMPLATE = "AES256_GCM"
    }
}
