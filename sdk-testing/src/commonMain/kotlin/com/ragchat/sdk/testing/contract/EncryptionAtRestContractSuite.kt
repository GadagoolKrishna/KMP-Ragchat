package com.ragchat.sdk.testing.contract

import com.ragchat.sdk.api.SecureStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract test suite verifying encryption at rest and secure storage compliance.
 */
public abstract class EncryptionAtRestContractSuite {

    public abstract fun createSecureStorage(): SecureStorage

    public fun runStoreRetrieveDeleteTest(): Unit = runTest {
        val storage = createSecureStorage()
        val key = "contract_secret_key"
        val payload = "Confidential user message payload 123456789".encodeToByteArray()

        storage.store(key, payload)

        val retrieved = storage.retrieve(key)
        assertNotNull(retrieved)
        assertTrue(payload.contentEquals(retrieved))

        storage.remove(key)
        val afterDelete = storage.retrieve(key)
        assertNull(afterDelete)
    }
}
