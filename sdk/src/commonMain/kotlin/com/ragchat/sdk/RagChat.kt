package com.ragchat.sdk

import com.ragchat.sdk.api.*
import com.ragchat.sdk.core.RagOrchestrator
import kotlinx.coroutines.flow.Flow
import kotlin.native.ObjCName

/**
 * Public entrypoint facade for RagChat SDK.
 */
@ObjCName("RagChatClient")
public class RagChatClient(
    public val config: RagChatConfig = RagChatConfig(),
    private val orchestrator: RagOrchestrator
) {
    public suspend fun sendMessage(content: String): ChatMessage {
        return orchestrator.chat(content)
    }

    public fun streamMessage(content: String): Flow<String> {
        return orchestrator.chatStream(content)
    }

    public companion object {
        public const val VERSION: String = RagChatConfig.SDK_VERSION

        public fun create(
            llmProvider: LlmProvider,
            embeddingProvider: EmbeddingProvider,
            vectorStore: VectorStore,
            config: RagChatConfig = RagChatConfig()
        ): RagChatClient {
            val orchestrator = RagOrchestrator(
                llmProvider = llmProvider,
                embeddingProvider = embeddingProvider,
                vectorStore = vectorStore,
                config = config
            )
            return RagChatClient(config = config, orchestrator = orchestrator)
        }

        /**
         * Swift-friendly factory method accepting [RagChatOptions].
         */
        public fun createWithOptions(
            llmProvider: LlmProvider,
            embeddingProvider: EmbeddingProvider,
            vectorStore: VectorStore,
            options: RagChatOptions
        ): RagChatClient {
            return create(llmProvider, embeddingProvider, vectorStore, options.toConfig())
        }
    }
}
