package com.qamar.quran.core.database

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.db.SqlDriver
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.core.resource.ResourceReader
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class DatabaseSeeder(
    private val driver: SqlDriver,
    private val resourceReader: ResourceReader = ResourceReader(null),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty() {
        val db = QuranDatabase(driver)
        val count = db.quranDatabaseQueries.countVerses().awaitAsOne()
        if (count > 0L) return

        val arabic = json.decodeFromString(
            ListSerializer(ListSerializer(String.serializer())),
            resourceReader.readText("arabic.json"),
        )
        val quranPaged = json.decodeFromString(
            ListSerializer(ListSerializer(Int.serializer())),
            resourceReader.readText("quran_paged.json"),
        )

        db.quranDatabaseQueries.transaction {
            arabic.forEachIndexed { suraIndex, verses ->
                val sura = suraIndex + 1
                verses.forEachIndexed { ayahIndex, text ->
                    val ayah = ayahIndex + 1
                    db.quranDatabaseQueries.insertVerse(
                        sura = sura.toLong(),
                        ayah = ayah.toLong(),
                        arabic_text = text,
                    )
                }
            }
            quranPaged.forEach { row ->
                if (row.size >= 3) {
                    val page = row[0].toLong()
                    val sura = row[1].toLong()
                    val ayah = row[2].toLong()
                    db.quranDatabaseQueries.insertPage(
                        page = page,
                        sura = sura,
                        ayah = ayah,
                    )
                }
            }
        }
    }
}
