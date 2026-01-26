package com.qamar.quran.core.database

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.qamar.quran.core.resource.ResourceReader
import java.io.File

actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(null)

    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val dbDir = File(System.getProperty("user.home"), ".qamar/db")
        dbDir.mkdirs()
        val dbFile = File(dbDir, config.name)

        val bundledDbCopied = if (config.copyBundledIfMissing && !dbFile.exists()) {
            runCatching {
                val bytes = resourceReader.readBytes("databases/quran.db")
                dbFile.writeBytes(bytes)
            }.isSuccess
        } else {
            false
        }

        val driver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbFile.absolutePath}")

        // Only create schema if the database wasn't copied from bundle
        // If it was copied, the schema already exists
        // If it wasn't copied but tables exist, awaitCreate will handle it gracefully
        if (!bundledDbCopied) {
            try {
                QuranDatabase.Schema.awaitCreate(driver)
            } catch (e: Exception) {
                // If tables already exist, that's fine - the database is already initialized
                // Check if it's a SQLite "table already exists" error
                val isTableExistsError = e.javaClass.name.contains("SQLiteException") &&
                        (e.message?.contains("already exists") == true ||
                                e.message?.contains("table") == true && e.message?.contains("exists") == true)
                if (!isTableExistsError) {
                    throw e
                }
            }
        }

        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}
