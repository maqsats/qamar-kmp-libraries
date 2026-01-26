package com.qamar.quran.search

import com.qamar.quran.api.QuranApi
import com.qamar.quran.core.model.Verse
import com.qamar.quran.transliteration.model.TransliterationLanguage

class QuranSearchEngine(
    private val api: QuranApi,
) {
    suspend fun searchArabic(query: String): List<Verse> = api.searchArabic(query)

    suspend fun searchTranslation(query: String, translationId: String? = null): List<Verse> =
        api.searchTranslation(query, translationId)

    suspend fun searchTransliteration(query: String, language: TransliterationLanguage): List<Verse> =
        api.searchTransliteration(query, language)
}
