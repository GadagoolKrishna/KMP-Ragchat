package com.ragchat.sdk.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ragchat.sdk.storage.db.RagChatDatabase
import com.ragchat.sdk.testing.contract.StorageContractSuite
import kotlin.test.Test

class CommonStorageContractTest : StorageContractSuite() {

    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        RagChatDatabase.Schema.create(driver)
        return driver
    }

    @Test
    fun testScopeIsolation() {
        runScopeIsolationTest()
    }

    @Test
    fun testPurgeCompleteness() {
        runPurgeCompletenessTest()
    }

    @Test
    fun testQuantizedEmbeddings() {
        runQuantizedEmbeddingsTest()
    }

    @Test
    fun testCryptoShredding() {
        runCryptoShreddingTest()
    }
}
