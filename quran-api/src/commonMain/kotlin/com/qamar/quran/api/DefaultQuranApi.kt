package com.qamar.quran.api

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.core.model.Verse
import com.qamar.quran.translations.TranslationManager
import com.qamar.quran.translations.model.TranslationInfo
import com.qamar.quran.transliteration.TransliterationProvider
import com.qamar.quran.transliteration.model.TransliterationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultQuranApi(
    private val database: QuranDatabase,
    private val translationManager: TranslationManager? = null,
    private val transliterationProvider: TransliterationProvider? = null,
    private val cacheSize: Int = 256,
) : QuranApi {
    private val cacheMap = LinkedHashMap<String, Verse>(cacheSize)
    private val cacheOrder = ArrayDeque<String>()

    private fun cacheKey(sura: Int, ayah: Int) = "$sura:$ayah"

    private fun Verse.withCache(): Verse {
        putCache(cacheKey(sura, ayah), this)
        return this
    }

    private fun getCache(key: String): Verse? {
        val value = cacheMap[key] ?: return null
        cacheOrder.remove(key)
        cacheOrder.addLast(key)
        return value
    }

    private fun putCache(key: String, value: Verse) {
        if (cacheMap.containsKey(key)) {
            cacheOrder.remove(key)
        } else if (cacheMap.size >= cacheSize) {
            val eldest = cacheOrder.removeFirstOrNull()
            if (eldest != null) {
                cacheMap.remove(eldest)
            }
        }
        cacheMap[key] = value
        cacheOrder.addLast(key)
    }

    override suspend fun getVerse(sura: Int, ayah: Int): Verse? = withContext(Dispatchers.Default) {
        getCache(cacheKey(sura, ayah)) ?: database.quranDatabaseQueries.getVerse(
            sura = sura.toLong(),
            ayah = ayah.toLong(),
        ).awaitAsOneOrNull()?.let { row ->
            Verse(
                sura = row.sura.toInt(),
                ayah = row.ayah.toInt(),
                arabicText = row.arabic_text,
                page = runCatching { getAyahPage(sura, ayah) }.getOrNull(),
            ).withCache()
        }
    }

    override suspend fun getSura(sura: Int): List<Verse> = withContext(Dispatchers.Default) {
        database.quranDatabaseQueries.getSura(sura.toLong()).awaitAsList().map { row ->
            Verse(
                sura = row.sura.toInt(),
                ayah = row.ayah.toInt(),
                arabicText = row.arabic_text,
                page = runCatching { getAyahPage(row.sura.toInt(), row.ayah.toInt()) }.getOrNull(),
            ).withCache()
        }
    }

    override suspend fun getPage(page: Int): List<Verse> = withContext(Dispatchers.Default) {
        database.quranDatabaseQueries.getPage(page.toLong()).awaitAsList().map { row ->
            Verse(
                sura = row.sura.toInt(),
                ayah = row.ayah.toInt(),
                arabicText = row.arabic_text,
                page = page,
            ).withCache()
        }
    }

    override suspend fun getAllVerses(): List<Verse> = withContext(Dispatchers.Default) {
        database.quranDatabaseQueries.getAllVerses().awaitAsList().map { row ->
            Verse(
                sura = row.sura.toInt(),
                ayah = row.ayah.toInt(),
                arabicText = row.arabic_text,
            ).withCache()
        }
    }

    override suspend fun getAyahPage(sura: Int, ayah: Int): Int = withContext(Dispatchers.Default) {
        database.quranDatabaseQueries.getAyahPage(sura.toLong(), ayah.toLong()).awaitAsOne().toInt()
    }

    override suspend fun getPageStart(page: Int): Pair<Int, Int>? =
        withContext(Dispatchers.Default) {
            database.quranDatabaseQueries.getPageStart(page.toLong()).awaitAsOneOrNull()
                ?.let { it.sura.toInt() to it.ayah.toInt() }
        }

    override suspend fun getTranslation(sura: Int, ayah: Int, translationId: String?): String? {
        // TODO: hook into translation storage/DB once wired.
        return translationManager?.let { manager ->
            // Placeholder to ensure API contract without crashing.
            val available = manager.getAvailableTranslations()
            val target = translationId ?: available.firstOrNull()?.translationId
            if (target != null) {
                // Real translation lookup should be implemented.
                null
            } else null
        }
    }

    override suspend fun getSuraTranslation(sura: Int, translationId: String?): List<String> {
        // TODO: implement using translation storage.
        return emptyList()
    }

    override suspend fun getAvailableTranslations(): List<TranslationInfo> =
        translationManager?.getAvailableTranslations().orEmpty()

    override suspend fun isTranslationDownloaded(translationId: String): Boolean =
        translationManager?.isTranslationDownloaded(translationId) ?: false

    override suspend fun getTransliteration(
        sura: Int,
        ayah: Int,
        language: TransliterationLanguage,
    ): String? = transliterationProvider?.getTransliteration(sura, ayah, language)

    override suspend fun getSuraTransliteration(
        sura: Int,
        language: TransliterationLanguage,
    ): List<String> = transliterationProvider?.getSuraTransliteration(sura, language).orEmpty()

    override suspend fun searchArabic(query: String): List<Verse> =
        withContext(Dispatchers.Default) {
            if (query.isBlank()) return@withContext emptyList()
            database.quranDatabaseQueries.searchArabic(query).awaitAsList().map { row ->
                Verse(
                    sura = row.sura.toInt(),
                    ayah = row.ayah.toInt(),
                    arabicText = row.arabic_text,
                )
            }
        }

    override suspend fun searchTranslation(
        query: String,
        translationId: String?,
    ): List<Verse> {
        // TODO: implement when translation storage is wired.
        return emptyList()
    }

    override suspend fun searchTransliteration(
        query: String,
        language: TransliterationLanguage,
    ): List<Verse> {
        val provider = transliterationProvider ?: return emptyList()
        if (query.isBlank()) return emptyList()
        val hits = provider.search(query, language)
        return hits.mapNotNull { (sura, ayah) -> getVerse(sura, ayah) }
    }

    override suspend fun getVerses(requests: List<VerseRequest>): List<Verse> =
        requests.mapNotNull { getVerse(it.sura, it.ayah) }

    override suspend fun getVerseRange(sura: Int, startAyah: Int, endAyah: Int): List<Verse> =
        getSura(sura).filter { it.ayah in startAyah..endAyah }

    override suspend fun clearCache() {
        cacheMap.clear()
        cacheOrder.clear()
    }

    override suspend fun preloadSura(sura: Int) {
        getSura(sura)
    }

    override suspend fun preloadPage(page: Int) {
        getPage(page)
    }
}
