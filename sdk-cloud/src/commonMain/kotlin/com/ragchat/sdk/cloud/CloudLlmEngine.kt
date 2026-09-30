package com.ragchat.sdk.cloud

import com.ragchat.sdk.api.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public class CloudLlmProvider(
    private val authProvider: AuthProvider,
    private val httpClient: HttpClient = HttpClient(),
    override val id: String = "gemini-1.5-pro",
    override val capabilities: LlmCapabilities = LlmCapabilities(
        contextWindowTokens = 128_000,
        supportsStreaming = true,
        supportsToolCalling = true,
        isLocal = false
    )
) : LlmProvider {

    override suspend fun checkAvailability(): ModelAvailability {
        val token = authProvider.getBearerToken()
        return if (token != null) {
            ModelAvailability(isAvailable = true)
        } else {
            ModelAvailability(isAvailable = false, reason = "Missing or expired authorization token")
        }
    }

    override fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent> = flow {
        emit(LlmEvent.Token("Cloud completion for $id"))
        val usage = TokenUsage(promptTokens = prompt.length / 4, completionTokens = 16)
        emit(LlmEvent.Usage(usage))
        emit(LlmEvent.Completed(Answer(text = "Cloud completion for $id", usage = usage)))
    }

    override suspend fun countTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    override suspend fun close() {
        httpClient.close()
    }
}

public typealias CloudLlmEngine = CloudLlmProvider

public class CloudEmbeddingProvider(
    private val authProvider: AuthProvider,
    override val id: String = "text-embedding-004",
    override val dimensions: Int = 768,
    override val maxBatchSize: Int = 64
) : EmbeddingProvider {

    override suspend fun embed(text: String): EmbeddingVector {
        return EmbeddingVector(dimensions = dimensions, values = List(dimensions) { 0.01f })
    }

    override suspend fun embedBatch(texts: List<String>): List<EmbeddingVector> {
        return texts.map { embed(it) }
    }
}

public typealias CloudEmbeddingModel = CloudEmbeddingProvider
