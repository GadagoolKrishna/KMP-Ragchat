package com.ragchat.sdk.api

import kotlinx.serialization.Serializable
import kotlin.native.ObjCName

/**
 * Immutable configuration snapshot for RagChat SDK.
 *
 * Designed to be created either through the Kotlin DSL [RagChatConfigBuilder]
 * or directly via [RagChatOptions] from Swift/Objective-C.
 *
 * @property appPrivateDirectory Root sandboxed directory path for SDK data storage.
 * @property maxContextTokens Budget threshold for prompt assembling and retrieval context.
 * @property topKRetrieval Default count of top-k chunks retrieved.
 * @property hybridLexicalWeight Balancing factor between lexical BM25 and vector search (0.0 = dense only, 1.0 = lexical only).
 * @property routingMode Default routing behavior (e.g. [ModelRoutingMode.AUTO]).
 * @property enablePiiRedaction Whether PII redaction runs prior to prompt assembly and storage.
 * @property enableAuditChain Whether cryptographic event hash chains are recorded.
 */
@Serializable
@ObjCName("RagChatConfig")
public data class RagChatConfig(
    public val appPrivateDirectory: String = "",
    public val maxContextTokens: Int = 2048,
    public val topKRetrieval: Int = 5,
    public val hybridLexicalWeight: Float = 0.3f,
    public val routingMode: ModelRoutingMode = ModelRoutingMode.AUTO,
    public val enablePiiRedaction: Boolean = true,
    public val enableAuditChain: Boolean = true
) {
    public companion object {
        public const val SDK_VERSION: String = "0.1.0-alpha01"

        /**
         * Swift-friendly factory method for creating default options.
         */
        public fun defaultOptions(): RagChatOptions = RagChatOptions()
    }
}

/**
 * Swift-friendly mutable configuration builder / options carrier.
 *
 * Allows Swift consumers to construct configuration without Kotlin lambda DSLs.
 */
@ObjCName("RagChatOptions")
public class RagChatOptions {
    public var appPrivateDirectory: String = ""
    public var maxContextTokens: Int = 2048
    public var topKRetrieval: Int = 5
    public var hybridLexicalWeight: Float = 0.3f
    public var routingMode: ModelRoutingMode = ModelRoutingMode.AUTO
    public var enablePiiRedaction: Boolean = true
    public var enableAuditChain: Boolean = true

    /**
     * Builds an immutable [RagChatConfig] instance.
     */
    public fun toConfig(): RagChatConfig = RagChatConfig(
        appPrivateDirectory = appPrivateDirectory,
        maxContextTokens = maxContextTokens,
        topKRetrieval = topKRetrieval,
        hybridLexicalWeight = hybridLexicalWeight,
        routingMode = routingMode,
        enablePiiRedaction = enablePiiRedaction,
        enableAuditChain = enableAuditChain
    )
}

/**
 * Type-safe Kotlin DSL builder for [RagChatConfig].
 */
public class RagChatConfigBuilder {
    public var appPrivateDirectory: String = ""
    public var maxContextTokens: Int = 2048
    public var topKRetrieval: Int = 5
    public var hybridLexicalWeight: Float = 0.3f
    public var routingMode: ModelRoutingMode = ModelRoutingMode.AUTO
    public var enablePiiRedaction: Boolean = true
    public var enableAuditChain: Boolean = true

    public fun build(): RagChatConfig = RagChatConfig(
        appPrivateDirectory = appPrivateDirectory,
        maxContextTokens = maxContextTokens,
        topKRetrieval = topKRetrieval,
        hybridLexicalWeight = hybridLexicalWeight,
        routingMode = routingMode,
        enablePiiRedaction = enablePiiRedaction,
        enableAuditChain = enableAuditChain
    )
}

/**
 * DSL entry point for configuring the SDK in idiomatic Kotlin.
 */
public fun ragChatConfig(block: RagChatConfigBuilder.() -> Unit): RagChatConfig {
    return RagChatConfigBuilder().apply(block).build()
}
