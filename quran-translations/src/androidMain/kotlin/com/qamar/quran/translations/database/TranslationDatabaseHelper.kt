package com.qamar.quran.translations.database

import android.content.Context
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

actual class TranslationDatabaseHelper actual constructor(platformContext: Any?) {
    private val context = platformContext as? Context
        ?: throw IllegalArgumentException("Android requires Context")
    private val drivers = mutableMapOf<String, SqlDriver>()

    actual suspend fun getDriver(translationId: String): SqlDriver? {
        if (!isDatabaseDownloaded(translationId)) return null
        return drivers.getOrPut(translationId) {
            AndroidSqliteDriver(
                schema = NoOpSchema,
                context = context,
                name = dbName(translationId),
            )
        }
    }

    actual suspend fun deleteDatabase(translationId: String): Boolean {
        drivers.remove(translationId)?.close()
        val file = File(getDatabasePath(translationId))
        return if (file.exists()) file.delete() else true
    }

    actual suspend fun isDatabaseDownloaded(translationId: String): Boolean {
        val file = File(getDatabasePath(translationId))
        if (!file.exists() || file.length() < 16) return false
        return runCatching {
            file.inputStream().use { stream ->
                val header = ByteArray(15)
                val read = stream.read(header)
                read == 15 && header.decodeToString() == SqliteMagic
            }
        }.getOrDefault(false)
    }

    actual fun getDatabasePath(translationId: String): String {
        return context.getDatabasePath(dbName(translationId)).absolutePath
    }

    actual suspend fun writeDatabaseBytes(translationId: String, bytes: ByteArray) {
        drivers.remove(translationId)?.close()
        val file = File(getDatabasePath(translationId))
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    actual suspend fun decompressIfZip(bytes: ByteArray): ByteArray {
        if (!bytes.isZip()) return bytes
        return runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val out = ByteArrayOutputStream()
                        zip.copyTo(out)
                        return@use out.toByteArray()
                    }
                    entry = zip.nextEntry
                }
                bytes
            }
        }.getOrDefault(bytes)
    }

    private fun dbName(translationId: String): String = "translation_${translationId}.db"

    private companion object {
        const val SqliteMagic = "SQLite format 3"
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
