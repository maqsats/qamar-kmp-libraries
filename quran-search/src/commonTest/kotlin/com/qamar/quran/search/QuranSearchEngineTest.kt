package com.qamar.quran.search

import com.qamar.quran.api.QuranApi
import com.qamar.quran.core.model.Verse
import com.qamar.quran.test.runTest
import com.qamar.quran.translations.model.TranslationInfo
import com.qamar.quran.transliteration.model.TransliterationLanguage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuranSearchEngineTest {
    private lateinit var searchEngine: QuranSearchEngine
    private lateinit var mockApi: MockQuranApi

    @BeforeTest
    fun setup() {
        mockApi = MockQuranApi()
        searchEngine = QuranSearchEngine(mockApi)
    }

    @Test
    fun testSearchArabic() = runTest {
        val results = searchEngine.searchArabic("بسم")

        // Verify the API was called
        assertEquals(1, mockApi.searchArabicCallCount, "searchArabic should be called once")
        // Mock returns a verse, so results should not be empty
        assertTrue(results.isNotEmpty(), "Results should not be empty - mock returns a verse")
        // Verify the results contain the expected verse
        if (results.isNotEmpty()) {
            results.forEach { verse ->
                // The mock returns a verse with "بِسْمِ" which contains "بسم"
                assertTrue(
                    verse.arabicText.contains("بسم") || verse.arabicText.contains("بِسْمِ"),
                    "Verse should contain search query"
                )
            }
        }
    }

    @Test
    fun testSearchTranslation() = runTest {
        val results = searchEngine.searchTranslation("test query", "translation_id")

        assertEquals(1, mockApi.searchTranslationCallCount)
        assertEquals("test query", mockApi.lastTranslationQuery)
        assertEquals("translation_id", mockApi.lastTranslationId)
    }

    @Test
    fun testSearchTranslationDefaultId() = runTest {
        val results = searchEngine.searchTranslation("test query")

        assertEquals(1, mockApi.searchTranslationCallCount)
        assertEquals("test query", mockApi.lastTranslationQuery)
        assertNull(mockApi.lastTranslationId)
    }

    @Test
    fun testSearchTransliteration() = runTest {
        val results =
            searchEngine.searchTransliteration("Bismillah", TransliterationLanguage.ENGLISH)

        assertEquals(1, mockApi.searchTransliterationCallCount)
        assertEquals("Bismillah", mockApi.lastTransliterationQuery)
        assertEquals(TransliterationLanguage.ENGLISH, mockApi.lastTransliterationLanguage)
    }

    @Test
    fun testSearchTransliterationRussian() = runTest {
        val results = searchEngine.searchTransliteration("test", TransliterationLanguage.RUSSIAN)

        assertEquals(TransliterationLanguage.RUSSIAN, mockApi.lastTransliterationLanguage)
    }

    @Test
    fun testSearchTransliterationKazakh() = runTest {
        val results = searchEngine.searchTransliteration("test", TransliterationLanguage.KAZAKH)

        assertEquals(TransliterationLanguage.KAZAKH, mockApi.lastTransliterationLanguage)
    }

    @Test
    fun testSearchDelegatesToApi() = runTest {
        // Verify that search methods delegate to the API
        searchEngine.searchArabic("query1")
        searchEngine.searchTranslation("query2", "trans_id")
        searchEngine.searchTransliteration("query3", TransliterationLanguage.ENGLISH)

        assertEquals(1, mockApi.searchArabicCallCount)
        assertEquals(1, mockApi.searchTranslationCallCount)
        assertEquals(1, mockApi.searchTransliterationCallCount)
    }

    // Mock implementation of QuranApi for testing
    private class MockQuranApi : QuranApi {
        var searchArabicCallCount = 0
        var searchTranslationCallCount = 0
        var searchTransliterationCallCount = 0
        var lastTranslationQuery: String? = null
        var lastTranslationId: String? = null
        var lastTransliterationQuery: String? = null
        var lastTransliterationLanguage: TransliterationLanguage? = null

        override suspend fun getVerse(sura: Int, ayah: Int): Verse? = null
        override suspend fun getSura(sura: Int): List<Verse> = emptyList()
        override suspend fun getPage(page: Int): List<Verse> = emptyList()
        override suspend fun getAllVerses(): List<Verse> = emptyList()
        override suspend fun getAyahPage(sura: Int, ayah: Int): Int = 1
        override suspend fun getPageStart(page: Int): Pair<Int, Int>? = null
        override suspend fun getTranslation(sura: Int, ayah: Int, translationId: String?): String? =
            null

        override suspend fun getSuraTranslation(sura: Int, translationId: String?): List<String> =
            emptyList()

        override suspend fun getAvailableTranslations(): List<TranslationInfo> = emptyList()
        override suspend fun isTranslationDownloaded(translationId: String): Boolean = false
        override suspend fun getTransliteration(
            sura: Int,
            ayah: Int,
            language: TransliterationLanguage
        ): String? = null

        override suspend fun getSuraTransliteration(
            sura: Int,
            language: TransliterationLanguage
        ): List<String> = emptyList()

        override suspend fun searchArabic(query: String): List<Verse> {
            searchArabicCallCount++
            return listOf(
                Verse(
                    sura = 1,
                    ayah = 1,
                    arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                )
            )
        }

        override suspend fun searchTranslation(query: String, translationId: String?): List<Verse> {
            searchTranslationCallCount++
            lastTranslationQuery = query
            lastTranslationId = translationId
            return emptyList()
        }

        override suspend fun searchTransliteration(
            query: String,
            language: TransliterationLanguage
        ): List<Verse> {
            searchTransliterationCallCount++
            lastTransliterationQuery = query
            lastTransliterationLanguage = language
            return emptyList()
        }

        override suspend fun getVerses(requests: List<com.qamar.quran.api.VerseRequest>): List<Verse> =
            emptyList()

        override suspend fun getVerseRange(sura: Int, startAyah: Int, endAyah: Int): List<Verse> =
            emptyList()

        override suspend fun clearCache() {}
        override suspend fun preloadSura(sura: Int) {}
        override suspend fun preloadPage(page: Int) {}
    }
}
