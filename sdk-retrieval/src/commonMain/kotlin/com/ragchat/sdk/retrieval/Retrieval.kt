package com.ragchat.sdk.retrieval

import com.ragchat.sdk.api.Chunk
import com.ragchat.sdk.api.EmbeddingVector
import com.ragchat.sdk.api.SearchResult
import kotlin.math.sqrt

public object VectorSimilarity {
    public fun cosineSimilarity(a: EmbeddingVector, b: EmbeddingVector): Float {
        if (a.dimensions != b.dimensions || a.dimensions == 0) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until a.dimensions) {
            val valA = a.values[i]
            val valB = b.values[i]
            dot += valA * valB
            normA += valA * valA
            normB += valB * valB
        }
        if (normA <= 0f || normB <= 0f) return 0f
        return (dot / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }
}

/**
 * Reciprocal Rank Fusion (RRF) for combining dense vector and lexical ranks.
 */
public object ReciprocalRankFusion {
    public fun fuse(
        denseResults: List<SearchResult>,
        lexicalResults: List<SearchResult>,
        kConstant: Int = 60
    ): List<SearchResult> {
        val scoreMap = mutableMapOf<String, Float>()
        val chunkMap = mutableMapOf<String, Chunk>()

        denseResults.forEachIndexed { rank, item ->
            scoreMap[item.chunk.id] = (scoreMap[item.chunk.id] ?: 0f) + (1f / (kConstant + rank + 1))
            chunkMap[item.chunk.id] = item.chunk
        }

        lexicalResults.forEachIndexed { rank, item ->
            scoreMap[item.chunk.id] = (scoreMap[item.chunk.id] ?: 0f) + (1f / (kConstant + rank + 1))
            chunkMap[item.chunk.id] = item.chunk
        }

        return scoreMap.entries
            .sortedByDescending { it.value }
            .map { SearchResult(chunk = chunkMap.getValue(it.key), score = it.value) }
    }
}
