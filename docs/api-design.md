# RagChat SDK - API Design & SPI Extension Points

This document provides architectural guidance, type definitions, and idiomatic Kotlin and Swift usage samples for all extension points (SPIs) defined in `:sdk-api`.

---

## 1. Architectural Principles & Swift Interop

1. **Explicit API & KDoc**: Every public interface and domain model has full KDoc and explicit visibility modifiers (`public`).
2. **Swift Platform-Friendliness**:
   - Primitive data types avoid inline/value classes.
   - Sealed hierarchies are flat.
   - Interfaces avoid Kotlin default argument implementations; overloads or configuration objects are provided instead.
   - Objective-C name attributes (`@ObjCName("Rag...")`) ensure clean, conflict-free symbols when bridged into Swift.
3. **Reactive Flow & Swift Concurrency**:
   - Streaming APIs expose `kotlinx.coroutines.flow.Flow<T>`, bridged seamlessly into Swift `AsyncSequence` via SKIE or runtime adapters.
4. **Configuration Ergonomics**:
   - Kotlin consumers use the type-safe builder DSL: `ragChatConfig { ... }`.
   - Swift consumers use `RagChatOptions()` directly with mutable properties and call `toConfig()`.

---

## 2. SPI Reference & Usage Samples

### 1. `LlmProvider`
Handles prompt completions, token accounting, and streaming token generation.

#### Kotlin Implementation Sample
```kotlin
import com.ragchat.sdk.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CustomLlmProvider : LlmProvider {
    override val id: String = "custom-llm"
    override val capabilities: LlmCapabilities = LlmCapabilities(
        contextWindowTokens = 8192,
        supportsStreaming = true,
        supportsToolCalling = false,
        isLocal = true
    )

    override suspend fun checkAvailability(): ModelAvailability =
        ModelAvailability(isAvailable = true)

    override fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent> = flow {
        emit(LlmEvent.Token("Generated "))
        emit(LlmEvent.Token("response."))
        emit(LlmEvent.Completed(Answer(text = "Generated response.")))
    }

    override suspend fun countTokens(text: String): Int = text.length / 4

    override suspend fun close() {}
}
```

#### Swift Implementation Sample
```swift
import RagChat

final class CustomLlmProvider: RagLlmProvider {
    let id: String = "custom-swift-llm"
    let capabilities = RagLlmCapabilities(
        contextWindowTokens: 8192,
        supportsStreaming: true,
        supportsToolCalling: false,
        isLocal: true
    )

    func checkAvailability() async throws -> RagModelAvailability {
        return RagModelAvailability(isAvailable: true, reason: nil)
    }

    func generate(prompt: String, options: RagLlmOptions) -> any SkieSwiftFlow<RagLlmEvent> {
        // Return async sequence / flow bridge
        fatalError("Supplied via SKIE flow adapter")
    }

    func countTokens(text: String) async throws -> Int32 {
        return Int32(text.count / 4)
    }

    func close() async throws {}
}
```

---

### 2. `EmbeddingProvider`
Produces dense floating-point vector representations for text.

#### Kotlin Implementation Sample
```kotlin
class CustomEmbeddingProvider : EmbeddingProvider {
    override val id: String = "custom-embedder"
    override val dimensions: Int = 384
    override val maxBatchSize: Int = 32

    override suspend fun embed(text: String): EmbeddingVector =
        EmbeddingVector(dimensions = 384, values = List(384) { 0.0f })

    override suspend fun embedBatch(texts: List<String>): List<EmbeddingVector> =
        texts.map { embed(it) }
}
```

#### Swift Implementation Sample
```swift
final class CustomEmbeddingProvider: RagEmbeddingProvider {
    let id: String = "custom-swift-embedder"
    let dimensions: Int32 = 384
    let maxBatchSize: Int32 = 32

    func embed(text: String) async throws -> RagEmbeddingVector {
        let values = Array(repeating: Float(0.0), count: Int(dimensions))
        return RagEmbeddingVector(dimensions: dimensions, values: values)
    }

    func embedBatch(texts: [String]) async throws -> [RagEmbeddingVector] {
        return try await texts.asyncMap { try await self.embed(text: $0) }
    }
}
```

---

### 3. `VectorStore`
Partitioned similarity index for chunks and vectors.

#### Kotlin Implementation Sample
```kotlin
class CustomVectorStore : VectorStore {
    override suspend fun upsert(collectionId: String, chunk: Chunk, embedding: EmbeddingVector) {}
    override suspend fun upsertBatch(collectionId: String, items: List<Pair<Chunk, EmbeddingVector>>) {}
    override suspend fun search(collectionId: String, queryEmbedding: EmbeddingVector, topK: Int): List<SearchResult> = emptyList()
    override suspend fun delete(collectionId: String, documentId: String) {}
    override suspend fun clear(collectionId: String) {}
}
```

