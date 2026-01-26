package com.qamar.quran.transliteration

import com.qamar.quran.core.resource.ResourceReader
import com.qamar.quran.transliteration.model.TransliterationLanguage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class JsonTransliterationProvider(
    platformContext: Any? = null,
    private val resourceReader: ResourceReader = ResourceReader(platformContext),
) : TransliterationProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache: MutableMap<TransliterationLanguage, List<List<String>>> = mutableMapOf()

    private fun load(language: TransliterationLanguage): List<List<String>> {
        return cache.getOrPut(language) {
            val file = when (language) {
                TransliterationLanguage.ENGLISH -> "translit_en.json"
                TransliterationLanguage.RUSSIAN -> "translit_ru.json"
                TransliterationLanguage.KAZAKH -> "translit_kk.json"
            }
            json.decodeFromString(
                ListSerializer(ListSerializer(String.serializer())),
                resourceReader.readText(file),
            )
        }
    }

    override suspend fun getTransliteration(
        sura: Int,
        ayah: Int,
        language: TransliterationLanguage,
    ): String? = load(language).getOrNull(sura - 1)?.getOrNull(ayah - 1)

    override suspend fun getSuraTransliteration(
        sura: Int,
        language: TransliterationLanguage,
    ): List<String> = load(language).getOrNull(sura - 1).orEmpty()

    override suspend fun search(
        query: String,
        language: TransliterationLanguage,
    ): List<Pair<Int, Int>> {
        val lower = query.trim().lowercase()
        if (lower.isEmpty()) return emptyList()
        val data = load(language)
        val hits = mutableListOf<Pair<Int, Int>>()
        data.forEachIndexed { suraIdx, verses ->
            verses.forEachIndexed { ayahIdx, text ->
                if (text.lowercase().contains(lower)) {
                    hits += (suraIdx + 1) to (ayahIdx + 1)
                }
            }
        }
        return hits
    }
}
