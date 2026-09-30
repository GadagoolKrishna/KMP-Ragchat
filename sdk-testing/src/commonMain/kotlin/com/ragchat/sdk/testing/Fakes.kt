package com.ragchat.sdk.testing

import com.ragchat.sdk.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

/**
 * In-memory test double for [LlmProvider].
 */
public class FakeLlmProvider(
    override val id: String = "fake-llm-provider",
    override val capabilities: LlmCapabilities = LlmCapabilities(
        contextWindowTokens = 4096,
        supportsStreaming = true,
        supportsToolCalling = false,
        isLocal = true
    ),
    public var isAvailable: Boolean = true,
    public var defaultAnswer: String = "Test generation response"
) : LlmProvider {

    override suspend fun checkAvailability(): ModelAvailability {
        return ModelAvailability(isAvailable = isAvailable, reason = if (isAvailable) null else "Disabled in test")
    }

    override fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent> = flow {
        emit(LlmEvent.Token(defaultAnswer))
        val usage = TokenUsage(promptTokens = prompt.length / 4, completionTokens = defaultAnswer.length / 4)
        emit(LlmEvent.Usage(usage))
        emit(LlmEvent.Completed(Answer(text = defaultAnswer, usage = usage)))
    }

    override suspend fun countTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    override suspend fun close() {
        // No-op for test double
    }
}

/**
 * Backward compatibility fake for [LlmEngine].
 */
public typealias FakeLlmEngine = FakeLlmProvider

/**
 * In-memory test double for [EmbeddingProvider].
 */
public class FakeEmbeddingProvider(
    override val id: String = "fake-embedding-provider",
    override val dimensions: Int = 4,
    override val maxBatchSize: Int = 32
) : EmbeddingProvider {

    override suspend fun embed(text: String): EmbeddingVector {
        return EmbeddingVector(dimensions = dimensions, values = List(dimensions) { 0.1f * (it + 1) })
    }

    override suspend fun embedBatch(texts: List<String>): List<EmbeddingVector> {
        return texts.map { embed(it) }
    }
}

/**
 * Backward compatibility fake for [EmbeddingModel].
 */
public typealias FakeEmbeddingModel = FakeEmbeddingProvider

/**
 * In-memory test double for [VectorStore].
 */
public class InMemoryVectorStore : VectorStore {
    private val entries = mutableMapOf<String, MutableList<Pair<Chunk, EmbeddingVector>>>()

    override suspend fun upsert(collectionId: String, chunk: Chunk, embedding: EmbeddingVector) {
        val list = entries.getOrPut(collectionId) { mutableListOf() }
        list.removeAll { it.first.id == chunk.id }
        list.add(chunk to embedding)
    }

    override suspend fun upsertBatch(collectionId: String, items: List<Pair<Chunk, EmbeddingVector>>) {
        items.forEach { (chunk, vec) -> upsert(collectionId, chunk, vec) }
    }

    override suspend fun search(collectionId: String, queryEmbedding: EmbeddingVector, topK: Int): List<SearchResult> {
        val list = entries[collectionId] ?: emptyList()
        return list.take(topK).map { SearchResult(chunk = it.first, score = 0.95f) }
    }

    override suspend fun delete(collectionId: String, documentId: String) {
        entries[collectionId]?.removeAll { it.first.documentId == documentId }
    }

    override suspend fun clear(collectionId: String) {
        entries[collectionId]?.clear()
    }
}

/**
 * In-memory test double for [DocumentStore].
 */
public class InMemoryDocumentStore : DocumentStore {
    private val docs = mutableMapOf<String, Document>()

    override suspend fun storeDocument(document: Document) {
        docs[document.id] = document
    }

    override suspend fun getDocument(id: String): Document? = docs[id]

    override suspend fun deleteDocument(id: String) {
        docs.remove(id)
    }
}

/**
 * In-memory test double for [FileAccess].
 */
public class FakeFileAccess(
    override val path: String = "memory://test.txt",
    override val mimeType: String? = "text/plain",
    public val data: ByteArray = "Sample file content".encodeToByteArray()
) : FileAccess {
    override val sizeBytes: Long = data.size.toLong()

    override suspend fun readBytes(): ByteArray = data

    override fun openByteStream(): Flow<ByteArray> = flowOf(data)
}

/**
 * In-memory test double for [DocumentParser].
 */
public class FakeDocumentParser(
    override val supportedMimeTypes: Set<String> = setOf("text/plain", "application/pdf")
) : DocumentParser {
    override fun parse(file: FileAccess): Flow<ParsedElement> = flow {
        emit(ParsedElement.Heading(text = "Document Title", level = 1))
        emit(ParsedElement.Text(content = file.readBytes().decodeToString()))
    }
}

/**
 * In-memory test double for [Chunker].
 */
public class FakeChunker(override val id: String = "fake-chunker") : Chunker {
    override suspend fun chunk(document: Document, elements: List<ParsedElement>): List<Chunk> {
        return elements.filterIsInstance<ParsedElement.Text>().mapIndexed { index, textEl ->
            Chunk(
                id = "${document.id}_chunk_$index",
                documentId = document.id,
                text = textEl.content,
                chunkIndex = index,
                tokenCount = (textEl.content.length / 4).coerceAtLeast(1)
            )
        }
    }
}

/**
 * In-memory test double for [Reranker].
 */
public class FakeReranker : Reranker {
    override suspend fun rerank(query: String, candidates: List<SearchResult>, topN: Int): List<SearchResult> {
        return candidates.take(topN)
    }
}

