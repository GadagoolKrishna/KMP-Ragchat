package com.ragchat.sdk.ingestion

import com.ragchat.sdk.api.Chunk
import com.ragchat.sdk.api.Chunker
import com.ragchat.sdk.api.Document
import com.ragchat.sdk.api.ParsedElement

/**
 * Interface for splitting documents into chunks.
 */
public interface DocumentChunker {
    public fun chunk(document: Document): List<Chunk>
}

/**
 * Fixed-window chunker with overlap implementing both [DocumentChunker] and SPI [Chunker].
 */
public class FixedWindowChunker(
    override val id: String = "fixed-window",
    private val chunkSizeChars: Int = 500,
    private val overlapChars: Int = 100
) : DocumentChunker, Chunker {

    override fun chunk(document: Document): List<Chunk> {
        val text = document.content
        if (text.isEmpty()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var start = 0
        var index = 0

        while (start < text.length) {
            val end = minOf(start + chunkSizeChars, text.length)
            val chunkText = text.substring(start, end)
            chunks.add(
                Chunk(
                    id = "${document.id}_chunk_$index",
                    documentId = document.id,
                    text = chunkText,
                    chunkIndex = index,
                    tokenCount = chunkText.split(Regex("\\s+")).size
                )
            )
            index++
            if (end == text.length) break
            start += (chunkSizeChars - overlapChars)
        }
        return chunks
    }

    override suspend fun chunk(document: Document, elements: List<ParsedElement>): List<Chunk> {
        return if (elements.isNotEmpty()) {
            val textContent = elements.filterIsInstance<ParsedElement.Text>().joinToString("\n\n") { it.content }
            chunk(document.copy(content = textContent))
        } else {
            chunk(document)
        }
    }
}
