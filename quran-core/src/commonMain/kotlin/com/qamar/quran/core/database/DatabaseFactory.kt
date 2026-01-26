package com.qamar.quran.core.database

import app.cash.sqldelight.db.SqlDriver

data class DatabaseConfig(
    val name: String = "quran.db",
    val copyBundledIfMissing: Boolean = true,
)

expect class DatabaseFactory(platformContext: Any?) {
    val platformContext: Any?
    suspend fun createDriver(config: DatabaseConfig = DatabaseConfig()): SqlDriver
}
