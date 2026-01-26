package com.qamar.quran.transliteration

import com.qamar.quran.transliteration.model.TransliterationLanguage

interface TransliterationProvider {
    suspend fun getTransliteration(
        sura: Int,
        ayah: Int,
        language: TransliterationLanguage,
    ): String?

    suspend fun getSuraTransliteration(
        sura: Int,
        language: TransliterationLanguage,
    ): List<String>

    suspend fun search(
        query: String,
        language: TransliterationLanguage,
    ): List<Pair<Int, Int>> // (sura, ayah)
}
