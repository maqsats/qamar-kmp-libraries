package com.qamar.quran.core.database

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.core.resource.ResourceReader
import java.io.File

actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(null)

    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val dbDir = File(System.getProperty("user.home"), ".qamar/db")
        dbDir.mkdirs()
        val dbFile = File(dbDir, config.name)

        if (config.copyBundledIfMissing && !dbFile.exists()) {
            runCatching {
                val bytes = resourceReader.readBytes("databases/quran.db")
                dbFile.writeBytes(bytes)
            }
        }

        val driver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbFile.absolutePath}")
        QuranDatabase.Schema.awaitCreate(driver)
        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}
