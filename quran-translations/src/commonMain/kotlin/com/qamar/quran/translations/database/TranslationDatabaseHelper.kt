package com.qamar.quran.translations.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.qamar.quran.translations.model.TranslationSearchHit

expect class TranslationDatabaseHelper(platformContext: Any?) {
    suspend fun getDriver(translationId: String): SqlDriver?
    suspend fun deleteDatabase(translationId: String): Boolean
    suspend fun isDatabaseDownloaded(translationId: String): Boolean
    fun getDatabasePath(translationId: String): String
    suspend fun writeDatabaseBytes(translationId: String, bytes: ByteArray)
    suspend fun decompressIfZip(bytes: ByteArray): ByteArray
}

fun ByteArray.isZip(): Boolean =
    size >= 4 &&
        this[0] == 0x50.toByte() &&
        this[1] == 0x4B.toByte() &&
        (this[2] == 0x03.toByte() || this[2] == 0x05.toByte() || this[2] == 0x07.toByte())

suspend fun TranslationDatabaseHelper.getTranslationText(translationId: String, sura: Int, ayah: Int): String? {
    val driver = getDriver(translationId) ?: return null
    return runCatching {
        val queryResult = driver.executeQuery(
            identifier = null,
            sql = "SELECT text FROM verses WHERE sura = ? AND ayah = ?",
            mapper = { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
            },
            parameters = 2,
            binders = {
                bindLong(0, sura.toLong())
                bindLong(1, ayah.toLong())
            }
        )
        queryResult.value
    }.getOrNull()
}

/**
 * Full-text-ish search over a downloaded translation's `verses` table. Matches
 * are case-insensitive substring (`LIKE`); `%`/`_`/`\` in the query are escaped
 * so they're treated literally. Returns up to [limit] hits ordered by position.
 */
suspend fun TranslationDatabaseHelper.searchTranslation(
    translationId: String,
    query: String,
    limit: Int = 50,
): List<TranslationSearchHit> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val driver = getDriver(translationId) ?: return emptyList()
    val escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    val like = "%$escaped%"
    return runCatching {
        driver.executeQuery(
            identifier = null,
            sql = "SELECT sura, ayah, text FROM verses WHERE text LIKE ? ESCAPE '\\' ORDER BY sura, ayah LIMIT ?",
            mapper = { cursor ->
                val hits = mutableListOf<TranslationSearchHit>()
                while (cursor.next().value) {
                    val sura = cursor.getLong(0)?.toInt()
                    val ayah = cursor.getLong(1)?.toInt()
                    val text = cursor.getString(2)
                    if (sura != null && ayah != null && text != null) {
                        hits.add(TranslationSearchHit(sura, ayah, text))
                    }
                }
                QueryResult.Value(hits.toList())
            },
            parameters = 2,
            binders = {
                bindString(0, like)
                bindLong(1, limit.toLong())
            },
        ).value
    }.getOrDefault(emptyList())
}
