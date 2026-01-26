package com.qamar.quran.api

import com.qamar.quran.core.model.Verse
import com.qamar.quran.transliteration.model.TransliterationLanguage
import com.qamar.quran.translations.model.TranslationInfo

interface QuranApi {
    // Verse Operations
    suspend fun getVerse(sura: Int, ayah: Int): Verse?
    suspend fun getSura(sura: Int): List<Verse>
    suspend fun getPage(page: Int): List<Verse>
    suspend fun getAllVerses(): List<Verse>

    // Page Operations
    suspend fun getAyahPage(sura: Int, ayah: Int): Int
    suspend fun getPageStart(page: Int): Pair<Int, Int>?

    // Translation Operations
    suspend fun getTranslation(sura: Int, ayah: Int, translationId: String? = null): String?
    suspend fun getSuraTranslation(sura: Int, translationId: String? = null): List<String>
    suspend fun getAvailableTranslations(): List<TranslationInfo>
    suspend fun isTranslationDownloaded(translationId: String): Boolean

    // Transliteration Operations
    suspend fun getTransliteration(sura: Int, ayah: Int, language: TransliterationLanguage): String?
    suspend fun getSuraTransliteration(sura: Int, language: TransliterationLanguage): List<String>

    // Search Operations
    suspend fun searchArabic(query: String): List<Verse>
    suspend fun searchTranslation(query: String, translationId: String? = null): List<Verse>
    suspend fun searchTransliteration(query: String, language: TransliterationLanguage): List<Verse>

    // Batch Operations
    suspend fun getVerses(requests: List<VerseRequest>): List<Verse>
    suspend fun getVerseRange(sura: Int, startAyah: Int, endAyah: Int): List<Verse>

    // Caching
    suspend fun clearCache()
    suspend fun preloadSura(sura: Int)
    suspend fun preloadPage(page: Int)
}

data class VerseRequest(
    val sura: Int,
    val ayah: Int,
)
