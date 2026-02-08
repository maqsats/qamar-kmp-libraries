package com.qamar.quran.sample

import com.qamar.quran.api.DefaultQuranApi
import com.qamar.quran.api.QuranApi
import com.qamar.quran.api.VerseRequest
import com.qamar.quran.core.database.DatabaseConfig
import com.qamar.quran.core.database.DatabaseFactory
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.search.QuranSearchEngine
import com.qamar.quran.translations.TranslationMetadataSource
import com.qamar.quran.transliteration.JsonTransliterationProvider
import com.qamar.quran.transliteration.model.TransliterationLanguage

/**
 * Comprehensive sample demonstrating all functionality of the Qamar KMP Libraries.
 *
 * This sample shows how to:
 * - Initialize the Quran API with database, translations, and transliteration
 * - Retrieve verses by sura/ayah, page, and ranges
 * - Search Arabic text, translations, and transliterations
 * - Use batch operations for multiple verses
 * - Work with translations and transliterations
 * - Use caching and preloading features
 */
class QuranApiSample {
    private lateinit var api: QuranApi
    private lateinit var searchEngine: QuranSearchEngine

    /**
     * Initialize the Quran API with all required components.
     *
     * @param platformContext Platform-specific context (Android Context, iOS Bundle, etc.)
     */
    suspend fun initialize(platformContext: Any? = null) {
        println("=== Initializing Quran API ===")

        // Create database driver
        val databaseFactory = DatabaseFactory(platformContext)
        val driver = databaseFactory.createDriver(
            config = DatabaseConfig(
                name = "quran.db",
                copyBundledIfMissing = true
            )
        )
        val database = QuranDatabase(driver)

        // Create transliteration provider
        val transliterationProvider = JsonTransliterationProvider(platformContext)

        // Create API instance
        api = DefaultQuranApi(
            database = database,
            translationManager = null, // Can be wired in when translation downloads are implemented
            transliterationProvider = transliterationProvider,
            cacheSize = 256
        )

        // Create search engine
        searchEngine = QuranSearchEngine(api)

        println("✓ API initialized successfully\n")
    }

    /**
     * Demonstrate verse retrieval operations.
     */
    suspend fun demonstrateVerseOperations() {
        println("=== Verse Operations ===")

        // Get a single verse
        println("\n1. Get single verse (Sura 1, Ayah 1):")
        val verse1 = api.getVerse(1, 1)
        verse1?.let {
            println("   Sura: ${it.sura}, Ayah: ${it.ayah}")
            println("   Arabic: ${it.arabicText}")
            println("   Page: ${it.page}")
        } ?: println("   Verse not found")

        // Get all verses in a sura
        println("\n2. Get all verses in Sura 1 (Al-Fatiha):")
        val sura1 = api.getSura(1)
        println("   Found ${sura1.size} verses")
        sura1.take(3).forEach { verse ->
            println("   Ayah ${verse.ayah}: ${verse.arabicText.take(50)}...")
        }
        if (sura1.size > 3) {
            println("   ... and ${sura1.size - 3} more verses")
        }

        // Get all verses
        println("\n3. Get all verses:")
        val allVerses = api.getAllVerses()
        println("   Total verses: ${allVerses.size}")

        println("\n✓ Verse operations completed\n")
    }

    /**
     * Demonstrate page operations.
     */
    suspend fun demonstratePageOperations() {
        println("=== Page Operations ===")

        // Get page content
        println("\n1. Get verses on page 1:")
        val page1 = api.getPage(1)
        println("   Found ${page1.size} verses on page 1")
        page1.take(3).forEach { verse ->
            println("   Sura ${verse.sura}, Ayah ${verse.ayah}: ${verse.arabicText.take(40)}...")
        }

        // Get page number for a specific ayah
        println("\n2. Get page number for Sura 1, Ayah 1:")
        val page = api.getAyahPage(1, 1)
        println("   Page: $page")

        // Get page start
        println("\n3. Get starting verse for page 1:")
        val pageStart = api.getPageStart(1)
        pageStart?.let { (sura, ayah) ->
            println("   Starts at Sura $sura, Ayah $ayah")
        }

        println("\n✓ Page operations completed\n")
    }

    /**
     * Demonstrate transliteration operations.
     */
    suspend fun demonstrateTransliterationOperations() {
        println("=== Transliteration Operations ===")

        // Get transliteration for a verse
        println("\n1. Get transliteration (Sura 1, Ayah 1) in English:")
        val transliteration = api.getTransliteration(1, 1, TransliterationLanguage.ENGLISH)
        transliteration?.let {
            println("   $it")
        } ?: println("   Transliteration not available")

        // Get transliteration for a sura
        println("\n2. Get transliteration for Sura 1 in English:")
        val suraTransliteration = api.getSuraTransliteration(1, TransliterationLanguage.ENGLISH)
        println("   Found ${suraTransliteration.size} transliterations")
        suraTransliteration.take(3).forEachIndexed { index, text ->
            println("   Ayah ${index + 1}: $text")
        }

        // Try different languages
        println("\n3. Get transliteration in Russian:")
        val russianTranslit = api.getTransliteration(1, 1, TransliterationLanguage.RUSSIAN)
        russianTranslit?.let {
            println("   $it")
        }

        println("\n4. Get transliteration in Kazakh:")
        val kazakhTranslit = api.getTransliteration(1, 1, TransliterationLanguage.KAZAKH)
        kazakhTranslit?.let {
            println("   $it")
        }

        println("\n✓ Transliteration operations completed\n")
    }

