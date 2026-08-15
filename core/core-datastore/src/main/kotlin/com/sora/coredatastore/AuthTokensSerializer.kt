package com.sora.coredatastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.sora.coredatastore.crypto.CryptoManager
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import javax.inject.Inject

/**
 * Encrypted Proto DataStore serializer for [AuthTokens].
 *
 * The whole file is encrypted with Tink AES256-GCM before it touches disk, so
 * the AniList access token and the server password are never at rest in
 * plaintext (project brief: "store it encrypted via DataStore").
 *
 * ASSOCIATED DATA
 *   The file name is passed as AEAD associated data. It is not secret; it
 *   binds the ciphertext to this particular file, so an attacker cannot swap
 *   in a valid-but-different encrypted blob from elsewhere in the app and
 *   have it decrypt successfully.
 */
class AuthTokensSerializer @Inject constructor(
    private val cryptoManager: CryptoManager,
) : Serializer<AuthTokens> {

    /** Empty tokens = signed out. */
    override val defaultValue: AuthTokens = AuthTokens.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AuthTokens =
        try {
            val ciphertext = input.readBytes()
            // A zero-length file is a fresh install, not corruption.
            if (ciphertext.isEmpty()) {
                defaultValue
            } else {
                AuthTokens.parseFrom(cryptoManager.aead.decrypt(ciphertext, ASSOCIATED_DATA))
            }
        } catch (exception: GeneralSecurityException) {
            // Wrong key or tampered file. Surfaced as corruption so DataStore
            // falls back to the default (signed out) and the user re-auths,
            // rather than the app crash-looping on launch.
            throw CorruptionException("Unable to decrypt AuthTokens", exception)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to parse AuthTokens", exception)
        }

    override suspend fun writeTo(t: AuthTokens, output: OutputStream) {
        output.write(cryptoManager.aead.encrypt(t.toByteArray(), ASSOCIATED_DATA))
    }

    private companion object {
        val ASSOCIATED_DATA: ByteArray = "sora_auth_tokens.pb".toByteArray()
    }
}
