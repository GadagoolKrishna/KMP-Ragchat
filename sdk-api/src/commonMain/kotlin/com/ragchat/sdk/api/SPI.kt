package com.ragchat.sdk.api

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.native.ObjCName

/**
 * Functional capabilities supported by an [LlmProvider].
 *
 * @property contextWindowTokens Maximum token limit supported by the model's context window.
 * @property supportsStreaming Whether real-time token emission is supported.
 * @property supportsToolCalling Whether function calling or structured tool usage is supported.
 * @property isLocal Whether inference executes directly on the physical client device.
 */
@Serializable
@ObjCName("RagLlmCapabilities")
public data class LlmCapabilities(
    public val contextWindowTokens: Int,
    public val supportsStreaming: Boolean,
    public val supportsToolCalling: Boolean,
    public val isLocal: Boolean
)

/**
 * Operational availability status for an AI model runtime.
 *
 * @property isAvailable True if model weights are resident and ready to run inference.
 * @property reason Diagnostic explanation if the model is currently unavailable.
 */
@Serializable
@ObjCName("RagModelAvailability")
public data class ModelAvailability(
    public val isAvailable: Boolean,
    public val reason: String? = null
)

/**
 * Runtime execution options passed to [LlmProvider.generate].
 *
 * @property temperature Sampling temperature (0.0 = deterministic, 1.0 = creative).
 * @property maxOutputTokens Upper bound on generated tokens.
 * @property stopSequences Optional stop words terminating generation.
 */
@Serializable
@ObjCName("RagLlmOptions")
public data class LlmOptions(
    public val temperature: Float = 0.7f,
    public val maxOutputTokens: Int = 1024,
    public val stopSequences: List<String> = emptyList()
)

/**
 * Service Provider Interface for Large Language Model generation and token analysis.
 */
@ObjCName("RagLlmProvider")
public interface LlmProvider {
    /** Unique identifier of the model provider instance. */
    public val id: String

    /** Model capability declaration. */
    public val capabilities: LlmCapabilities

    /** Checks whether the runtime environment and weights are available. */
    public suspend fun checkAvailability(): ModelAvailability

    /** Generates a streaming flow of [LlmEvent]s for the provided prompt. */
    public fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent>

    /** Estimates or precisely calculates the token count of [text]. */
    public suspend fun countTokens(text: String): Int

    /** Releases internal memory, native buffers, or network connections. */
    public suspend fun close()
}

/**
 * Backward-compatibility interface alias for [LlmProvider].
 */
public typealias LlmEngine = LlmProvider

/**
 * Service Provider Interface for vector embedding generation.
 */
@ObjCName("RagEmbeddingProvider")
public interface EmbeddingProvider {
    /** Model identifier. */
    public val id: String

    /** Dimensionality of vectors produced by this model. */
    public val dimensions: Int

    /** Maximum texts accepted in a single [embedBatch] invocation. */
    public val maxBatchSize: Int

    /** Computes a dense vector embedding for a single text. */
    public suspend fun embed(text: String): EmbeddingVector

    /** Computes dense vector embeddings for a batch of texts. */
    public suspend fun embedBatch(texts: List<String>): List<EmbeddingVector>
}

/**
 * Backward-compatibility interface alias for [EmbeddingProvider].
 */
public typealias EmbeddingModel = EmbeddingProvider

/**
 * Service Provider Interface for partitioned vector similarity storage and retrieval.
 */
@ObjCName("RagVectorStore")
public interface VectorStore {
    /** Inserts or updates an individual chunk vector within a collection. */
    public suspend fun upsert(collectionId: String, chunk: Chunk, embedding: EmbeddingVector)

    /** Batch inserts or updates chunk vectors within a collection. */
    public suspend fun upsertBatch(collectionId: String, items: List<Pair<Chunk, EmbeddingVector>>)

    /** Searches for top-k most similar chunks against [queryEmbedding]. */
    public suspend fun search(collectionId: String, queryEmbedding: EmbeddingVector, topK: Int): List<SearchResult>

    /** Removes all chunks belonging to [documentId] from the collection. */
    public suspend fun delete(collectionId: String, documentId: String)

    /** Clears all vectors and indexed records in [collectionId]. */
    public suspend fun clear(collectionId: String)
}

/**
 * Service Provider Interface for document catalog and raw document persistence.
 */
@ObjCName("RagDocumentStore")
public interface DocumentStore {
    public suspend fun storeDocument(document: Document)
    public suspend fun getDocument(id: String): Document?
    public suspend fun deleteDocument(id: String)
}

/**
 * Multiplatform abstraction for reading files across Android Content URIs, java.io, and iOS security-scoped URLs.
 */
@ObjCName("RagFileAccess")
public interface FileAccess {
    /** Path, filename, or stringified resource URI. */
    public val path: String

    /** MIME type detected or supplied for the file resource. */
    public val mimeType: String?

    /** File size in bytes, or -1 if indeterminate. */
    public val sizeBytes: Long

    /** Reads the complete file into an in-memory byte array. */
    public suspend fun readBytes(): ByteArray

    /** Opens a streaming flow of byte chunks for large file ingestion. */
    public fun openByteStream(): Flow<ByteArray>
}

/**
 * Service Provider Interface for parsing structured elements from input files.
 */
