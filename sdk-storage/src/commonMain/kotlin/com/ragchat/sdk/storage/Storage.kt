package com.ragchat.sdk.storage

import com.ragchat.sdk.api.Document
import com.ragchat.sdk.api.DocumentStore

/**
 * In-memory document storage adapter suitable for testing and runtime session caching.
 */
public class InMemoryDocumentStore : DocumentStore {
    private val documents = mutableMapOf<String, Document>()

    override suspend fun storeDocument(document: Document) {
        documents[document.id] = document
    }

    override suspend fun getDocument(id: String): Document? {
        return documents[id]
    }

    override suspend fun deleteDocument(id: String) {
        documents.remove(id)
    }
}
