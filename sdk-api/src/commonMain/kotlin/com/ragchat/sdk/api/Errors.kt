package com.ragchat.sdk.api

import kotlin.native.ObjCName

/**
 * Root sealed hierarchy for all typed errors emitted by the RagChat SDK.
 *
 * Designed for clean Swift bridging via SKIE or native ObjC name mapping.
 */
@ObjCName("RagSdkError")
public sealed class SdkError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Error raised when SDK components fail initialization or dependency validation.
     */
    @ObjCName("RagSdkInitializationError")
    public class InitializationError(
        message: String,
        cause: Throwable? = null
    ) : SdkError(message, cause)

    /**
     * Error raised when an LLM, embedding, or parsing model is unavailable on the current device or target.
     */
    @ObjCName("RagSdkModelUnavailableError")
    public class ModelUnavailableError(
        public val modelId: String,
        public val isLocal: Boolean,
        public val reason: String
    ) : SdkError("Model '$modelId' is unavailable (isLocal=$isLocal): $reason")

    /**
     * Error raised during LLM prompt evaluation or token generation.
     */
    @ObjCName("RagSdkInferenceError")
    public class InferenceError(
        public val modelId: String,
        message: String,
        public val statusCode: Int? = null,
        cause: Throwable? = null
    ) : SdkError("Inference failed for '$modelId' (statusCode=$statusCode): $message", cause)

    /**
     * Error raised during vector embedding computation.
     */
    @ObjCName("RagSdkEmbeddingError")
    public class EmbeddingError(
        public val modelId: String,
        message: String,
        cause: Throwable? = null
    ) : SdkError("Embedding generation failed for '$modelId': $message", cause)

    /**
     * Error raised during persistent database, file, or key storage operations.
     */
    @ObjCName("RagSdkStorageError")
    public class StorageError(
        public val operation: String,
        message: String,
        cause: Throwable? = null
    ) : SdkError("Storage operation '$operation' failed: $message", cause)

    /**
     * Error raised during document reading, parsing, or chunking.
     */
    @ObjCName("RagSdkDocumentParsingError")
    public class DocumentParsingError(
        public val sourceUri: String,
        public val format: String,
        message: String,
        cause: Throwable? = null
    ) : SdkError("Failed parsing document '$sourceUri' (format=$format): $message", cause)

    /**
     * Error raised when a safety rule, guardrail, or enterprise policy rejects an input or output.
     */
    @ObjCName("RagSdkPolicyViolationError")
    public class PolicyViolationError(
        public val ruleId: String,
        public val reason: String,
        public val isBlocked: Boolean
    ) : SdkError("Policy rule '$ruleId' violated (isBlocked=$isBlocked): $reason")

    /**
     * Error raised when required user consent is absent.
     */
    @ObjCName("RagSdkConsentRequiredError")
    public class ConsentRequiredError(
        public val consentType: String,
        message: String
    ) : SdkError("Consent required for '$consentType': $message")

    /**
     * Error raised when an authentication credential cannot be retrieved, is expired, or is unauthorized.
     */
    @ObjCName("RagSdkAuthError")
    public class AuthError(
        message: String,
        cause: Throwable? = null
    ) : SdkError(message, cause)

    /**
     * Error raised during network communication with cloud LLM or embedding services.
     */
    @ObjCName("RagSdkNetworkError")
    public class NetworkError(
        public val statusCode: Int?,
        message: String,
        cause: Throwable? = null
    ) : SdkError("Network error (statusCode=$statusCode): $message", cause)
}