#### Swift Implementation Sample
```swift
final class CustomVectorStore: RagVectorStore {
    func upsert(collectionId: String, chunk: RagChunk, embedding: RagEmbeddingVector) async throws {}
    func upsertBatch(collectionId: String, items: [KotlinPair<RagChunk, RagEmbeddingVector>]) async throws {}
    func search(collectionId: String, queryEmbedding: RagEmbeddingVector, topK: Int32) async throws -> [RagSearchResult] {
        return []
    }
    func delete(collectionId: String, documentId: String) async throws {}
    func clear(collectionId: String) async throws {}
}
```

---

### 4. `DocumentParser`
Extracts structured elements (`ParsedElement`) from files.

#### Kotlin Implementation Sample
```kotlin
class CustomDocumentParser : DocumentParser {
    override val supportedMimeTypes: Set<String> = setOf("text/plain", "text/markdown")

    override fun parse(file: FileAccess): Flow<ParsedElement> = flow {
        emit(ParsedElement.Text(content = file.readBytes().decodeToString()))
    }
}
```

#### Swift Implementation Sample
```swift
final class CustomDocumentParser: RagDocumentParser {
    let supportedMimeTypes: Set<String> = ["text/plain", "application/pdf"]

    func parse(file: RagFileAccess) -> any SkieSwiftFlow<RagParsedElement> {
        // Stream parsed PDF / text elements
        fatalError("Bridged via SKIE")
    }
}
```

---

### 5. `Chunker`
Splits parsed documents and structured elements into indexable `Chunk` entities.

#### Kotlin Implementation Sample
```kotlin
class CustomChunker : Chunker {
    override val id: String = "simple-chunker"

    override suspend fun chunk(document: Document, elements: List<ParsedElement>): List<Chunk> {
        return listOf(Chunk(id = "${document.id}_0", documentId = document.id, text = document.content, chunkIndex = 0, tokenCount = 10))
    }
}
```

#### Swift Implementation Sample
```swift
final class CustomChunker: RagChunker {
    let id: String = "swift-chunker"

    func chunk(document: RagDocument, elements: [RagParsedElement]) async throws -> [RagChunk] {
        return [
            RagChunk(id: "\(document.id)_0", documentId: document.id, text: document.content, chunkIndex: 0, tokenCount: 10, metadata: [:])
        ]
    }
}
```

---

### 6. `Reranker`
Re-scores retrieval candidates based on query cross-attention or BM25 fusion.

#### Kotlin Implementation Sample
```kotlin
class CustomReranker : Reranker {
    override suspend fun rerank(query: String, candidates: List<SearchResult>, topN: Int): List<SearchResult> =
        candidates.sortedByDescending { it.score }.take(topN)
}
```

#### Swift Implementation Sample
```swift
final class CustomReranker: RagReranker {
    func rerank(query: String, candidates: [RagSearchResult], topN: Int32) async throws -> [RagSearchResult] {
        return Array(candidates.sorted(by: { $0.score > $1.score }).prefix(Int(topN)))
    }
}
```

---

### 7. `QueryRewriter`
Contextual query expansion and conversational disambiguation.

#### Kotlin Implementation Sample
```kotlin
class CustomQueryRewriter : QueryRewriter {
    override suspend fun rewrite(history: List<ChatMessage>, currentQuery: String): String =
        currentQuery.trim()
}
```

#### Swift Implementation Sample
```swift
final class CustomQueryRewriter: RagQueryRewriter {
    func rewrite(history: [RagChatMessage], currentQuery: String) async throws -> String {
        return currentQuery.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
```

---

### 8. `PromptTemplate`
Formats context chunks and conversational variables into LLM prompt strings.

#### Kotlin Implementation Sample
```kotlin
class CustomPromptTemplate : PromptTemplate {
    override val templateId: String = "qa-v1"

    override fun format(variables: Map<String, String>, contextChunks: List<Chunk>): String {
        val q = variables["query"] ?: ""
        val ctx = contextChunks.joinToString("\n") { it.text }
        return "Context:\n$ctx\n\nQuestion: $q"
    }
}
```

