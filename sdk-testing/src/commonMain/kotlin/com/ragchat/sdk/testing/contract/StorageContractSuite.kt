package com.ragchat.sdk.testing.contract

import app.cash.sqldelight.db.SqlDriver
import com.ragchat.sdk.api.Chunk
import com.ragchat.sdk.api.Collection
import com.ragchat.sdk.api.Document
import com.ragchat.sdk.api.EmbeddingVector
import com.ragchat.sdk.storage.SqlDelightStorageRepository
import com.ragchat.sdk.storage.StorageRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reusable abstract contract test suite verifying StorageRepository compliance across platforms.
 */
public abstract class StorageContractSuite {

    /**
     * Platform-specific driver factory providing an isolated test database.
     */
    public abstract fun createDriver(): SqlDriver

    private fun createRepository(scopeId: String): StorageRepository {
        return SqlDelightStorageRepository(createDriver(), scopeId)
    }

    public fun runScopeIsolationTest(): Unit = runTest {
        val repoUserA = createRepository("user_A")
        val repoUserB = createRepository("user_B")

        val collectionA = Collection(
            id = "col_1",
            name = "Collection A",
            embeddingModelId = "gemma-2b",
            embeddingModelVersion = "1.0",
            embeddingDimensions = 384,
            createdAt = Instant.fromEpochMilliseconds(1000L)
        )

        repoUserA.putCollection(collectionA)

        // Verify User A sees it
        assertNotNull(repoUserA.getCollection("col_1"))
        assertEquals(1, repoUserA.listCollections().size)

        // Verify User B is strictly isolated and does NOT see User A's collection
        assertNull(repoUserB.getCollection("col_1"))
        assertEquals(0, repoUserB.listCollections().size)
    }

    public fun runPurgeCompletenessTest(): Unit = runTest {
        val repo = createRepository("scope_purge")

        val col = Collection(
            id = "col_purge",
            name = "Purge Collection",
            embeddingModelId = "gemma-2b",
            embeddingModelVersion = "1.0",
            embeddingDimensions = 384,
            createdAt = Instant.fromEpochMilliseconds(2000L)
        )
        repo.putCollection(col)

        val doc = Document(
            id = "doc_purge",
            collectionId = "col_purge",
            title = "Document to Purge",
            content = "Full content for verification"
        )
        repo.putDocument(doc)

        val chunk = Chunk(
            id = "chunk_purge",
            documentId = "doc_purge",
            text = "Full content for verification",
            chunkIndex = 0,
            tokenCount = 5
        )
        repo.putChunks(listOf(chunk))

        val embedding = EmbeddingVector(
            dimensions = 3,
            values = listOf(0.1f, -0.2f, 0.9f)
        )
        repo.putEmbedding("chunk_purge", embedding)

        // Verify items exist
        assertNotNull(repo.getDocument("doc_purge"))
        assertNotNull(repo.getChunk("chunk_purge"))
        assertNotNull(repo.getEmbedding("chunk_purge"))

        // Cascade purge document
        repo.purgeDocumentCascade("doc_purge")

        // Verify complete purge
        assertNull(repo.getDocument("doc_purge"))
        assertNull(repo.getChunk("chunk_purge"))
        assertNull(repo.getEmbedding("chunk_purge"))
    }

    public fun runQuantizedEmbeddingsTest(): Unit = runTest {
        val repo = createRepository("scope_quant")

        val col = Collection(
            id = "col_q",
            name = "Q Col",
            embeddingModelId = "gemma-2b",
            embeddingModelVersion = "1.0",
            embeddingDimensions = 4,
            createdAt = Instant.fromEpochMilliseconds(3000L)
        )
        repo.putCollection(col)

        val doc = Document(id = "doc_q", collectionId = "col_q", title = "Q Doc", content = "text")
        repo.putDocument(doc)

        val chunk = Chunk(id = "chunk_q", documentId = "doc_q", text = "text", chunkIndex = 0, tokenCount = 1)
        repo.putChunks(listOf(chunk))

        val originalVector = listOf(0.0f, 0.5f, -0.75f, 1.0f)
        repo.putEmbedding("chunk_q", EmbeddingVector(4, originalVector))

        val retrieved = repo.getEmbedding("chunk_q")
        assertNotNull(retrieved)
        assertEquals(4, retrieved.dimensions)

        for (i in originalVector.indices) {
            val diff = kotlin.math.abs(originalVector[i] - retrieved.values[i])
            assertTrue(diff < 0.05f, "Difference $diff too large at coordinate $i")
        }
    }

    public fun runCryptoShreddingTest(): Unit = runTest {
        val repoA = createRepository("user_shred_A")
        val repoB = createRepository("user_shred_B")

        repoA.putCollection(Collection("col_A", "A", "gemma", "1.0", 4, Instant.fromEpochMilliseconds(100L)))
        repoB.putCollection(Collection("col_B", "B", "gemma", "1.0", 4, Instant.fromEpochMilliseconds(100L)))

        // Shred user A
        repoA.clearScope()

        assertEquals(0, repoA.listCollections().size)
        // User B remains unaffected
        assertEquals(1, repoB.listCollections().size)
    }
}
