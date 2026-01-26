package com.qamar.quran.search

import com.qamar.quran.api.DefaultQuranApi
import com.qamar.quran.api.QuranApi
import com.qamar.quran.api.VerseRequest
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.translations.TranslationManager
import com.qamar.quran.transliteration.TransliterationProvider
import com.qamar.quran.transliteration.model.TransliterationLanguage
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun testSearchArabic() = runBlocking {
        val results = searchEngine.searchArabic("بسم")
        
        assertTrue(results.isNotEmpty())
        assertEquals(1, mockApi.searchArabicCallCount)
        results.forEach { verse ->
            assertTrue(verse.arabicText.contains("بسم"))
        }
    }

    @Test
    fun testSearchTranslation() = runBlocking {
        val results = searchEngine.searchTranslation("test query", "translation_id")
        
        assertEquals(1, mockApi.searchTranslationCallCount)
        assertEquals("test query", mockApi.lastTranslationQuery)
        assertEquals("translation_id", mockApi.lastTranslationId)
    }

    @Test
    fun testSearchTranslationDefaultId() = runBlocking {
        val results = searchEngine.searchTranslation("test query")
        
        assertEquals(1, mockApi.searchTranslationCallCount)
        assertEquals("test query", mockApi.lastTranslationQuery)
        assertNull(mockApi.lastTranslationId)
    }

    @Test
    fun testSearchTransliteration() = runBlocking {
        val results = searchEngine.searchTransliteration("Bismillah", TransliterationLanguage.ENGLISH)
        
        assertEquals(1, mockApi.searchTransliterationCallCount)
        assertEquals("Bismillah", mockApi.lastTransliterationQuery)
        assertEquals(TransliterationLanguage.ENGLISH, mockApi.lastTransliterationLanguage)
    }

    @Test
    fun testSearchTransliterationRussian() = runBlocking {
        val results = searchEngine.searchTransliteration("test", TransliterationLanguage.RUSSIAN)
        
        assertEquals(TransliterationLanguage.RUSSIAN, mockApi.lastTransliterationLanguage)
    }

    @Test
    fun testSearchTransliterationKazakh() = runBlocking {
        val results = searchEngine.searchTransliteration("test", TransliterationLanguage.KAZAKH)
        
        assertEquals(TransliterationLanguage.KAZAKH, mockApi.lastTransliterationLanguage)
    }

    @Test
    fun testSearchDelegatesToApi() = runBlocking {
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

        override suspend fun getVerse(sura: Int, ayah: Int) = null
        override suspend fun getSura(sura: Int) = emptyList()
        override suspend fun getPage(page: Int) = emptyList()
        override suspend fun getAllVerses() = emptyList()
        override suspend fun getAyahPage(sura: Int, ayah: Int) = 1
        override suspend fun getPageStart(page: Int) = null
        override suspend fun getTranslation(sura: Int, ayah: Int, translationId: String?) = null
        override suspend fun getSuraTranslation(sura: Int, translationId: String?) = emptyList()
        override suspend fun getAvailableTranslations() = emptyList()
        override suspend fun isTranslationDownloaded(translationId: String) = false
        override suspend fun getTransliteration(sura: Int, ayah: Int, language: TransliterationLanguage) = null
        override suspend fun getSuraTransliteration(sura: Int, language: TransliterationLanguage) = emptyList()
        
        override suspend fun searchArabic(query: String): List<com.qamar.quran.core.model.Verse> {
            searchArabicCallCount++
            return listOf(
                com.qamar.quran.core.model.Verse(
                    sura = 1,
                    ayah = 1,
                    arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                )
            )
        }
        
        override suspend fun searchTranslation(query: String, translationId: String?): List<com.qamar.quran.core.model.Verse> {
            searchTranslationCallCount++
            lastTranslationQuery = query
            lastTranslationId = translationId
            return emptyList()
        }
        
        override suspend fun searchTransliteration(query: String, language: TransliterationLanguage): List<com.qamar.quran.core.model.Verse> {
            searchTransliterationCallCount++
            lastTransliterationQuery = query
            lastTransliterationLanguage = language
            return emptyList()
        }
        
        override suspend fun getVerses(requests: List<VerseRequest>) = emptyList()
        override suspend fun getVerseRange(sura: Int, startAyah: Int, endAyah: Int) = emptyList()
        override suspend fun clearCache() {}
        override suspend fun preloadSura(sura: Int) {}
        override suspend fun preloadPage(page: Int) {}
    }
}
