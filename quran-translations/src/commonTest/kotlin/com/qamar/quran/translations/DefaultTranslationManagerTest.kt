package com.qamar.quran.translations

import com.qamar.quran.test.runTest
import com.qamar.quran.translations.model.DownloadStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first

/**
 * Behaviour of the manager against the catalog that ships inside the artifact.
 *
 * `remoteCatalogUrl = null` throughout: these assertions are about the bundled
 * catalog and must not depend on the network. The hosted lookup, its fallbacks
 * and the download flow are covered in [HostedCatalogTest], which drives them
 * from a MockEngine.
 *
 * Note the seven tests that used to live here drove real downloads from
 * android.quran.com and asserted a `PENDING` first emission the implementation
 * has never produced; they failed on every run. What they were reaching for is
 * covered deterministically here and in [HostedCatalogTest].
 */
class DefaultTranslationManagerTest {
    private lateinit var manager: DefaultTranslationManager
    private lateinit var metadataSource: TranslationMetadataSource

    @BeforeTest
    fun setup() {
        // The resolved catalog is cached process-wide, so it is cleared here -
        // otherwise one test is answered from another test's lookup.
        TranslationCatalogCache.clear()
        metadataSource = TranslationMetadataSource(null)
        manager = DefaultTranslationManager(null, metadataSource, remoteCatalogUrl = null)
    }

    @AfterTest
    fun tearDown() {
        TranslationCatalogCache.clear()
    }

    @Test
    fun availableTranslationsComeFromTheBundledCatalog() = runTest {
        val translations = manager.getAvailableTranslations()
        assertTrue(translations.isNotEmpty(), "bundled catalog must not be empty")
        translations.forEach {
            assertTrue(it.translationId.isNotBlank())
            assertTrue(it.displayName.isNotBlank())
            assertTrue(it.languageCode.isNotBlank())
        }
    }

    @Test
    fun checkForUpdatesReturnsTheSameCatalog() = runTest {
        assertEquals(manager.getAvailableTranslations(), manager.checkForUpdates())
    }

    @Test
    fun autoDownloadResolvesAKnownLanguage() = runTest {
        val language = manager.getAvailableTranslations().first().languageCode
        val result = manager.autoDownloadTranslation(language)
        assertTrue(result.isSuccess)
        assertEquals(language, result.getOrNull()?.languageCode)
    }

    @Test
    fun autoDownloadRejectsAnUnknownLanguage() = runTest {
        assertTrue(manager.autoDownloadTranslation("invalid_lang_code").isFailure)
    }

    @Test
    fun downloadingAnIdThatIsNotInTheCatalogFails() = runTest {
        // The id is resolved against the catalog before any request is made, so
        // this never touches the network.
        val progress = manager.downloadTranslation("quran.not.a.real.edition").first()
        assertEquals(DownloadStatus.FAILED, progress.status)
        assertEquals("quran.not.a.real.edition", progress.translationId)
    }

    @Test
    fun anUntouchedTranslationIsPendingAndNotDownloaded() = runTest {
        val id = "quran.not.a.real.edition"
        assertEquals(DownloadStatus.PENDING, manager.getDownloadStatus(id))
        assertFalse(manager.isTranslationDownloaded(id))
    }

    @Test
    fun cancelDownloadMarksTheTranslationCancelled() = runTest {
        val id = manager.getAvailableTranslations().first().translationId
        assertTrue(manager.cancelDownload(id))
        assertEquals(DownloadStatus.CANCELLED, manager.getDownloadStatus(id))
    }

    @Test
    fun deletingATranslationThatWasNeverDownloadedSucceeds() = runTest {
        val result = manager.deleteTranslation("quran.not.a.real.edition")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }
}
