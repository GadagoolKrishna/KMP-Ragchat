package com.ragchat.sdk.platform.android.storage

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ragchat.sdk.storage.db.RagChatDatabase

/**
 * Factory creating an [AndroidSqliteDriver] for [RagChatDatabase] with foreign keys enabled.
 */
public object AndroidSqlDelightDriverFactory {

    public fun create(
        context: Context,
        dbName: String = "ragchat.db"
    ): SqlDriver {
        return AndroidSqliteDriver(
            schema = RagChatDatabase.Schema,
            context = context,
            name = dbName,
            callback = object : AndroidSqliteDriver.Callback(RagChatDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys=ON;")
                }
            }
        )
    }
}
