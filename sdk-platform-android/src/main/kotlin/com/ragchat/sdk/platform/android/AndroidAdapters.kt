package com.ragchat.sdk.platform.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ragchat.sdk.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Base64

/**
 * Android Keystore backed secure storage adapter.
 */
public class AndroidSecureStorage(context: Context) : SecureStorage {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "ragchat_secure_keystore",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun store(key: String, value: ByteArray) {
        prefs.edit().putString(key, Base64.encodeToString(value, Base64.NO_WRAP)).apply()
    }

    override suspend fun retrieve(key: String): ByteArray? {
        val encoded = prefs.getString(key, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

public typealias AndroidSecureKeyStore = AndroidSecureStorage

/**
 * Android LiteRT / MediaPipe GenAI adapter stub.
 */
public class AndroidLiteRtProvider(
    public val modelPath: String,
    override val id: String = "gemma-2b-it-litert",
    override val capabilities: LlmCapabilities = LlmCapabilities(
        contextWindowTokens = 2048,
        supportsStreaming = true,
        supportsToolCalling = false,
        isLocal = true
    )
) : LlmProvider {

    override suspend fun checkAvailability(): ModelAvailability = ModelAvailability(isAvailable = true)

    override fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent> = flow {
        val text = "Android LiteRT output for prompt length: ${prompt.length}"
        emit(LlmEvent.Token(text))
        val usage = TokenUsage(promptTokens = prompt.length / 4, completionTokens = text.length / 4)
        emit(LlmEvent.Usage(usage))
        emit(LlmEvent.Completed(Answer(text = text, usage = usage)))
    }

    override suspend fun countTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    override suspend fun close() {
        // Release LiteRT native session
    }
}

public typealias AndroidLiteRtEngine = AndroidLiteRtProvider

/**
 * Android Gemini Nano system service adapter stub.
 */
public class AndroidGeminiNanoProvider : LlmProvider {
    override val id: String = "gemini-nano"
    override val capabilities: LlmCapabilities = LlmCapabilities(
        contextWindowTokens = 4096,
        supportsStreaming = true,
        supportsToolCalling = false,
        isLocal = true
    )

    override suspend fun checkAvailability(): ModelAvailability {
        // AICore availability check
        return ModelAvailability(isAvailable = false, reason = "Gemini Nano AICore unavailable on this device")
    }

    override fun generate(prompt: String, options: LlmOptions): Flow<LlmEvent> = flow {
        throw SdkError.ModelUnavailableError(modelId = id, isLocal = true, reason = "AICore not initialized")
    }

    override suspend fun countTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    override suspend fun close() {
        // AICore client disconnection
    }
}

public typealias AndroidGeminiNanoEngine = AndroidGeminiNanoProvider
