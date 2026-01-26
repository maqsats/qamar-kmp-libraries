package com.qamar.quran.translations

import com.qamar.quran.translations.model.DownloadProgress
import com.qamar.quran.translations.model.DownloadStatus
import com.qamar.quran.translations.model.TranslationInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Stub implementation that serves bundled metadata and tracks in-memory download state.
 * Replace the download logic with real file IO + checksum verification as needed.
 */
class DefaultTranslationManager(
    platformContext: Any? = null,
    private val metadataSource: TranslationMetadataSource = TranslationMetadataSource(platformContext),
) : TranslationManager {

    private val statuses = mutableMapOf<String, DownloadStatus>()
    private val mutex = Mutex()

    override suspend fun downloadTranslation(translationId: String): Flow<DownloadProgress> = flow {
        emit(
            DownloadProgress(
                translationId = translationId,
                bytesDownloaded = 0,
                totalBytes = 0,
                percentage = 0f,
                status = DownloadStatus.PENDING,
            )
        )
        // Stub: immediately complete.
        mutex.withLock { statuses[translationId] = DownloadStatus.COMPLETED }
        emit(
            DownloadProgress(
                translationId = translationId,
                bytesDownloaded = 0,
                totalBytes = 0,
                percentage = 100f,
                status = DownloadStatus.COMPLETED,
            )
        )
    }

    override suspend fun autoDownloadTranslation(languageCode: String): Result<TranslationInfo> {
        val match = metadataSource.loadBundled().firstOrNull { it.languageCode == languageCode }
            ?: return Result.failure(IllegalArgumentException("No translation for $languageCode"))
        downloadTranslation(match.translationId)
        return Result.success(match)
    }

    override suspend fun deleteTranslation(translationId: String): Result<Unit> {
        mutex.withLock { statuses.remove(translationId) }
        // Stub: no files to delete yet.
        return Result.success(Unit)
    }

    override suspend fun checkForUpdates(): List<TranslationInfo> = metadataSource.loadBundled()

    override suspend fun updateTranslation(translationId: String): Flow<DownloadProgress> =
        downloadTranslation(translationId)

    override suspend fun getDownloadStatus(translationId: String): DownloadStatus =
        mutex.withLock { statuses[translationId] ?: DownloadStatus.PENDING }

    override suspend fun cancelDownload(translationId: String): Boolean {
        mutex.withLock { statuses[translationId] = DownloadStatus.CANCELLED }
        return true
    }

    override suspend fun getAvailableTranslations(): List<TranslationInfo> =
        metadataSource.loadBundled()

    override suspend fun isTranslationDownloaded(translationId: String): Boolean =
        mutex.withLock { statuses[translationId] == DownloadStatus.COMPLETED }
}
