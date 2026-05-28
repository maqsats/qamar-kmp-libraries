package com.qamar.quran.translations

import com.qamar.quran.translations.database.TranslationDatabaseHelper
import com.qamar.quran.translations.database.getTranslationText
import com.qamar.quran.translations.database.searchTranslation
import com.qamar.quran.translations.model.DownloadProgress
import com.qamar.quran.translations.model.DownloadStatus
import com.qamar.quran.translations.model.TranslationInfo
import com.qamar.quran.translations.model.TranslationSearchHit
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultTranslationManager(
    platformContext: Any? = null,
    private val metadataSource: TranslationMetadataSource = TranslationMetadataSource(platformContext),
) : TranslationManager {

    private val dbHelper = TranslationDatabaseHelper(platformContext)
    private val httpClient = HttpClient {
        install(HttpRedirect) {
            checkHttpMethod = false
            allowHttpsDowngrade = true
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 60_000
        }
    }
    private val mutex = Mutex()
    private val statuses = mutableMapOf<String, DownloadStatus>()

    private fun looksLikeSqlite(bytes: ByteArray): Boolean {
        if (bytes.size < 16) return false
        val header = "SQLite format 3".encodeToByteArray()
        for (i in header.indices) {
            if (bytes[i] != header[i]) return false
        }
        return true
    }

    override suspend fun downloadTranslation(translationId: String): Flow<DownloadProgress> = flow {
        val info = getAvailableTranslations().firstOrNull { it.translationId == translationId }
        if (info == null) {
            emit(DownloadProgress(translationId, 0, 0, 0f, DownloadStatus.FAILED))
            return@flow
        }

        mutex.withLock { statuses[translationId] = DownloadStatus.DOWNLOADING }
        emit(DownloadProgress(translationId, 0, 0, 0f, DownloadStatus.DOWNLOADING))

        try {
            val url = info.fileUrl
            val response: HttpResponse = httpClient.get(url)
            val channel: ByteReadChannel = response.body()

            val totalBytes = response.headers["Content-Length"]?.toLong() ?: 0L
            var bytesDownloaded = 0L

            val accumulator = ArrayList<Byte>()
            val buffer = ByteArray(8192)
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead > 0) {
                    for (i in 0 until bytesRead) accumulator.add(buffer[i])
                    bytesDownloaded += bytesRead

                    val percentage = if (totalBytes > 0) {
                        (bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100f
                    } else 0f

                    emit(
                        DownloadProgress(
                            translationId,
                            bytesDownloaded,
                            totalBytes,
                            percentage,
                            DownloadStatus.DOWNLOADING,
                        )
                    )
                }
            }

            val rawBytes = accumulator.toByteArray()
            val dbBytes = dbHelper.decompressIfZip(rawBytes)
            if (!looksLikeSqlite(dbBytes)) {
                mutex.withLock { statuses[translationId] = DownloadStatus.FAILED }
                emit(DownloadProgress(translationId, 0, 0, 0f, DownloadStatus.FAILED))
                return@flow
            }
            dbHelper.writeDatabaseBytes(translationId, dbBytes)

            mutex.withLock { statuses[translationId] = DownloadStatus.COMPLETED }
            emit(
                DownloadProgress(
                    translationId,
                    bytesDownloaded,
                    totalBytes,
                    100f,
                    DownloadStatus.COMPLETED,
                )
            )

        } catch (e: Exception) {
            mutex.withLock { statuses[translationId] = DownloadStatus.FAILED }
            emit(DownloadProgress(translationId, 0, 0, 0f, DownloadStatus.FAILED))
            dbHelper.deleteDatabase(translationId)
        }
    }

    override suspend fun autoDownloadTranslation(languageCode: String): Result<TranslationInfo> {
        val match = metadataSource.loadBundled().firstOrNull { it.languageCode == languageCode }
            ?: return Result.failure(IllegalArgumentException("No translation for $languageCode"))
        return Result.success(match)
    }

    override suspend fun deleteTranslation(translationId: String): Result<Unit> {
        mutex.withLock { statuses.remove(translationId) }
        val success = dbHelper.deleteDatabase(translationId)
        return if (success) Result.success(Unit) else Result.failure(Exception("Failed to delete database"))
    }

    override suspend fun checkForUpdates(): List<TranslationInfo> = metadataSource.loadBundled()

    override suspend fun updateTranslation(translationId: String): Flow<DownloadProgress> =
        downloadTranslation(translationId)

    override suspend fun getDownloadStatus(translationId: String): DownloadStatus {
        return mutex.withLock {
            statuses[translationId] ?: if (dbHelper.isDatabaseDownloaded(translationId)) {
                DownloadStatus.COMPLETED
            } else {
                DownloadStatus.PENDING
            }
        }
    }

    override suspend fun cancelDownload(translationId: String): Boolean {
        mutex.withLock { statuses[translationId] = DownloadStatus.CANCELLED }
        return true
    }

    override suspend fun getAvailableTranslations(): List<TranslationInfo> =
        metadataSource.loadBundled()

    override suspend fun isTranslationDownloaded(translationId: String): Boolean =
        dbHelper.isDatabaseDownloaded(translationId)

    override suspend fun getVerseTranslation(sura: Int, ayah: Int, translationId: String): String? =
        dbHelper.getTranslationText(translationId, sura, ayah)

    override suspend fun searchTranslation(
        query: String,
        translationId: String,
        limit: Int,
    ): List<TranslationSearchHit> =
        dbHelper.searchTranslation(translationId, query, limit)
}