    /**
     * Demonstrate translation operations.
     */
    fun demonstrateTranslationOperations() {
        println("=== Translation Operations ===")

        // Get available translations
        println("\n1. Get available translations:")
        val metadata = TranslationMetadataSource(platformContext = null).loadBundled()
        if (metadata.isEmpty()) {
            println("   No bundled translation metadata found")
            return
        }
        val first = metadata.first()
        println("   Found ${metadata.size} entries, using first: ${first.displayName} (${first.languageCode})")

        println("   Download/reading not supported on this platform in the sample build.")
    }

    /**
     * Demonstrate search operations.
     */
    suspend fun demonstrateSearchOperations() {
        println("=== Search Operations ===")

        // Search Arabic text
        println("\n1. Search Arabic text 'بسم':")
        val arabicResults = api.searchArabic("بسم")
        println("   Found ${arabicResults.size} results")
        arabicResults.take(5).forEach { verse ->
            println("   Sura ${verse.sura}, Ayah ${verse.ayah}: ${verse.arabicText.take(50)}...")
        }

        // Search transliteration
        println("\n2. Search transliteration 'Bismillah' in English:")
        val translitResults =
            api.searchTransliteration("Bismillah", TransliterationLanguage.ENGLISH)
        println("   Found ${translitResults.size} results")
        translitResults.take(5).forEach { verse ->
            println("   Sura ${verse.sura}, Ayah ${verse.ayah}: ${verse.arabicText.take(50)}...")
        }

        // Search using search engine
        println("\n3. Search using QuranSearchEngine:")
        val searchResults = searchEngine.searchArabic("الرحمن")
        println("   Found ${searchResults.size} results")
        searchResults.take(3).forEach { verse ->
            println("   Sura ${verse.sura}, Ayah ${verse.ayah}: ${verse.arabicText.take(50)}...")
        }

        // Search translation (when implemented)
        println("\n4. Search translation:")
        val translationResults = api.searchTranslation("mercy", null)
        if (translationResults.isEmpty()) {
            println("   Translation search not yet implemented")
        } else {
            println("   Found ${translationResults.size} results")
        }

        println("\n✓ Search operations completed\n")
    }

    /**
     * Demonstrate batch operations.
     */
    suspend fun demonstrateBatchOperations() {
        println("=== Batch Operations ===")

        // Get multiple verses at once
        println("\n1. Get multiple verses in batch:")
        val requests = listOf(
            VerseRequest(1, 1),
            VerseRequest(1, 2),
            VerseRequest(1, 3),
            VerseRequest(2, 1),
            VerseRequest(2, 2)
        )
        val verses = api.getVerses(requests)
        println("   Requested ${requests.size} verses, got ${verses.size} results")
        verses.forEach { verse ->
            println("   Sura ${verse.sura}, Ayah ${verse.ayah}: ${verse.arabicText.take(40)}...")
        }

        // Get verse range
        println("\n2. Get verse range (Sura 1, Ayah 1-3):")
        val verseRange = api.getVerseRange(1, 1, 3)
        println("   Found ${verseRange.size} verses")
        verseRange.forEach { verse ->
            println("   Ayah ${verse.ayah}: ${verse.arabicText.take(50)}...")
        }

        println("\n✓ Batch operations completed\n")
    }

    /**
     * Demonstrate caching and preloading operations.
     */
    suspend fun demonstrateCachingOperations() {
        println("=== Caching Operations ===")

        // Preload a sura
        println("\n1. Preload Sura 1:")
        api.preloadSura(1)
        println("   ✓ Sura 1 preloaded into cache")

        // Preload a page
        println("\n2. Preload page 1:")
        api.preloadPage(1)
        println("   ✓ Page 1 preloaded into cache")

        // Access cached verses
        println("\n3. Access cached verses:")
        val cachedVerse1 = api.getVerse(1, 1)
        val cachedVerse2 = api.getVerse(1, 1) // Should use cache
        println("   First call: ${cachedVerse1?.arabicText?.take(30)}...")
        println("   Second call (cached): ${cachedVerse2?.arabicText?.take(30)}...")

        // Clear cache
        println("\n4. Clear cache:")
        api.clearCache()
        println("   ✓ Cache cleared")

        // Verify cache is cleared (verse should still be retrievable from DB)
        val verseAfterClear = api.getVerse(1, 1)
        println("   Verse after cache clear: ${verseAfterClear?.arabicText?.take(30)}...")

        println("\n✓ Caching operations completed\n")
    }

    /**
     * Run all demonstrations.
     */
    suspend fun runAll() {
        println("\n" + "=".repeat(60))
        println("QAMAR KMP LIBRARIES - COMPREHENSIVE FUNCTIONALITY SAMPLE")
        println("=".repeat(60) + "\n")

        try {
            initialize()
            demonstrateVerseOperations()
            demonstratePageOperations()
            demonstrateTransliterationOperations()
            demonstrateTranslationOperations()
            demonstrateSearchOperations()
            demonstrateBatchOperations()
            demonstrateCachingOperations()

            println("=".repeat(60))
            println("✓ ALL SAMPLES COMPLETED SUCCESSFULLY")
            println("=".repeat(60) + "\n")
        } catch (e: Exception) {
            println("\n❌ Error running samples: ${e.message}")
            e.printStackTrace()
        }
    }
}
