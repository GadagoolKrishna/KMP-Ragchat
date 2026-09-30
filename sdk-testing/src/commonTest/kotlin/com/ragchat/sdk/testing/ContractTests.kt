package com.ragchat.sdk.testing

import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class InMemoryVectorStoreTest : VectorStoreContract() {
    override fun createStore() = InMemoryVectorStore()

    @Test
    fun testVectorStoreContract() = runTest {
        verifyUpsertAndSearch()
    }
}

class FakeSecureKeyStoreTest : SecureKeyStoreContract() {
    override fun createStorage() = FakeSecureKeyStore()

    @Test
    fun testKeyStoreContract() = runTest {
        verifyStoreAndRetrieveKey()
    }
}
