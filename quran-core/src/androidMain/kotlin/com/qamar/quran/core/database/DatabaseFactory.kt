package com.qamar.quran.core.database

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.synchronous
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.core.resource.ResourceReader
import android.content.Context
import java.io.File

actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(platformContext)

    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val context = this.platformContext as? Context
            ?: throw IllegalArgumentException("Android DatabaseFactory requires a Context")

        if (config.copyBundledIfMissing) {
            val dbFile: File = context.getDatabasePath(config.name)
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
                // Try copy from bundled sqlite asset first, fallback to JSON seeding.
                runCatching {
                    context.assets.open("databases/quran.db").use { input ->
                        dbFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }.onFailure { dbFile.delete() }
            }
        }

        val driver = AndroidSqliteDriver(
            schema = QuranDatabase.Schema.synchronous(),
            context = context,
            name = config.name,
        )

        // Ensure data is present (use JSON seed if copy failed).
        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}
