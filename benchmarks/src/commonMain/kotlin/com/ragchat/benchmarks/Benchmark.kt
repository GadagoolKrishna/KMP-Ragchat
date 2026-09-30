package com.ragchat.benchmarks

import com.ragchat.sdk.api.EmbeddingVector
import com.ragchat.sdk.retrieval.VectorSimilarity

public class CosineBenchmark {
    public fun runBenchmark(iterations: Int = 1000): Long {
        val v1 = EmbeddingVector(768, List(768) { 0.1f })
        val v2 = EmbeddingVector(768, List(768) { 0.2f })

        val start = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        for (i in 0 until iterations) {
            VectorSimilarity.cosineSimilarity(v1, v2)
        }
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - start
    }
}
