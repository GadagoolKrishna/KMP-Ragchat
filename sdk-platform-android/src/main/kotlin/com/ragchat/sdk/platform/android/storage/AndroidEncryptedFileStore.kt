package com.ragchat.sdk.platform.android.storage

import android.content.Context
import com.ragchat.sdk.api.SdkError
import com.ragchat.sdk.api.SecureStorage
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android implementation of [SecureStorage] using AES-256-GCM envelope encryption.
 * Files are stored in an app-private sandbox directory and excluded from cloud backups.
 */
public class AndroidEncryptedFileStore(
    private val context: Context,
    private val keystoreKeyProvider: AndroidKeystoreKeyProvider,
    private val baseDirName: String = "ragchat_storage"
) : SecureStorage {

    private val secureRandom = SecureRandom()
    private val rootDir: File = File(context.filesDir, baseDirName).apply {
        if (!exists()) mkdirs()
    }

    private fun getStoragePath(key: String): File {
        val sanitizedKey = key.replace("/", "_").replace("\\", "_")
        return File(rootDir, "$sanitizedKey.enc")
    }

    override suspend fun store(key: String, value: ByteArray) {
        val file = getStoragePath(key)
        val dek = ByteArray(32).also { secureRandom.nextBytes(it) }
        val encryptedDek = keystoreKeyProvider.encrypt("dek_$key", dek)

        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(dek, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(value)

        // File format: [4 bytes header DEK length] + [encrypted DEK] + [12 bytes IV] + [Ciphertext + Tag]
        file.outputStream().use { fos ->
            val dekLen = encryptedDek.size
            fos.write((dekLen shr 24) and 0xFF)
            fos.write((dekLen shr 16) and 0xFF)
            fos.write((dekLen shr 8) and 0xFF)
            fos.write(dekLen and 0xFF)
            fos.write(encryptedDek)
            fos.write(iv)
            fos.write(ciphertext)
            fos.flush()
        }
    }

    override suspend fun retrieve(key: String): ByteArray? {
        val file = getStoragePath(key)
        if (!file.exists()) return null

        val bytes = file.readBytes()
        if (bytes.size < 4 + 12) {
            throw SdkError.StorageError("read", "Encrypted file corrupted: payload too small")
        }

        var offset = 0
        val dekLen = ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
        offset += 4

        if (bytes.size < offset + dekLen + 12) {
            throw SdkError.StorageError("read", "Encrypted file corrupted: invalid DEK length header")
        }

        val encryptedDek = bytes.copyOfRange(offset, offset + dekLen)
        offset += dekLen

        val iv = bytes.copyOfRange(offset, offset + 12)
        offset += 12

        val ciphertext = bytes.copyOfRange(offset, bytes.size)

        val dek = keystoreKeyProvider.decrypt("dek_$key", encryptedDek)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(dek, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    override suspend fun remove(key: String) {
        val file = getStoragePath(key)
        if (file.exists()) {
            file.delete()
        }
        keystoreKeyProvider.deleteKey("dek_$key")
    }

    public suspend fun clear() {
        rootDir.listFiles()?.forEach { it.delete() }
    }
}
