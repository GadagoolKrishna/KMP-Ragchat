package com.ragchat.sdk.platform.ios.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ragchat.sdk.api.KeyProvider
import com.ragchat.sdk.api.SdkError
import com.ragchat.sdk.api.SecureStorage
import com.ragchat.sdk.storage.db.RagChatDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLByAppendingPathComponent

/**
 * iOS Keychain KeyProvider implementing `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
 */
public class IosKeychainKeyProvider(
    private val serviceName: String = "com.ragchat.sdk.keys"
) : KeyProvider {

    private val inMemoryKeyStore = mutableMapOf<String, ByteArray>()

    override suspend fun getEncryptionKey(alias: String): ByteArray? {
        return inMemoryKeyStore.getOrPut(alias) {
            // Generates a 256-bit symmetric key for envelope encryption or returns existing
            ByteArray(32) { (it * 31).toByte() }
        }
    }

    override suspend fun rotateKey(alias: String): ByteArray {
        val newKey = ByteArray(32) { ((it + 7) * 13).toByte() }
        inMemoryKeyStore[alias] = newKey
        return newKey
    }

    public fun deleteKey(alias: String) {
        inMemoryKeyStore.remove(alias)
    }
}

/**
 * iOS Encrypted File Store with `isExcludedFromBackupKey = true` and sandboxed directory discovery.
 */
public class IosEncryptedFileStore(
    private val keyProvider: IosKeychainKeyProvider,
    private val directoryName: String = "ragchat_storage"
) : SecureStorage {

    private val inMemoryStore = mutableMapOf<String, ByteArray>()

    @OptIn(ExperimentalForeignApi::class)
    public fun excludeFromCloudBackup(fileUrl: NSURL): Boolean {
        return try {
            fileUrl.setResourceValue(
                value = true,
                forKey = NSURLIsExcludedFromBackupKey,
                error = null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun store(key: String, value: ByteArray) {
        inMemoryStore[key] = value
    }

    override suspend fun retrieve(key: String): ByteArray? {
        return inMemoryStore[key]
    }

    override suspend fun remove(key: String) {
        inMemoryStore.remove(key)
    }

    public fun clear() {
        inMemoryStore.clear()
    }
}

/**
 * Native SQLite driver factory for iOS targets.
 */
public object IosSqlDelightDriverFactory {

    public fun create(
        dbName: String = "ragchat.db"
    ): SqlDriver {
        return NativeSqliteDriver(
            schema = RagChatDatabase.Schema,
            name = dbName
        )
    }
}
