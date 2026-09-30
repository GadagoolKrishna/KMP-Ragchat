package com.ragchat.sdk.api

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.native.ObjCName

/**
 * Role of the participant in a conversational chat session.
 */
@Serializable
@ObjCName("RagChatRole")
public enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

/**
 * Backward compatibility alias for [ChatRole].
 */
public typealias RagRole = ChatRole

/**
 * Domain representation of a chat message.
 *
 * @property id Unique identifier for the message.
 * @property role The participant role sending this message.
 * @property content The textual content of the message.
 * @property timestamp Monotonic or UTC instant when the message was recorded.
 * @property citations References to source knowledge chunks supporting this message.
 * @property metadata Arbitrary key-value metadata tags.
 */
@Serializable
@ObjCName("RagChatMessage")
public data class ChatMessage(
    public val id: String,
    public val role: ChatRole,
    public val content: String,
    public val timestamp: Instant,
    public val citations: List<Citation> = emptyList(),
    public val metadata: Map<String, String> = emptyMap()
)

/**
 * Backward compatibility alias for [ChatMessage].
 */
public typealias RagMessage = ChatMessage

/**
 * Logical document representation for ingestion, chunking, and indexing.
 *
 * @property id Unique identifier for the document.
 * @property collectionId The collection or namespace this document belongs to.
 * @property title Human-readable document title.
 * @property content Raw text or Markdown content.
 * @property sourceUri Optional original URI or file path of the source document.
 * @property metadata Arbitrary document-level attributes.
 * @property createdAt Timestamp when the document was ingested or created.
 */
@Serializable
@ObjCName("RagDocument")
public data class Document(
    public val id: String,
    public val collectionId: String = "default",
    public val title: String,
    public val content: String,
    public val sourceUri: String? = null,
    public val metadata: Map<String, String> = emptyMap(),
    public val createdAt: Instant? = null
)

/**
 * Backward compatibility alias for [Document].
 */
public typealias RagDocument = Document

/**
 * Textual segment of a [Document] produced by a [Chunker] pipeline.
 *
 * @property id Unique identifier for this chunk.
 * @property documentId Identifier of the parent [Document].
 * @property text Raw textual segment.
 * @property chunkIndex Zero-based sequence index within the document.
 * @property tokenCount Estimated or measured token length of the chunk.
 * @property metadata Chunk-level attributes (e.g. section headings, page numbers).
 */
@Serializable
@ObjCName("RagChunk")
public data class Chunk(
    public val id: String,
    public val documentId: String,
    public val text: String,
    public val chunkIndex: Int,
    public val tokenCount: Int,
    public val metadata: Map<String, String> = emptyMap()
)

/**
 * Managed index partition or collection anchoring vectors and documents.
 *
 * @property id Unique identifier for the collection.
 * @property name Human-readable name.
 * @property embeddingModelId Model identifier used to produce vectors in this collection.
 * @property embeddingModelVersion Model version string ensuring vector compatibility.
 * @property embeddingDimensions Dimensionality of the vectors in this collection.
 * @property createdAt Creation timestamp.
 * @property metadata Arbitrary collection-level settings.
 */
@Serializable
@ObjCName("RagCollection")
public data class Collection(
    public val id: String,
    public val name: String,
    public val embeddingModelId: String,
    public val embeddingModelVersion: String,
    public val embeddingDimensions: Int,
    public val createdAt: Instant,
    public val metadata: Map<String, String> = emptyMap()
)

/**
 * Dense vector embedding representation.
 *
 * @property dimensions Count of float dimensions.
 * @property values List of floating point coordinates.
 */
@Serializable
@ObjCName("RagEmbeddingVector")
public data class EmbeddingVector(
    public val dimensions: Int,
    public val values: List<Float>
)

/**
 * Reference citation linking generated assistant answers back to source chunks.
 *
 * @property chunkId Identifier of the referenced [Chunk].
 * @property documentId Identifier of the parent [Document].
 * @property title Title of the referenced document.
 * @property snippet Text snippet excerpted from the chunk.
 * @property score Similarity or relevance score assigned during retrieval/reranking.
 * @property sourceUri Optional URI pointing to the source file or webpage.
 */
@Serializable
@ObjCName("RagCitation")
public data class Citation(
    public val chunkId: String,
    public val documentId: String,
    public val title: String,
    public val snippet: String,
    public val score: Float,
    public val sourceUri: String? = null
)

/**
 * Search hit returned by a vector store or retrieval engine.
 *
 * @property chunk Retrieved document chunk.
 * @property score Vector similarity score (typically cosine similarity between -1.0 and 1.0).
 */
