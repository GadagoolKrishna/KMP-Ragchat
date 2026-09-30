package com.ragchat.sdk.core

import com.ragchat.sdk.api.*
import com.ragchat.sdk.governance.PiiRedactor
import com.ragchat.sdk.governance.PolicyEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Central orchestrator connecting retrieval, governance, and LLM runtime.
 */
public class RagOrchestrator(
    private val llmProvider: LlmProvider,
    private val embeddingProvider: EmbeddingProvider,
    private val vectorStore: VectorStore,
    private val piiRedactor: PiiRedactor = PiiRedactor(),
    private val policyEngine: PolicyEngine = PolicyEngine(),
    private val config: RagChatConfig = RagChatConfig()
) {
    public suspend fun chat(userMessage: String): ChatMessage {
        val sanitized = if (config.enablePiiRedaction) piiRedactor.redact(userMessage) else userMessage
        require(policyEngine.validatePrompt(sanitized)) { "Prompt rejected by policy engine." }

        // Retrieve relevant context from default collection
        val queryEmbedding = embeddingProvider.embed(sanitized)
        val searchResults = vectorStore.search("default", queryEmbedding, config.topKRetrieval)

        // Build prompt with context
        val contextPrompt = buildString {
            if (searchResults.isNotEmpty()) {
                appendLine("Reference Context:")
                searchResults.forEach { appendLine("- ${it.chunk.text}") }
                appendLine()
            }
            appendLine("User Question: $sanitized")
        }

        var responseText = ""
        llmProvider.generate(contextPrompt, LlmOptions()).collect { event ->
            when (event) {
                is LlmEvent.Token -> responseText += event.text
                is LlmEvent.Completed -> responseText = event.answer.text
                else -> {}
            }
        }
        val finalResponse = if (config.enablePiiRedaction) piiRedactor.redact(responseText) else responseText

        val citations = searchResults.map {
            Citation(
                chunkId = it.chunk.id,
                documentId = it.chunk.documentId,
                title = it.chunk.metadata["title"] ?: "Source",
                snippet = it.chunk.text.take(120),
                score = it.score
            )
        }

        return ChatMessage(
            id = "msg_${Clock.System.now().toEpochMilliseconds()}",
            role = ChatRole.ASSISTANT,
            content = finalResponse,
            timestamp = Clock.System.now(),
            citations = citations
        )
    }

    public fun chatStream(userMessage: String): Flow<String> = flow {
        val sanitized = if (config.enablePiiRedaction) piiRedactor.redact(userMessage) else userMessage
        llmProvider.generate(sanitized, LlmOptions()).collect { event ->
            if (event is LlmEvent.Token) {
                emit(event.text)
            }
        }
    }
}
