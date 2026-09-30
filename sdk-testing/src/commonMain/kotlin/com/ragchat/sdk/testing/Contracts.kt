package com.ragchat.sdk.testing

import com.ragchat.sdk.api.Chunk
import com.ragchat.sdk.api.EmbeddingVector
import com.ragchat.sdk.api.SecureStorage
import com.ragchat.sdk.api.VectorStore

/**
 * Shared abstract contract test suite for VectorStore implementations.
 * Platform and module test targets inherit from this contract in commonTest or platform tests.
 */
public abstract class VectorStoreContract {
    public abstract fun createStore(): VectorStore

    public suspend fun verifyUpsertAndSearch() {
        val store = createStore()
        val collectionId = "test-collection"
        val chunk = Chunk("c1", "doc1", "Sample text", 0, 2)
        val vec = EmbeddingVector(2, listOf(1.0f, 0.0f))
        store.upsert(collectionId, chunk, vec)

        val results = store.search(collectionId, vec, 1)
        check(results.size == 1) { "Expected 1 search result but got ${results.size}" }
        check(results.first().chunk.id == "c1") { "Expected chunk id c1" }
    }
}

/**
 * Shared abstract contract test suite for SecureStorage implementations.
 */
public abstract class SecureStorageContract {
    public abstract fun createStorage(): SecureStorage

    public suspend fun verifyStoreAndRetrieveKey() {
        val storage = createStorage()
        val keyData = byteArrayOf(1, 2, 3, 4)
        storage.store("master_key", keyData)

        val retrieved = storage.retrieve("master_key")
        check(retrieved != null) { "Expected non-null retrieved key" }
        check(retrieved.size == keyData.size) { "Key size mismatch" }

        storage.remove("master_key")
        check(storage.retrieve("master_key") == null) { "Expected null after deletion" }
    }
}

/**
 * Backward compatibility contract for SecureKeyStore.
 */
public typealias SecureKeyStoreContract = SecureStorageContract
