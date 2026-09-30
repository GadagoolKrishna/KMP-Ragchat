package com.ragchat.sdk.storage

import com.ragchat.sdk.api.Chunk
import com.ragchat.sdk.api.Collection
import com.ragchat.sdk.api.Document
import com.ragchat.sdk.api.EmbeddingVector
import kotlinx.datetime.Instant

/**
 * Audit log entry stored immutably in SQLite.
 */
public data class StoredAuditEntry(
    public val scopeId: String,
    public val eventId: String,
    public val eventType: String,
    public val eventHash: String,
    public val prevHash: String?,
    public val timestamp: Instant,
    public val metadataJson: String
)

/**
 * Ingestion job status model.
 */
public data class StoredIngestionJob(
    public val scopeId: String,
    public val id: String,
    public val documentId: String,
    public val status: String,
    public val progress: Float,
    public val errorMessage: String?,
    public val updatedAt: Instant
)

/**
 * Storage repository interface offering scope-isolated CRUD and transaction execution.
 */
public interface StorageRepository {
    public val scopeId: String

    /**
     * Executes the given [block] within an isolated SQLite database transaction.
     */
    public suspend fun <R> transaction(block: suspend () -> R): R

    // Collections
    public suspend fun putCollection(collection: Collection)
    public suspend fun getCollection(id: String): Collection?
    public suspend fun listCollections(): List<Collection>
    public suspend fun deleteCollection(id: String)

    // Documents
    public suspend fun putDocument(document: Document)
    public suspend fun getDocument(id: String): Document?
    public suspend fun listDocuments(collectionId: String): List<Document>
    public suspend fun deleteDocument(id: String)

    // Chunks & FTS
    public suspend fun putChunks(chunks: List<Chunk>)
    public suspend fun getChunk(id: String): Chunk?
    public suspend fun listChunks(documentId: String): List<Chunk>
    public suspend fun searchLexical(query: String): List<String>

    // Embeddings
    public suspend fun putEmbedding(chunkId: String, vector: EmbeddingVector)
    public suspend fun getEmbedding(chunkId: String): EmbeddingVector?
    public suspend fun listEmbeddingsForCollection(collectionId: String): Map<String, EmbeddingVector>

    // Ingestion jobs
    public suspend fun putJob(job: StoredIngestionJob)
    public suspend fun getJob(id: String): StoredIngestionJob?
    public suspend fun listJobs(): List<StoredIngestionJob>

    // Audit log
    public suspend fun appendAuditLog(entry: StoredAuditEntry)
    public suspend fun listAuditLogs(): List<StoredAuditEntry>
    public suspend fun getLatestAuditLog(): StoredAuditEntry?

    // Purge / Crypto-shredding
    public suspend fun purgeDocumentCascade(id: String)
    public suspend fun clearScope()
    public suspend fun clearAll()
}