#### Swift Implementation Sample
```swift
final class CustomPromptTemplate: RagPromptTemplate {
    let templateId: String = "qa-v1"

    func format(variables: [String: String], contextChunks: [RagChunk]) -> String {
        let q = variables["query"] ?? ""
        let ctx = contextChunks.map { $0.text }.joined(separator: "\n")
        return "Context:\n\(ctx)\n\nQuestion: \(q)"
    }
}
```

---

### 9. `AuthProvider`
Provides dynamic authorization tokens without hardcoding secrets.

#### Kotlin Implementation Sample
```kotlin
class DynamicAuthProvider : AuthProvider {
    override suspend fun getBearerToken(): String? = "dynamic-token"
    override suspend fun refreshToken(): Boolean = true
}
```

#### Swift Implementation Sample
```swift
final class DynamicAuthProvider: RagAuthProvider {
    func getBearerToken() async throws -> String? {
        return "dynamic-token"
    }

    func refreshToken() async throws -> Bool {
        return true
    }
}
```

---

### 10. `KeyProvider`
Encryption key retrieval and cryptographic rotation.

#### Kotlin Implementation Sample
```kotlin
class CustomKeyProvider : KeyProvider {
    override suspend fun getEncryptionKey(alias: String): ByteArray? = ByteArray(32) { 1 }
    override suspend fun rotateKey(alias: String): ByteArray = ByteArray(32) { 2 }
}
```

#### Swift Implementation Sample
```swift
final class CustomKeyProvider: RagKeyProvider {
    func getEncryptionKey(alias: String) async throws -> KotlinByteArray? {
        return nil
    }

    func rotateKey(alias: String) async throws -> KotlinByteArray {
        return KotlinByteArray(size: 32)
    }
}
```

---

### 11. `SecureStorage`
Hardware-backed key/value storage (Android Keystore / iOS Keychain).

#### Kotlin Implementation Sample
```kotlin
class CustomSecureStorage : SecureStorage {
    private val map = mutableMapOf<String, ByteArray>()
    override suspend fun store(key: String, value: ByteArray) { map[key] = value }
    override suspend fun retrieve(key: String): ByteArray? = map[key]
    override suspend fun remove(key: String) { map.remove(key) }
}
```

#### Swift Implementation Sample
```swift
final class CustomSecureStorage: RagSecureStorage {
    func store(key: String, value: KotlinByteArray) async throws {}
    func retrieve(key: String) async throws -> KotlinByteArray? { return nil }
    func remove(key: String) async throws {}
}
```

---

### 12. `TelemetrySink`
Performance counters and execution latency recording.

#### Kotlin Implementation Sample
```kotlin
class CustomTelemetrySink : TelemetrySink {
    override suspend fun recordLatency(metricName: String, durationMs: Long, attributes: Map<String, String>) {}
    override suspend fun recordCounter(metricName: String, delta: Long, attributes: Map<String, String>) {}
}
```

#### Swift Implementation Sample
```swift
final class CustomTelemetrySink: RagTelemetrySink {
    func recordLatency(metricName: String, durationMs: Int64, attributes: [String: String]) async throws {}
    func recordCounter(metricName: String, delta: Int64, attributes: [String: String]) async throws {}
}
```

---

### 13. `AuditSink`
Cryptographic, immutable hash-chained event persistence.

#### Kotlin Implementation Sample
```kotlin
class CustomAuditSink : AuditSink {
    override suspend fun recordEvent(eventType: String, eventHash: String, prevHash: String, metadata: Map<String, String>) {}
}
```

#### Swift Implementation Sample
```swift
final class CustomAuditSink: RagAuditSink {
    func recordEvent(eventType: String, eventHash: String, prevHash: String, metadata: [String: String]) async throws {}
}
```

---

### 14. `PolicyProvider`
Input prompt and output completion safety boundaries.

#### Kotlin Implementation Sample
```kotlin
class CustomPolicyProvider : PolicyProvider {
    override suspend fun evaluateInput(text: String, userId: String?): PolicyDecision =
        PolicyDecision(isAllowed = true)

    override suspend fun evaluateOutput(text: String, userId: String?): PolicyDecision =
        PolicyDecision(isAllowed = true)
}
```

#### Swift Implementation Sample
```swift
final class CustomPolicyProvider: RagPolicyProvider {
    func evaluateInput(text: String, userId: String?) async throws -> RagPolicyDecision {
        return RagPolicyDecision(isAllowed: true, violatedRule: nil, reason: nil)
    }

    func evaluateOutput(text: String, userId: String?) async throws -> RagPolicyDecision {
        return RagPolicyDecision(isAllowed: true, violatedRule: nil, reason: nil)
    }
}
```

---

### 15. `ConsentProvider`
User consent management for telemetry and data retention.

