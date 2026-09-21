package com.qamar.quran.translations

import com.qamar.quran.translations.model.TranslationInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The hosted catalog is fetched at runtime and can be replaced by a deploy without
 * any app release, so these are the tests that stand between a bad upload and a
 * broken translation list on every phone.
 */
class TranslationCatalogTest {

    private val source = TranslationMetadataSource(null)

    @BeforeTest
    fun setup() = TranslationCatalogCache.clear()

    @AfterTest
    fun tearDown() = TranslationCatalogCache.clear()

    private fun entry(
        id: Int = 1,
        displayName: String = "Russian Translation",
        languageCode: String = "ru",
        fileUrl: String = "https://example.test/quran.ru.kalam.db",
        fileName: String = "quran.ru.kalam.db",
    ) = TranslationInfo(
        id = id,
        displayName = displayName,
        languageCode = languageCode,
        fileUrl = fileUrl,
        fileName = fileName,
    )

    // ── what we are willing to run on ──────────────────────────────────────

    @Test
    fun completeCatalogIsAccepted() {
        assertTrue(TranslationMetadataSource.isUsable(listOf(entry())))
    }

    @Test
    fun emptyCatalogIsRejected() {
        // A truncated or half-written deploy must never be able to empty the list.
        assertFalse(TranslationMetadataSource.isUsable(emptyList()))
    }

    @Test
    fun entryWithoutADownloadUrlIsRejected() {
        // One unusable row poisons the whole document rather than stranding a user
        // on a translation that can never finish downloading.
        assertFalse(TranslationMetadataSource.isUsable(listOf(entry(), entry(fileUrl = ""))))
    }

    @Test
    fun entryWithoutADatabaseFileNameIsRejected() {
        // translationId is derived from fileName, so a row that is not "*.db"
        // cannot be addressed at all.
        assertFalse(TranslationMetadataSource.isUsable(listOf(entry(fileName = "quran.ru.kalam"))))
    }

    @Test
    fun entryWithoutALanguageOrNameIsRejected() {
        assertFalse(TranslationMetadataSource.isUsable(listOf(entry(languageCode = ""))))
        assertFalse(TranslationMetadataSource.isUsable(listOf(entry(displayName = ""))))
    }

    @Test
    fun aSmallerCatalogIsStillAccepted() {
        // Retiring an edition legitimately shrinks the list, so size alone must not
        // be treated as corruption.
        assertTrue(TranslationMetadataSource.isUsable(listOf(entry())))
    }

    // ── parsing ────────────────────────────────────────────────────────────

    @Test
    fun parsesTheQuranComWrapperShape() {
        val parsed = source.parse(
            """{"data":[{"id":81,"displayName":"Русский (Russian Tr.)","translator":"Калям Шариф",
               "languageCode":"ru","fileUrl":"https://example.test/x.db","fileName":"quran.ru.kalam.db"}]}"""
        )
        assertEquals(1, parsed.size)
        assertEquals("quran.ru.kalam", parsed.single().translationId)
        assertEquals("ru", parsed.single().languageCode)
    }

    @Test
    fun ignoresUnknownFields() {
        // The hosted document may gain fields before the app knows about them; that
        // must not make the whole catalog unreadable.
        val parsed = source.parse(
            """{"data":[{"id":81,"displayName":"x","languageCode":"ru",
               "fileUrl":"https://example.test/x.db","fileName":"quran.ru.kalam.db",
               "somethingAddedLater":true}]}"""
        )
        assertEquals(1, parsed.size)
    }

    // ── the bundled floor ──────────────────────────────────────────────────

    @Test
    fun bundledCatalogIsItselfUsable() {
        // The fallback has to satisfy the same bar we hold the hosted copy to,
        // otherwise the app could ship a catalog it would have rejected remotely.
        val bundled = source.loadBundled()
        assertTrue(TranslationMetadataSource.isUsable(bundled), "bundled catalog is not usable")
    }

    @Test
    fun bundledCatalogHasUniqueTranslationIds() {
        val bundled = source.loadBundled()
        val ids = bundled.map { it.translationId }
        assertEquals(ids.size, ids.toSet().size, "duplicate translationId in the bundled catalog")
    }

    // ── the process-wide cache ─────────────────────────────────────────────

    @Test
    fun cacheStartsEmptyAndStale() {
        assertNull(TranslationCatalogCache.entries)
        assertTrue(TranslationCatalogCache.isStale, "an empty cache must read as stale")
    }

    @Test
    fun cachedValueIsServedUntilItsTtlExpires() {
        val entries = listOf(entry())
        TranslationCatalogCache.put(entries, TranslationCatalogCache.HostedTtl)
        assertEquals(entries, TranslationCatalogCache.entries)
        assertFalse(TranslationCatalogCache.isStale)
    }

    @Test
    fun aZeroTtlEntryIsImmediatelyStale() {
        TranslationCatalogCache.put(listOf(entry()), kotlin.time.Duration.ZERO)
        assertTrue(TranslationCatalogCache.isStale, "a zero TTL must not be served")
    }

    @Test
    fun aFallbackIsRetriedSoonerThanAGoodFetch() {
        // The whole point of two TTLs: recovering from offline must not wait as
        // long as a healthy refresh does.
        assertTrue(
            TranslationCatalogCache.RetryTtl < TranslationCatalogCache.HostedTtl,
            "fallback TTL must be shorter than the hosted TTL",
        )
    }

    @Test
    fun clearForgetsEverything() {
        TranslationCatalogCache.put(listOf(entry()), TranslationCatalogCache.HostedTtl)
        TranslationCatalogCache.clear()
        assertNull(TranslationCatalogCache.entries)
        assertTrue(TranslationCatalogCache.isStale)
    }

    // ── the storage key must never collide with a real translation ─────────

    @Test
    fun catalogCacheKeyCannotCollideWithATranslationId() {
        val bundled = source.loadBundled()
        assertFalse(
            bundled.any { it.translationId == TranslationCatalogCache.Key },
            "the catalog cache key collides with a shipping translationId",
        )
    }
}
