package com.qamar.quran.core.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.qamar.quran.core.resource.ResourceReader
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(null)
    private val fileManager = NSFileManager.defaultManager

    /**
     * Must match [co.touchlab.sqliter.DatabaseFileContext.databaseDirPath]:
     * Application Support + "/databases". [NativeSqliteDriver] takes a file name only.
     */
    private val databaseDirectory: String
        get() {
            val path = NSHomeDirectory() + "/Library/Application Support/databases"
            fileManager.createDirectoryAtPath(
                path = path,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            return path
        }

    @OptIn(BetaInteropApi::class)
    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val dbFile = "${databaseDirectory}/${config.name}"

        if (config.copyBundledIfMissing && !fileManager.fileExistsAtPath(dbFile)) {
            runCatching {
                val bytes = runCatching { resourceReader.readBytes("databases/quran.db") }
                    .getOrElse { resourceReader.readBytes("quran.db") }
                val data = bytes.usePinned { pinned ->
                    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                }
                fileManager.createFileAtPath(dbFile, data, null)
            }
        }

        val driver: SqlDriver = NativeSqliteDriver(
            schema = QuranDatabase.Schema.synchronous(),
            name = config.name,
        )
        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}
