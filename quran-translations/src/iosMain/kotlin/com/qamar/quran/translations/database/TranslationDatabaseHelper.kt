package com.qamar.quran.translations.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile

actual class TranslationDatabaseHelper actual constructor(platformContext: Any?) {
    private val drivers = mutableMapOf<String, SqlDriver>()
    private val fileManager = NSFileManager.defaultManager

    @OptIn(ExperimentalForeignApi::class)
    private val dbDir: String
        get() {
            // Must match co.touchlab.sqliter.DatabaseFileContext.databaseDirPath():
            // NSApplicationSupportDirectory + "/databases". Without the "/databases"
            // suffix, NativeSqliteDriver opens an empty DB at a different path and
            // queries fail with "no such table: verses".
            val dbPath = NSHomeDirectory() + "/Library/Application Support/databases"
            fileManager.createDirectoryAtPath(
                path = dbPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            return dbPath
        }

    actual suspend fun getDriver(translationId: String): SqlDriver? {
        if (!isDatabaseDownloaded(translationId)) return null
        return drivers.getOrPut(translationId) {
            NativeSqliteDriver(
                schema = NoOpSchema,
                name = "translation_${translationId}.db",
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun deleteDatabase(translationId: String): Boolean {
        drivers.remove(translationId)?.close()
        val path = getDatabasePath(translationId)
        return if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, null)
        } else true
    }

    actual suspend fun isDatabaseDownloaded(translationId: String): Boolean {
        return fileManager.fileExistsAtPath(getDatabasePath(translationId))
    }

    actual fun getDatabasePath(translationId: String): String {
        return "$dbDir/translation_${translationId}.db"
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun writeDatabaseBytes(translationId: String, bytes: ByteArray) {
        drivers.remove(translationId)?.close()
        val path = getDatabasePath(translationId)
        val data = bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.toULong(),
            )
        }
        data.writeToFile(path, atomically = true)
    }

    actual suspend fun decompressIfZip(bytes: ByteArray): ByteArray {
        if (!bytes.isZip()) return bytes
        return IosZipExtractor.extractFirstEntry(bytes) ?: bytes
    }

    private object NoOpSchema : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1
        override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Value(Unit)
        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ): QueryResult.Value<Unit> = QueryResult.Value(Unit)
    }
}