/**
 * In-memory test double for [QueryRewriter].
 */
public class FakeQueryRewriter : QueryRewriter {
    override suspend fun rewrite(history: List<ChatMessage>, currentQuery: String): String {
        return currentQuery.trim()
    }
}

/**
 * In-memory test double for [PromptTemplate].
 */
public class FakePromptTemplate(override val templateId: String = "default-qa") : PromptTemplate {
    override fun format(variables: Map<String, String>, contextChunks: List<Chunk>): String {
        val query = variables["query"] ?: ""
        val context = contextChunks.joinToString("\n") { it.text }
        return "Context:\n$context\n\nQuestion: $query"
    }
}

/**
 * In-memory test double for [AuthProvider].
 */
public class FakeAuthProvider(public var token: String? = "mock-bearer-token") : AuthProvider {
    override suspend fun getBearerToken(): String? = token
    override suspend fun refreshToken(): Boolean = true
}

/**
 * In-memory test double for [KeyProvider].
 */
public class FakeKeyProvider : KeyProvider {
    private val keys = mutableMapOf<String, ByteArray>()

    override suspend fun getEncryptionKey(alias: String): ByteArray {
        return keys.getOrPut(alias) { ByteArray(32) { 1 } }
    }

    override suspend fun rotateKey(alias: String): ByteArray {
        val newKey = ByteArray(32) { 2 }
        keys[alias] = newKey
        return newKey
    }
}

/**
 * In-memory test double for [SecureStorage].
 */
public class FakeSecureStorage : SecureStorage {
    private val store = mutableMapOf<String, ByteArray>()

    override suspend fun store(key: String, value: ByteArray) {
        store[key] = value
    }

    override suspend fun retrieve(key: String): ByteArray? = store[key]

    override suspend fun remove(key: String) {
        store.remove(key)
    }
}

/**
 * Backward compatibility fake for [SecureKeyStore].
 */
public typealias FakeSecureKeyStore = FakeSecureStorage

/**
 * In-memory test double for [TelemetrySink].
 */
public class FakeTelemetrySink : TelemetrySink {
    public val latencyRecords: MutableList<Pair<String, Long>> = mutableListOf()
    public val counterRecords: MutableList<Pair<String, Long>> = mutableListOf()

    override suspend fun recordLatency(metricName: String, durationMs: Long, attributes: Map<String, String>) {
        latencyRecords.add(metricName to durationMs)
    }

    override suspend fun recordCounter(metricName: String, delta: Long, attributes: Map<String, String>) {
        counterRecords.add(metricName to delta)
    }
}

/**
 * In-memory test double for [AuditSink].
 */
public class FakeAuditSink : AuditSink {
    public val events: MutableList<Pair<String, String>> = mutableListOf()

    override suspend fun recordEvent(
        eventType: String,
        eventHash: String,
        prevHash: String,
        metadata: Map<String, String>
    ) {
        events.add(eventType to eventHash)
    }
}

/**
 * In-memory test double for [PolicyProvider].
 */
public class FakePolicyProvider(public var allowAll: Boolean = true) : PolicyProvider {
    override suspend fun evaluateInput(text: String, userId: String?): PolicyDecision {
        return if (allowAll) PolicyDecision(isAllowed = true) else PolicyDecision(isAllowed = false, violatedRule = "TEST_BLOCK", reason = "Blocked by fake policy")
    }

    override suspend fun evaluateOutput(text: String, userId: String?): PolicyDecision {
        return if (allowAll) PolicyDecision(isAllowed = true) else PolicyDecision(isAllowed = false, violatedRule = "TEST_BLOCK", reason = "Blocked by fake policy")
    }
}

/**
 * In-memory test double for [ConsentProvider].
 */
public class FakeConsentProvider(public var consentGranted: Boolean = true) : ConsentProvider {
    override suspend fun hasConsent(userId: String, consentType: String): Boolean = consentGranted
    override suspend fun requestConsent(userId: String, consentType: String): Boolean = consentGranted
}

/**
 * In-memory test double for [PiiRedactor].
 */
public class FakePiiRedactor : PiiRedactor {
    override fun redact(text: String): RedactionResult {
        return RedactionResult(redactedText = text, redactionCount = 0, detectedTypes = emptySet())
    }
}

/**
 * In-memory test double for [BackgroundScheduler].
 */
public class FakeBackgroundScheduler : BackgroundScheduler {
    public val scheduledTasks: MutableMap<String, suspend () -> Unit> = mutableMapOf()

    override suspend fun schedulePeriodicTask(taskId: String, intervalSeconds: Long, task: suspend () -> Unit) {
        scheduledTasks[taskId] = task
    }

    override suspend fun cancelTask(taskId: String) {
        scheduledTasks.remove(taskId)
    }
}

/**
 * In-memory test double for [ModelSource].
 */
public class FakeModelSource(override val sourceId: String = "fake-model-source") : ModelSource {
    override suspend fun getModelPath(modelId: String): String? = "/local/models/$modelId.bin"
    override suspend fun isModelDownloaded(modelId: String): Boolean = true
}

/**
 * In-memory test double for [Logger].
 */
public class FakeLogger : Logger {
    public val logs: MutableList<String> = mutableListOf()

    override fun debug(tag: String, message: String) {
        logs.add("DEBUG [$tag] $message")
    }

    override fun info(tag: String, message: String) {
        logs.add("INFO [$tag] $message")
    }

    override fun warn(tag: String, message: String) {
        logs.add("WARN [$tag] $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        logs.add("ERROR [$tag] $message (cause=${throwable?.message})")
    }
}