@ObjCName("RagDocumentParser")
public interface DocumentParser {
    /** Supported MIME types (e.g. "application/pdf", "text/plain"). */
    public val supportedMimeTypes: Set<String>

    /** Emits a streaming flow of structured [ParsedElement] items. */
    public fun parse(file: FileAccess): Flow<ParsedElement>
}

/**
 * Service Provider Interface for splitting parsed document content into indexable [Chunk]s.
 */
@ObjCName("RagChunker")
public interface Chunker {
    public val id: String
    public suspend fun chunk(document: Document, elements: List<ParsedElement>): List<Chunk>
}

/**
 * Service Provider Interface for secondary reranking of candidate search hits.
 */
@ObjCName("RagReranker")
public interface Reranker {
    public suspend fun rerank(query: String, candidates: List<SearchResult>, topN: Int): List<SearchResult>
}

/**
 * Service Provider Interface for conversational query rewriting and context expansion.
 */
@ObjCName("RagQueryRewriter")
public interface QueryRewriter {
    public suspend fun rewrite(history: List<ChatMessage>, currentQuery: String): String
}

/**
 * Service Provider Interface for prompt assembly and template variable interpolation.
 */
@ObjCName("RagPromptTemplate")
public interface PromptTemplate {
    public val templateId: String
    public fun format(variables: Map<String, String>, contextChunks: List<Chunk>): String
}

/**
 * Service Provider Interface for dynamic authorization token retrieval without hardcoded credentials.
 */
@ObjCName("RagAuthProvider")
public interface AuthProvider {
    public suspend fun getBearerToken(): String?
    public suspend fun refreshToken(): Boolean
}

/**
 * Service Provider Interface for encryption key acquisition and rotation.
 */
@ObjCName("RagKeyProvider")
public interface KeyProvider {
    public suspend fun getEncryptionKey(alias: String): ByteArray?
    public suspend fun rotateKey(alias: String): ByteArray
}

/**
 * Service Provider Interface for hardware-backed secure key/value storage.
 */
@ObjCName("RagSecureStorage")
public interface SecureStorage {
    public suspend fun store(key: String, value: ByteArray)
    public suspend fun retrieve(key: String): ByteArray?
    public suspend fun remove(key: String)
}

/**
 * Backward compatibility alias for [SecureStorage].
 */
public typealias SecureKeyStore = SecureStorage

/**
 * Service Provider Interface for operational metrics and performance telemetry.
 */
@ObjCName("RagTelemetrySink")
public interface TelemetrySink {
    public suspend fun recordLatency(metricName: String, durationMs: Long, attributes: Map<String, String>)
    public suspend fun recordCounter(metricName: String, delta: Long, attributes: Map<String, String>)
}

/**
 * Service Provider Interface for immutable, privacy-compliant audit logging.
 */
@ObjCName("RagAuditSink")
public interface AuditSink {
    public suspend fun recordEvent(
        eventType: String,
        eventHash: String,
        prevHash: String,
        metadata: Map<String, String>
    )
}

/**
 * Decision returned by enterprise policy or safety guardrail checks.
 */
@Serializable
@ObjCName("RagPolicyDecision")
public data class PolicyDecision(
    public val isAllowed: Boolean,
    public val violatedRule: String? = null,
    public val reason: String? = null
)

/**
 * Service Provider Interface for input prompt and output completion safety evaluations.
 */
@ObjCName("RagPolicyProvider")
public interface PolicyProvider {
    public suspend fun evaluateInput(text: String, userId: String?): PolicyDecision
    public suspend fun evaluateOutput(text: String, userId: String?): PolicyDecision
}

/**
 * Service Provider Interface for managing privacy consent and legal tracking authorization.
 */
@ObjCName("RagConsentProvider")
public interface ConsentProvider {
    public suspend fun hasConsent(userId: String, consentType: String): Boolean
    public suspend fun requestConsent(userId: String, consentType: String): Boolean
}

/**
 * Result of PII redaction containing the sanitized string and token replacement counts.
 */
@Serializable
@ObjCName("RagRedactionResult")
public data class RedactionResult(
    public val redactedText: String,
    public val redactionCount: Int,
    public val detectedTypes: Set<String>
)

/**
 * Service Provider Interface for identifying and masking personally identifiable information (PII).
 */
@ObjCName("RagPiiRedactor")
public interface PiiRedactor {
    public fun redact(text: String): RedactionResult
}

/**
 * Service Provider Interface for scheduling off-thread maintenance or syncing tasks.
 */
@ObjCName("RagBackgroundScheduler")
public interface BackgroundScheduler {
    public suspend fun schedulePeriodicTask(taskId: String, intervalSeconds: Long, task: suspend () -> Unit)
    public suspend fun cancelTask(taskId: String)
}

/**
 * Service Provider Interface for discovering, verifying, and locating on-device model weights.
 */
@ObjCName("RagModelSource")
public interface ModelSource {
    public val sourceId: String
    public suspend fun getModelPath(modelId: String): String?
    public suspend fun isModelDownloaded(modelId: String): Boolean
}

/**
 * Service Provider Interface for structured logging without leaking sensitive payloads.
 */
@ObjCName("RagLogger")
public interface Logger {
    public fun debug(tag: String, message: String)
    public fun info(tag: String, message: String)
    public fun warn(tag: String, message: String)
    public fun error(tag: String, message: String, throwable: Throwable? = null)
}
