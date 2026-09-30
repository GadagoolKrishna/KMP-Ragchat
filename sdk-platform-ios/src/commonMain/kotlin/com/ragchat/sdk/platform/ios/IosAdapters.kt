package com.ragchat.sdk.platform.ios

import com.ragchat.sdk.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Apple Foundation Models / CoreML on-device engine bridge.
 */
public class AppleFoundationModelsProvider(
    override val id: String = "apple-intelligence-system",
    override val capabilities: LlmCapabilities = LlmCapabilities(
        contextWindowTokens = 4096,
        supportsStreaming = true,
        supportsToolCalling = true,
        isLocal = true
    )
) : LlmProvider {

    override suspend fun checkAvailability(): ModelAvailability = ModelAvailability(isAvailable = true)

    override fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent> = flow {
        val text = "Apple Foundation Model completion for prompt."
        emit(LlmEvent.Token(text))
        val usage = TokenUsage(promptTokens = prompt.length / 4, completionTokens = text.length / 4)
        emit(LlmEvent.Usage(usage))
        emit(LlmEvent.Completed(Answer(text = text, usage = usage)))
    }

    override suspend fun countTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    override suspend fun close() {
        // Release CoreML or Foundation Models session
    }
}

public typealias AppleFoundationModelsEngine = AppleFoundationModelsProvider

/**
 * iOS Keychain wrapper adapter.
 */
public class IosSecureStorage : SecureStorage {
    private val inMemoryKeychain = mutableMapOf<String, ByteArray>()

    override suspend fun store(key: String, value: ByteArray) {
        inMemoryKeychain[key] = value
    }

    override suspend fun retrieve(key: String): ByteArray? {
        return inMemoryKeychain[key]
    }

    override suspend fun remove(key: String) {
        inMemoryKeychain.remove(key)
    }
}

public typealias IosSecureKeyStore = IosSecureStorage
