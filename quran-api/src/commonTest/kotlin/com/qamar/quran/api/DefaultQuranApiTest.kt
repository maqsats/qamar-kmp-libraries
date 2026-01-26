package com.qamar.quran.api

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.translations.TranslationManager
import com.qamar.quran.translations.model.TranslationInfo
import com.qamar.quran.transliteration.TransliterationProvider
import com.qamar.quran.transliteration.model.TransliterationLanguage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultQuranApiTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: QuranDatabase
    private lateinit var api: DefaultQuranApi
    private var mockTranslationManager: MockTranslationManager? = null
    private var mockTransliterationProvider: MockTransliterationProvider? = null

    @BeforeTest
    fun setup() = runTest {
        driver = createTestDriver()
        database = QuranDatabase(driver)
        seedTestData()
        mockTranslationManager = MockTranslationManager()
        mockTransliterationProvider = MockTransliterationProvider()
        api = DefaultQuranApi(
            database = database,
            translationManager = mockTranslationManager,
            transliterationProvider = mockTransliterationProvider,
            cacheSize = 10
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    // Verse Operations Tests
    @Test
    fun testGetVerse() = runTest {
        val verse = api.getVerse(1, 1)
        assertNotNull(verse)
        assertEquals(1, verse.sura)
        assertEquals(1, verse.ayah)
        assertEquals("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", verse.arabicText)
    }

    @Test
    fun testGetVerseNotFound() = runTest {
        val verse = api.getVerse(999, 999)
        assertNull(verse)
    }

    @Test
    fun testGetSura() = runTest {
        val verses = api.getSura(1)
        assertTrue(verses.isNotEmpty())
        assertEquals(7, verses.size) // Al-Fatiha has 7 verses
        verses.forEach { verse ->
            assertEquals(1, verse.sura)
        }
        // Check ordering
        verses.forEachIndexed { index, verse ->
            assertEquals(index + 1, verse.ayah)
        }
    }

    @Test
    fun testGetPage() = runTest {
        val verses = api.getPage(1)
        assertTrue(verses.isNotEmpty())
        verses.forEach { verse ->
            assertEquals(1, verse.page)
        }
    }

    @Test
    fun testGetAllVerses() = runTest {
        val verses = api.getAllVerses()
        assertTrue(verses.isNotEmpty())
        // Should have all verses from test data
        assertTrue(verses.size >= 10)
    }

    // Page Operations Tests
    @Test
    fun testGetAyahPage() = runTest {
        val page = api.getAyahPage(1, 1)
        assertEquals(1, page)
    }

    @Test
    fun testGetPageStart() = runTest {
        val start = api.getPageStart(1)
        assertNotNull(start)
        assertEquals(1, start.first) // sura
        assertEquals(1, start.second) // ayah
    }

    @Test
    fun testGetPageStartNotFound() = runTest {
        val start = api.getPageStart(999)
        assertNull(start)
    }

    // Translation Operations Tests
    @Test
    fun testGetAvailableTranslations() = runTest {
        val translations = api.getAvailableTranslations()
        assertTrue(translations.isNotEmpty())
        assertEquals(2, translations.size) // Based on mock data
    }

    @Test
    fun testIsTranslationDownloaded() = runTest {
        val isDownloaded = api.isTranslationDownloaded("test_translation_1")
        assertTrue(isDownloaded)
        
        val notDownloaded = api.isTranslationDownloaded("non_existent")
        assertTrue(!notDownloaded)
    }

    @Test
    fun testGetTranslation() = runTest {
        // This returns null in current implementation (TODO)
        val translation = api.getTranslation(1, 1, "test_translation_1")
        // Currently returns null as per implementation
    }

    @Test
    fun testGetSuraTranslation() = runTest {
        // This returns empty list in current implementation (TODO)
        val translations = api.getSuraTranslation(1, "test_translation_1")
        assertTrue(translations.isEmpty())
    }

    // Transliteration Operations Tests
    @Test
    fun testGetTransliteration() = runTest {
        val transliteration = api.getTransliteration(1, 1, TransliterationLanguage.ENGLISH)
        assertNotNull(transliteration)
        assertEquals("Bismillahi ar-Rahmani ar-Raheem", transliteration)
    }

    @Test
    fun testGetSuraTransliteration() = runTest {
        val transliterations = api.getSuraTransliteration(1, TransliterationLanguage.ENGLISH)
        assertTrue(transliterations.isNotEmpty())
        assertEquals(7, transliterations.size) // Al-Fatiha has 7 verses
    }

    // Search Operations Tests
    @Test
    fun testSearchArabic() = runTest {
        val results = api.searchArabic("بسم")
        assertTrue(results.isNotEmpty())
        results.forEach { verse ->
            assertTrue(verse.arabicText.contains("بسم"))
        }
    }

    @Test
    fun testSearchArabicEmptyQuery() = runTest {
        val results = api.searchArabic("")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSearchArabicBlankQuery() = runTest {
        val results = api.searchArabic("   ")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSearchTransliteration() = runTest {
        val results = api.searchTransliteration("Bismillah", TransliterationLanguage.ENGLISH)
        assertTrue(results.isNotEmpty())
        // Results should contain verses with matching transliteration
    }

    @Test
    fun testSearchTransliterationEmptyQuery() = runTest {
        val results = api.searchTransliteration("", TransliterationLanguage.ENGLISH)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSearchTranslation() = runTest {
        // Currently returns empty list (TODO)
        val results = api.searchTranslation("test", "test_translation_1")
        assertTrue(results.isEmpty())
    }

    // Batch Operations Tests
    @Test
    fun testGetVerses() = runTest {
        val requests = listOf(
            VerseRequest(1, 1),
            VerseRequest(1, 2),
            VerseRequest(2, 1)
        )
        val verses = api.getVerses(requests)
        assertEquals(3, verses.size)
        assertEquals(1, verses[0].sura)
        assertEquals(1, verses[0].ayah)
        assertEquals(1, verses[1].sura)
        assertEquals(2, verses[1].ayah)
        assertEquals(2, verses[2].sura)
        assertEquals(1, verses[2].ayah)
    }

    @Test
    fun testGetVerseRange() = runTest {
        val verses = api.getVerseRange(1, 1, 3)
        assertEquals(3, verses.size)
        verses.forEachIndexed { index, verse ->
            assertEquals(1, verse.sura)
            assertEquals(index + 1, verse.ayah)
        }
    }

    @Test
    fun testGetVerseRangeSingleAyah() = runTest {
        val verses = api.getVerseRange(1, 1, 1)
        assertEquals(1, verses.size)
        assertEquals(1, verses[0].ayah)
    }

    // Caching Tests
    @Test
    fun testCache() = runTest {
        // First call should hit database
        val verse1 = api.getVerse(1, 1)
        assertNotNull(verse1)
        
        // Second call should use cache
        val verse2 = api.getVerse(1, 1)
        assertNotNull(verse2)
        assertEquals(verse1.arabicText, verse2.arabicText)
    }

    @Test
    fun testClearCache() = runTest {
        // Load some verses to populate cache
        api.getVerse(1, 1)
        api.getVerse(1, 2)
        
        // Clear cache
        api.clearCache()
        
        // Cache should be empty, but verses should still be retrievable
        val verse = api.getVerse(1, 1)
        assertNotNull(verse)
    }

    @Test
    fun testPreloadSura() = runTest {
        // Preload should not throw
        api.preloadSura(1)
        
        // Verify verses are accessible
        val verse = api.getVerse(1, 1)
        assertNotNull(verse)
    }

    @Test
    fun testPreloadPage() = runTest {
        // Preload should not throw
        api.preloadPage(1)
        
        // Verify verses are accessible
        val page = api.getPage(1)
        assertTrue(page.isNotEmpty())
    }

    // Helper functions
    private suspend fun createTestDriver(): SqlDriver {
        // Driver creation and schema setup is handled in platform-specific implementations
        return createInMemoryDriver()
    }

    private suspend fun seedTestData() {
        // Insert test verses
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 1L, arabic_text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 2L, arabic_text = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ")
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 3L, arabic_text = "الرَّحْمَٰنِ الرَّحِيمِ")
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 4L, arabic_text = "مَالِكِ يَوْمِ الدِّينِ")
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 5L, arabic_text = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ")
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 6L, arabic_text = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ")
        database.quranDatabaseQueries.insertVerse(sura = 1L, ayah = 7L, arabic_text = "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ")
        
        database.quranDatabaseQueries.insertVerse(sura = 2L, ayah = 1L, arabic_text = "الم")
        database.quranDatabaseQueries.insertVerse(sura = 2L, ayah = 2L, arabic_text = "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ")
        database.quranDatabaseQueries.insertVerse(sura = 2L, ayah = 3L, arabic_text = "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ")

        // Insert page mappings
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 1L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 2L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 3L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 4L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 5L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 6L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 1L, ayah = 7L)
        database.quranDatabaseQueries.insertPage(page = 1L, sura = 2L, ayah = 1L)
    }

    // Mock classes
    private class MockTranslationManager : TranslationManager {
        private val translations = listOf(
            TranslationInfo(
                id = 1,
                displayName = "Test Translation 1",
                translator = "Test Translator",
                languageCode = "en",
                fileUrl = "http://example.com/trans1.db",
                fileName = "test_translation_1.db"
            ),
            TranslationInfo(
                id = 2,
                displayName = "Test Translation 2",
                translator = "Test Translator 2",
                languageCode = "ar",
                fileUrl = "http://example.com/trans2.db",
                fileName = "test_translation_2.db"
            )
        )
        
        private val downloaded = mutableSetOf("test_translation_1")

        override suspend fun downloadTranslation(translationId: String) = kotlinx.coroutines.flow.flow {
            // Stub implementation
        }

        override suspend fun autoDownloadTranslation(languageCode: String) = 
            Result.failure(NotImplementedError())

        override suspend fun deleteTranslation(translationId: String) = Result.success(Unit)

        override suspend fun checkForUpdates() = emptyList<TranslationInfo>()

        override suspend fun updateTranslation(translationId: String) = kotlinx.coroutines.flow.flow {
            // Stub implementation
        }

        override suspend fun getDownloadStatus(translationId: String) = 
            com.qamar.quran.translations.model.DownloadStatus.PENDING

        override suspend fun cancelDownload(translationId: String) = true

        override suspend fun getAvailableTranslations() = translations

        override suspend fun isTranslationDownloaded(translationId: String) = 
            downloaded.contains(translationId)
    }

    private class MockTransliterationProvider : TransliterationProvider {
        private val transliterations = mapOf(
            TransliterationLanguage.ENGLISH to mapOf(
                1 to mapOf(
                    1 to "Bismillahi ar-Rahmani ar-Raheem",
                    2 to "Al-hamdu lillahi rabbil alameen",
                    3 to "Ar-Rahmani ar-Raheem",
                    4 to "Maliki yawmi ad-deen",
                    5 to "Iyyaka na'budu wa iyyaka nasta'een",
                    6 to "Ihdina as-sirata al-mustaqeem",
                    7 to "Sirata alladhina an'amta alayhim ghayri al-maghdubi alayhim wa la ad-dalleen"
                )
            )
        )

        override suspend fun getTransliteration(
            sura: Int,
            ayah: Int,
            language: TransliterationLanguage
        ): String? {
            return transliterations[language]?.get(sura)?.get(ayah)
        }

        override suspend fun getSuraTransliteration(
            sura: Int,
            language: TransliterationLanguage
        ): List<String> {
            return transliterations[language]?.get(sura)?.values?.toList() ?: emptyList()
        }

        override suspend fun search(
            query: String,
            language: TransliterationLanguage
        ): List<Pair<Int, Int>> {
            val lower = query.trim().lowercase()
            if (lower.isEmpty()) return emptyList()
            
            val results = mutableListOf<Pair<Int, Int>>()
            transliterations[language]?.forEach { (sura, verses) ->
                verses.forEach { (ayah, text) ->
                    if (text.lowercase().contains(lower)) {
                        results.add(sura to ayah)
                    }
                }
            }
            return results
        }
    }

    // Helper to run suspend tests
    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }
}

// Platform-specific driver creation
expect fun createInMemoryDriver(): SqlDriver
