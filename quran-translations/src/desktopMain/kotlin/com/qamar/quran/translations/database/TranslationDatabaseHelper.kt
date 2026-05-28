package com.qamar.quran.translations.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

actual class TranslationDatabaseHelper actual constructor(platformContext: Any?) {
    private val drivers = mutableMapOf<String, SqlDriver>()
    private val baseDir: File =
        File(System.getProperty("user.home") ?: ".", ".qamar/translations").apply { mkdirs() }

    actual suspend fun getDriver(translationId: String): SqlDriver? {
        if (!isDatabaseDownloaded(translationId)) return null
        return drivers.getOrPut(translationId) {
            JdbcSqliteDriver(url = "jdbc:sqlite:${getDatabasePath(translationId)}").also {
                NoOpSchema.create(it)
            }
        }
    }

    actual suspend fun deleteDatabase(translationId: String): Boolean {
        drivers.remove(translationId)?.close()
        val file = File(getDatabasePath(translationId))
        return if (file.exists()) file.delete() else true
    }

    actual suspend fun isDatabaseDownloaded(translationId: String): Boolean =
        File(getDatabasePath(translationId)).exists()

    actual fun getDatabasePath(translationId: String): String =
        File(baseDir, "translation_${translationId}.db").absolutePath

    actual suspend fun writeDatabaseBytes(translationId: String, bytes: ByteArray) {
        drivers.remove(translationId)?.close()
        File(getDatabasePath(translationId)).writeBytes(bytes)
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
