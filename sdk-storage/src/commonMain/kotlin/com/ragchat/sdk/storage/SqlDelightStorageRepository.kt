package com.ragchat.sdk.storage

import app.cash.sqldelight.db.SqlDriver
import com.ragchat.sdk.api.Chunk
import com.ragchat.sdk.api.Collection
import com.ragchat.sdk.api.Document
import com.ragchat.sdk.api.EmbeddingVector
import com.ragchat.sdk.storage.db.RagChatDatabase
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Production implementation of [StorageRepository] backed by SQLDelight and SQLite.
 */
public class SqlDelightStorageRepository(
    private val database: RagChatDatabase,
    override val scopeId: String
) : StorageRepository {

    public constructor(driver: SqlDriver, scopeId: String) : this(
        database = RagChatDatabase(driver),
        scopeId = scopeId
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val queries = database.ragChatQueries

    override suspend fun <R> transaction(block: suspend () -> R): R {
        return database.transactionWithResult {
            // Transaction boundary in SQLDelight
            kotlinx.coroutines.runBlocking {
                block()
            }
        }
    }

    override suspend fun putCollection(collection: Collection) {
        val metadataStr = json.encodeToString(collection.metadata)
        queries.insertCollection(
            scope_id = scopeId,
            id = collection.id,
            name = collection.name,
            embedding_model_id = collection.embeddingModelId,
            embedding_model_version = collection.embeddingModelVersion,
            embedding_dimensions = collection.embeddingDimensions.toLong(),
            created_at = collection.createdAt.toEpochMilliseconds(),
            metadata_json = metadataStr
        )
    }

    override suspend fun getCollection(id: String): Collection? {
        val row = queries.getCollection(scope_id = scopeId, id = id).executeAsOneOrNull() ?: return null
        val metadata: Map<String, String> = runCatching {
            json.decodeFromString<Map<String, String>>(row.metadata_json)
        }.getOrDefault(emptyMap())
        return Collection(
            id = row.id,
            name = row.name,
            embeddingModelId = row.embedding_model_id,
            embeddingModelVersion = row.embedding_model_version,
            embeddingDimensions = row.embedding_dimensions.toInt(),
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            metadata = metadata
        )
    }

    override suspend fun listCollections(): List<Collection> {
        return queries.listCollections(scopeId).executeAsList().map { row ->
            val metadata: Map<String, String> = runCatching {
                json.decodeFromString<Map<String, String>>(row.metadata_json)
            }.getOrDefault(emptyMap())
            Collection(
                id = row.id,
                name = row.name,
                embeddingModelId = row.embedding_model_id,
                embeddingModelVersion = row.embedding_model_version,
                embeddingDimensions = row.embedding_dimensions.toInt(),
                createdAt = Instant.fromEpochMilliseconds(row.created_at),
                metadata = metadata
            )
        }
    }

    override suspend fun deleteCollection(id: String) {
        queries.deleteCollection(scopeId, id)
    }

    override suspend fun putDocument(document: Document) {
        val metadataStr = json.encodeToString(document.metadata)
        val createdAtMs = document.createdAt?.toEpochMilliseconds() ?: 0L
        queries.insertDocument(
            scope_id = scopeId,
            id = document.id,
            collection_id = document.collectionId,
            title = document.title,
            content = document.content,
            source_uri = document.sourceUri,
            metadata_json = metadataStr,
            created_at = createdAtMs
        )
    }

    override suspend fun getDocument(id: String): Document? {
        val row = queries.getDocument(scope_id = scopeId, id = id).executeAsOneOrNull() ?: return null
        val metadata: Map<String, String> = runCatching {
            json.decodeFromString<Map<String, String>>(row.metadata_json)
        }.getOrDefault(emptyMap())
        return Document(
            id = row.id,
            collectionId = row.collection_id,
            title = row.title,
            content = row.content,
            sourceUri = row.source_uri,
            metadata = metadata,
            createdAt = if (row.created_at > 0) Instant.fromEpochMilliseconds(row.created_at) else null
        )
    }

    override suspend fun listDocuments(collectionId: String): List<Document> {
        return queries.listDocuments(scopeId, collectionId).executeAsList().map { row ->
            val metadata: Map<String, String> = runCatching {
                json.decodeFromString<Map<String, String>>(row.metadata_json)
            }.getOrDefault(emptyMap())
            Document(
                id = row.id,
                collectionId = row.collection_id,
                title = row.title,
                content = row.content,
                sourceUri = row.source_uri,
                metadata = metadata,
                createdAt = if (row.created_at > 0) Instant.fromEpochMilliseconds(row.created_at) else null
            )
        }
    }

    override suspend fun deleteDocument(id: String) {
        purgeDocumentCascade(id)
    }

    override suspend fun putChunks(chunks: List<Chunk>) {
        database.transaction {
            for (chunk in chunks) {
                val metadataStr = json.encodeToString(chunk.metadata)
                queries.insertChunk(
                    scope_id = scopeId,
                    id = chunk.id,
                    document_id = chunk.documentId,
                    chunk_index = chunk.chunkIndex.toLong(),
                    content = chunk.text,
                    token_count = chunk.tokenCount.toLong(),
                    metadata_json = metadataStr
                )
                queries.insertChunkFts(
                    chunk_id = chunk.id,
                    scope_id = scopeId,
                    content = chunk.text
                )
            }
        }
    }

    override suspend fun getChunk(id: String): Chunk? {
        val row = queries.getChunk(scopeId, id).executeAsOneOrNull() ?: return null
        val metadata: Map<String, String> = runCatching {
            json.decodeFromString<Map<String, String>>(row.metadata_json)
        }.getOrDefault(emptyMap())
        return Chunk(
            id = row.id,
            documentId = row.document_id,
            text = row.content,
            chunkIndex = row.chunk_index.toInt(),
            tokenCount = row.token_count.toInt(),
            metadata = metadata
        )
    }

    override suspend fun listChunks(documentId: String): List<Chunk> {
        return queries.listChunksByDocument(scopeId, documentId).executeAsList().map { row ->
            val metadata: Map<String, String> = runCatching {
                json.decodeFromString<Map<String, String>>(row.metadata_json)
            }.getOrDefault(emptyMap())
            Chunk(
                id = row.id,
                documentId = row.document_id,
                text = row.content,
                chunkIndex = row.chunk_index.toInt(),
                tokenCount = row.token_count.toInt(),
                metadata = metadata
            )
        }
    }

    override suspend fun searchLexical(query: String): List<String> {
        return queries.searchFtsLike(scopeId, query).executeAsList()
    }

    override suspend fun putEmbedding(chunkId: String, vector: EmbeddingVector) {
        val (scale, quantizedBytes) = Quantization.quantize(vector.values)
        queries.insertEmbedding(
            scope_id = scopeId,
            chunk_id = chunkId,
            dimensions = vector.dimensions.toLong(),
            scale = scale.toDouble(),
            quantized_data = quantizedBytes
        )
    }

    override suspend fun getEmbedding(chunkId: String): EmbeddingVector? {
        val row = queries.getEmbedding(scopeId, chunkId).executeAsOneOrNull() ?: return null
        val values = Quantization.dequantize(row.quantized_data, row.scale.toFloat())
        return EmbeddingVector(dimensions = row.dimensions.toInt(), values = values)
    }

    override suspend fun listEmbeddingsForCollection(collectionId: String): Map<String, EmbeddingVector> {
        val rows = queries.listEmbeddingsForCollection(scopeId, collectionId).executeAsList()
        val result = mutableMapOf<String, EmbeddingVector>()
        for (row in rows) {
            val values = Quantization.dequantize(row.quantized_data, row.scale.toFloat())
            result[row.chunk_id] = EmbeddingVector(dimensions = row.dimensions.toInt(), values = values)
        }
        return result
    }

    override suspend fun putJob(job: StoredIngestionJob) {
        queries.insertJob(
            scope_id = scopeId,
            id = job.id,
            document_id = job.documentId,
            status = job.status,
            progress = job.progress.toDouble(),
            error_message = job.errorMessage,
            updated_at = job.updatedAt.toEpochMilliseconds()
        )
    }

    override suspend fun getJob(id: String): StoredIngestionJob? {
        val row = queries.getJob(scopeId, id).executeAsOneOrNull() ?: return null
        return StoredIngestionJob(
            scopeId = row.scope_id,
            id = row.id,
            documentId = row.document_id,
            status = row.status,
            progress = row.progress.toFloat(),
            errorMessage = row.error_message,
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
        )
    }

    override suspend fun listJobs(): List<StoredIngestionJob> {
        return queries.listJobs(scopeId).executeAsList().map { row ->
            StoredIngestionJob(
                scopeId = row.scope_id,
                id = row.id,
                documentId = row.document_id,
                status = row.status,
                progress = row.progress.toFloat(),
                errorMessage = row.error_message,
                updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
            )
        }
    }

    override suspend fun appendAuditLog(entry: StoredAuditEntry) {
        queries.insertAuditLog(
            scope_id = scopeId,
            event_id = entry.eventId,
            event_type = entry.eventType,
            event_hash = entry.eventHash,
            prev_hash = entry.prevHash,
            timestamp = entry.timestamp.toEpochMilliseconds(),
            metadata_json = entry.metadataJson
        )
    }

    override suspend fun listAuditLogs(): List<StoredAuditEntry> {
        return queries.listAuditLog(scopeId).executeAsList().map { row ->
            StoredAuditEntry(
                scopeId = row.scope_id,
                eventId = row.event_id,
                eventType = row.event_type,
                eventHash = row.event_hash,
                prevHash = row.prev_hash,
                timestamp = Instant.fromEpochMilliseconds(row.timestamp),
                metadataJson = row.metadata_json
            )
        }
    }

    override suspend fun getLatestAuditLog(): StoredAuditEntry? {
        val row = queries.getLatestAuditLog(scopeId).executeAsOneOrNull() ?: return null
        return StoredAuditEntry(
            scopeId = row.scope_id,
            eventId = row.event_id,
            eventType = row.event_type,
            eventHash = row.event_hash,
            prevHash = row.prev_hash,
            timestamp = Instant.fromEpochMilliseconds(row.timestamp),
            metadataJson = row.metadata_json
        )
    }

    override suspend fun purgeDocumentCascade(id: String) {
        database.transaction {
            queries.deleteDocumentChunksFts(scope_id = scopeId, scope_id_ = scopeId, document_id = id)
            queries.purgeDocumentCascade(scopeId, id)
        }
    }

    override suspend fun clearScope() {
        database.transaction {
            queries.clearScopeAll(scopeId)
            queries.clearAuditScope(scopeId)
            queries.clearJobsScope(scopeId)
        }
    }

    override suspend fun clearAll() {
        database.transaction {
            queries.clearEntireDatabase()
        }
    }
}
