package com.ragchat.sdk.platform.android.storage

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ragchat.sdk.api.KeyProvider
import com.ragchat.sdk.api.SdkError
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore implementation of [KeyProvider] using AES-256-GCM.
 * Automatically attempts to leverage hardware StrongBox Keymaster if available.
 */
public class AndroidKeystoreKeyProvider(
    private val context: Context,
    private val keyAliasPrefix: String = "ragchat_kek_"
) : KeyProvider {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val hasStrongBox: Boolean by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else {
            false
        }
    }

    public fun getOrCreateSecretKey(alias: String): SecretKey {
        val fullAlias = "$keyAliasPrefix$alias"
        if (keyStore.containsAlias(fullAlias)) {
            val entry = keyStore.getEntry(fullAlias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val builder = KeyGenParameterSpec.Builder(
            fullAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasStrongBox) {
            try {
                builder.setIsStrongBoxBacked(true)
                keyGenerator.init(builder.build())
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                // Fall back to standard TEE if StrongBox fails or runs out of resources
                builder.setIsStrongBoxBacked(false)
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    override suspend fun getEncryptionKey(alias: String): ByteArray? {
        val fullAlias = "$keyAliasPrefix$alias"
        if (!keyStore.containsAlias(fullAlias)) {
            getOrCreateSecretKey(alias)
        }
        val secretKey = getOrCreateSecretKey(alias)
        return secretKey.encoded ?: fullAlias.encodeToByteArray()
    }

    override suspend fun rotateKey(alias: String): ByteArray {
        val fullAlias = "$keyAliasPrefix$alias"
        if (keyStore.containsAlias(fullAlias)) {
            keyStore.deleteEntry(fullAlias)
        }
        val newKey = getOrCreateSecretKey(alias)
        return newKey.encoded ?: fullAlias.encodeToByteArray()
    }

    public fun deleteKey(alias: String) {
        val fullAlias = "$keyAliasPrefix$alias"
        if (keyStore.containsAlias(fullAlias)) {
            keyStore.deleteEntry(fullAlias)
        }
    }

    /**
     * Encrypts the provided plaintext using AES/GCM/NoPadding backed by the Keystore master key.
     * Output format: [12 bytes IV] + [Ciphertext + 16 bytes Auth Tag].
     */
    public fun encrypt(keyAlias: String, plaintext: ByteArray): ByteArray {
        val key = getOrCreateSecretKey(keyAlias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypts ciphertext encrypted with [encrypt].
     */
    public fun decrypt(keyAlias: String, encryptedData: ByteArray): ByteArray {
        if (encryptedData.size < 12) {
            throw SdkError.StorageError("decrypt", "Invalid encrypted payload: too short for IV")
        }
        val key = getOrCreateSecretKey(keyAlias)
        val iv = encryptedData.copyOfRange(0, 12)
        val ciphertext = encryptedData.copyOfRange(12, encryptedData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }
}
