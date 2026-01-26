package com.qamar.quran.core.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.core.resource.ResourceReader
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(null)

    @OptIn(BetaInteropApi::class)
    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val dbPath = NSHomeDirectory() + "/Library/Application Support"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = dbPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val dbFile = "$dbPath/${config.name}"

        if (config.copyBundledIfMissing) {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(dbFile)) {
                runCatching {
                    val bytes = resourceReader.readBytes("databases/quran.db")
                    val data = bytes.usePinned { pinned ->
                        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                    }
                    fileManager.createFileAtPath(dbFile, data, null)
                }
            }
        }

        val driver: SqlDriver = NativeSqliteDriver(QuranDatabase.Schema.synchronous(), dbFile)
        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}
