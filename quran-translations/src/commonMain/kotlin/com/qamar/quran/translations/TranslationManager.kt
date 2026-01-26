package com.qamar.quran.translations

import com.qamar.quran.translations.model.DownloadProgress
import com.qamar.quran.translations.model.DownloadStatus
import com.qamar.quran.translations.model.TranslationInfo
import kotlinx.coroutines.flow.Flow

interface TranslationManager {
    suspend fun downloadTranslation(translationId: String): Flow<DownloadProgress>
    suspend fun autoDownloadTranslation(languageCode: String): Result<TranslationInfo>
    suspend fun deleteTranslation(translationId: String): Result<Unit>
    suspend fun checkForUpdates(): List<TranslationInfo>
    suspend fun updateTranslation(translationId: String): Flow<DownloadProgress>
    suspend fun getDownloadStatus(translationId: String): DownloadStatus
    suspend fun cancelDownload(translationId: String): Boolean
    suspend fun getAvailableTranslations(): List<TranslationInfo>
    suspend fun isTranslationDownloaded(translationId: String): Boolean
}