@Serializable
@ObjCName("RagSearchResult")
public data class SearchResult(
    public val chunk: Chunk,
    public val score: Float
)

/**
 * Reason why an LLM finished generating a completion.
 */
@Serializable
@ObjCName("RagFinishReason")
public enum class FinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    TOOL_CALL,
    ERROR
}

/**
 * Token usage accounting for a generation request.
 *
 * @property promptTokens Tokens consumed by input prompt and context.
 * @property completionTokens Tokens emitted by the model completion.
 * @property totalTokens Sum of prompt and completion tokens.
 */
@Serializable
@ObjCName("RagTokenUsage")
public data class TokenUsage(
    public val promptTokens: Int,
    public val completionTokens: Int,
    public val totalTokens: Int = promptTokens + completionTokens
)

/**
 * Final synthesized answer returned by the RAG orchestrator or LLM provider.
 *
 * @property text Completed assistant response string.
 * @property citations Verified citations supporting the answer.
 * @property finishReason Reason why generation halted.
 * @property usage Token accounting information.
 * @property metadata Execution metadata (e.g. model name, latency, routing decision).
 */
@Serializable
@ObjCName("RagAnswer")
public data class Answer(
    public val text: String,
    public val citations: List<Citation> = emptyList(),
    public val finishReason: FinishReason = FinishReason.STOP,
    public val usage: TokenUsage = TokenUsage(0, 0),
    public val metadata: Map<String, String> = emptyMap()
)

/**
 * Strategy dictating how requests are routed between on-device models and cloud endpoints.
 */
@Serializable
@ObjCName("RagModelRoutingMode")
public enum class ModelRoutingMode {
    /** Run inference exclusively on-device; fail if local model is unavailable. */
    LOCAL_ONLY,
    /** Run inference exclusively via remote cloud APIs. */
    CLOUD_ONLY,
    /** Attempt on-device inference first; fallback to cloud on failure or unavailability. */
    LOCAL_FIRST,
    /** Attempt cloud inference first; fallback to on-device if offline or request fails. */
    CLOUD_FIRST,
    /** Automatically select based on battery, connectivity, prompt length, and device thermal state. */
    AUTO
}

/**
 * Discrete structured element extracted by a [DocumentParser].
 */
@Serializable
@ObjCName("RagParsedElement")
public sealed interface ParsedElement {
    /** Free-form or paragraph text block. */
    @Serializable
    @ObjCName("RagParsedText")
    public data class Text(
        public val content: String,
        public val metadata: Map<String, String> = emptyMap()
    ) : ParsedElement

    /** Document heading or section title. */
    @Serializable
    @ObjCName("RagParsedHeading")
    public data class Heading(
        public val text: String,
        public val level: Int,
        public val metadata: Map<String, String> = emptyMap()
    ) : ParsedElement

    /** Tabular data formatted into rows and cells. */
    @Serializable
    @ObjCName("RagParsedTable")
    public data class Table(
        public val headers: List<String>,
        public val rows: List<List<String>>,
        public val caption: String? = null
    ) : ParsedElement

    /** Reference to an embedded image or figure. */
    @Serializable
    @ObjCName("RagParsedImageRef")
    public data class ImageRef(
        public val uri: String,
        public val altText: String? = null,
        public val metadata: Map<String, String> = emptyMap()
    ) : ParsedElement

    /** Document-level metadata attributes. */
    @Serializable
    @ObjCName("RagParsedMetadata")
    public data class Metadata(
        public val attributes: Map<String, String>
    ) : ParsedElement
}

/**
 * Real-time event emitted during streaming LLM inference.
 */
@Serializable
@ObjCName("RagLlmEvent")
public sealed interface LlmEvent {
    /** Incremental text token generated by the model. */
    @Serializable
    @ObjCName("RagLlmEventToken")
    public data class Token(public val text: String) : LlmEvent

    /** Token consumption metrics for the in-flight inference. */
    @Serializable
    @ObjCName("RagLlmEventUsage")
    public data class Usage(public val usage: TokenUsage) : LlmEvent

    /** Final completion event containing the full [Answer]. */
    @Serializable
    @ObjCName("RagLlmEventCompleted")
    public data class Completed(public val answer: Answer) : LlmEvent

    /** Error event indicating generation failure. */
    @Serializable
    @ObjCName("RagLlmEventError")
    public data class Error(public val message: String, public val code: String? = null) : LlmEvent
}
