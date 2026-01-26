package com.qamar.quran.translations

import com.qamar.quran.translations.model.DownloadProgress
import com.qamar.quran.translations.model.DownloadStatus
import com.qamar.quran.translations.model.TranslationInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultTranslationManagerTest {
    private lateinit var manager: DefaultTranslationManager
    private lateinit var metadataSource: TranslationMetadataSource

    @BeforeTest
    fun setup() {
        metadataSource = TranslationMetadataSource(null)
        manager = DefaultTranslationManager(null, metadataSource)
    }

    @AfterTest
    fun tearDown() {
        // Cleanup if needed
    }

    @Test
    fun testGetAvailableTranslations() = runBlocking {
        val translations = manager.getAvailableTranslations()
        assertTrue(translations.isNotEmpty(), "Should have available translations")
        translations.forEach { translation ->
            assertNotNull(translation.translationId)
            assertNotNull(translation.displayName)
            assertNotNull(translation.languageCode)
        }
    }

    @Test
    fun testDownloadTranslation() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            val progressFlow = manager.downloadTranslation(translationId)
            
            val progress = progressFlow.first()
            assertEquals(translationId, progress.translationId)
            assertEquals(DownloadStatus.PENDING, progress.status)
            
            // Wait for completion
            val finalProgress = progressFlow.first { it.status == DownloadStatus.COMPLETED }
            assertEquals(DownloadStatus.COMPLETED, finalProgress.status)
            assertEquals(100f, finalProgress.percentage)
        }
    }

    @Test
    fun testIsTranslationDownloaded() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            
            // Initially not downloaded
            assertFalse(manager.isTranslationDownloaded(translationId))
            
            // Download it
            manager.downloadTranslation(translationId).first { it.status == DownloadStatus.COMPLETED }
            
            // Now should be downloaded
            assertTrue(manager.isTranslationDownloaded(translationId))
        }
    }

    @Test
    fun testGetDownloadStatus() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            
            // Initially pending
            assertEquals(DownloadStatus.PENDING, manager.getDownloadStatus(translationId))
            
            // Download it
            manager.downloadTranslation(translationId).first { it.status == DownloadStatus.COMPLETED }
            
            // Should be completed
            assertEquals(DownloadStatus.COMPLETED, manager.getDownloadStatus(translationId))
        }
    }

    @Test
    fun testCancelDownload() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            
            // Start download
            val progressFlow = manager.downloadTranslation(translationId)
            
            // Cancel it
            val cancelled = manager.cancelDownload(translationId)
            assertTrue(cancelled)
            
            // Status should be cancelled
            assertEquals(DownloadStatus.CANCELLED, manager.getDownloadStatus(translationId))
        }
    }

    @Test
    fun testDeleteTranslation() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            
            // Download first
            manager.downloadTranslation(translationId).first { it.status == DownloadStatus.COMPLETED }
            assertTrue(manager.isTranslationDownloaded(translationId))
            
            // Delete it
            val result = manager.deleteTranslation(translationId)
            assertTrue(result.isSuccess)
            
            // Should no longer be downloaded
            assertFalse(manager.isTranslationDownloaded(translationId))
        }
    }

    @Test
    fun testUpdateTranslation() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            
            val progressFlow = manager.updateTranslation(translationId)
            val progress = progressFlow.first()
            
            assertEquals(translationId, progress.translationId)
            // Should complete successfully
            val finalProgress = progressFlow.first { it.status == DownloadStatus.COMPLETED }
            assertEquals(DownloadStatus.COMPLETED, finalProgress.status)
        }
    }

    @Test
    fun testCheckForUpdates() = runBlocking {
        val updates = manager.checkForUpdates()
        // Should return list of translations (may be empty)
        assertNotNull(updates)
    }

    @Test
    fun testAutoDownloadTranslation() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val languageCode = translations.first().languageCode
            val result = manager.autoDownloadTranslation(languageCode)
            
            if (result.isSuccess) {
                val translation = result.getOrNull()
                assertNotNull(translation)
                assertEquals(languageCode, translation.languageCode)
                assertTrue(manager.isTranslationDownloaded(translation.translationId))
            }
        }
    }

    @Test
    fun testAutoDownloadTranslationInvalidLanguage() = runBlocking {
        val result = manager.autoDownloadTranslation("invalid_lang_code")
        assertTrue(result.isFailure)
    }

    @Test
    fun testDownloadProgressFlow() = runBlocking {
        val translations = manager.getAvailableTranslations()
        if (translations.isNotEmpty()) {
            val translationId = translations.first().translationId
            val progressFlow = manager.downloadTranslation(translationId)
            
            var pendingReceived = false
            var completedReceived = false
            
            progressFlow.collect { progress ->
                when (progress.status) {
                    DownloadStatus.PENDING -> {
                        pendingReceived = true
                        assertEquals(0f, progress.percentage)
                    }
                    DownloadStatus.COMPLETED -> {
                        completedReceived = true
                        assertEquals(100f, progress.percentage)
                    }
                    else -> {}
                }
            }
            
            assertTrue(pendingReceived, "Should receive PENDING status")
            assertTrue(completedReceived, "Should receive COMPLETED status")
        }
    }
}