#### Kotlin Implementation Sample
```kotlin
class CustomConsentProvider : ConsentProvider {
    override suspend fun hasConsent(userId: String, consentType: String): Boolean = true
    override suspend fun requestConsent(userId: String, consentType: String): Boolean = true
}
```

#### Swift Implementation Sample
```swift
final class CustomConsentProvider: RagConsentProvider {
    func hasConsent(userId: String, consentType: String) async throws -> Bool { return true }
    func requestConsent(userId: String, consentType: String) async throws -> Bool { return true }
}
```

---

### 16. `PiiRedactor`
Regex-based token masking for PII prior to logging or prompt dispatch.

#### Kotlin Implementation Sample
```kotlin
class CustomPiiRedactor : PiiRedactor {
    override fun redact(text: String): RedactionResult =
        RedactionResult(redactedText = text, redactionCount = 0, detectedTypes = emptySet())
}
```

#### Swift Implementation Sample
```swift
final class CustomPiiRedactor: RagPiiRedactor {
    func redact(text: String) -> RagRedactionResult {
        return RagRedactionResult(redactedText: text, redactionCount: 0, detectedTypes: [])
    }
}
```

---

### 17. `BackgroundScheduler`
Off-thread background task coordination (Android WorkManager / iOS BGTaskScheduler).

#### Kotlin Implementation Sample
```kotlin
class CustomScheduler : BackgroundScheduler {
    override suspend fun schedulePeriodicTask(taskId: String, intervalSeconds: Long, task: suspend () -> Unit) {}
    override suspend fun cancelTask(taskId: String) {}
}
```

#### Swift Implementation Sample
```swift
final class CustomScheduler: RagBackgroundScheduler {
    func schedulePeriodicTask(taskId: String, intervalSeconds: Int64, task: @escaping () async -> Void) async throws {}
    func cancelTask(taskId: String) async throws {}
}
```

---

### 18. `ModelSource`
Model weights management, validation, and local path resolution.

#### Kotlin Implementation Sample
```kotlin
class CustomModelSource : ModelSource {
    override val sourceId: String = "local-storage"
    override suspend fun getModelPath(modelId: String): String? = "/data/models/$modelId.bin"
    override suspend fun isModelDownloaded(modelId: String): Boolean = true
}
```

#### Swift Implementation Sample
```swift
final class CustomModelSource: RagModelSource {
    let sourceId: String = "swift-model-source"
    func getModelPath(modelId: String) async throws -> String? { return "/models/\(modelId)" }
    func isModelDownloaded(modelId: String) async throws -> Bool { return true }
}
```

---

### 19. `Logger`
Structured logging without sensitive payload leakage.

#### Kotlin Implementation Sample
```kotlin
class CustomLogger : Logger {
    override fun debug(tag: String, message: String) = println("DEBUG: [$tag] $message")
    override fun info(tag: String, message: String) = println("INFO: [$tag] $message")
    override fun warn(tag: String, message: String) = println("WARN: [$tag] $message")
    override fun error(tag: String, message: String, throwable: Throwable?) =
        println("ERROR: [$tag] $message (cause: ${throwable?.message})")
}
```

#### Swift Implementation Sample
```swift
final class CustomLogger: RagLogger {
    func debug(tag: String, message: String) { print("DEBUG: [\(tag)] \(message)") }
    func info(tag: String, message: String) { print("INFO: [\(tag)] \(message)") }
    func warn(tag: String, message: String) { print("WARN: [\(tag)] \(message)") }
    func error(tag: String, message: String, throwable: KotlinThrowable?) {
        print("ERROR: [\(tag)] \(message) (cause: \(String(describing: throwable?.message)))")
    }
}
```

---

## 3. Configuration & Client Creation

### Kotlin DSL
```kotlin
val client = RagChatClient.create(
    llmProvider = customLlmProvider,
    embeddingProvider = customEmbeddingProvider,
    vectorStore = customVectorStore,
    config = ragChatConfig {
        maxContextTokens = 4096
        topKRetrieval = 10
        routingMode = ModelRoutingMode.LOCAL_FIRST
        enablePiiRedaction = true
    }
)
```

### Swift Initialization
```swift
let options = RagChatConfig.companion.defaultOptions()
options.maxContextTokens = 4096
options.topKRetrieval = 10
options.routingMode = .localFirst
options.enablePiiRedaction = true

let client = RagChatClient.companion.createWithOptions(
    llmProvider: customLlmProvider,
    embeddingProvider: customEmbeddingProvider,
    vectorStore: customVectorStore,
    options: options
)
```
