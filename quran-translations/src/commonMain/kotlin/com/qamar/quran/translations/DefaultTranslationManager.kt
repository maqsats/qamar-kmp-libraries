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
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class DefaultTranslationManager(
    platformContext: Any? = null,
    private val metadataSource: TranslationMetadataSource = TranslationMetadataSource(platformContext),
    /**
     * Hosted catalog to prefer over the bundled one, or `null` to skip the
     * lookup entirely and read only what shipped in the app. Tests pass `null`
     * so they never depend on the network.
     */
    private val remoteCatalogUrl: String? = TranslationMetadataSource.DefaultRemoteCatalogUrl,
    /** Injectable so tests can drive the catalog lookup and downloads off a MockEngine. */
    private val httpClient: HttpClient = defaultTranslationHttpClient(),
) : TranslationManager {

    private val dbHelper = TranslationDatabaseHelper(platformContext)
    private val mutex = Mutex()
    private val statuses = mutableMapOf<String, DownloadStatus>()

    /**
     * The translation catalog, best source first:
     *
     *  1. the **hosted** catalog, so `firebase deploy --only hosting` changes the
     *     list on devices that will never receive an app update;
     *  2. the **last hosted catalog this device accepted**, so an edition the user
     *     downloaded from it still resolves to a display name while offline -
     *     without this the reader header would fall back to printing a raw id and
     *     the downloads screen would lose its active row;
     *  3. the copy **bundled** in the app, which is always present.
     *
     * Cached process-wide (see [TranslationCatalogCache]) because every screen
     * that lists translations calls this and the app builds three managers. A
     * fallback is cached far more briefly than a good fetch, so coming back online
     * is noticed quickly without re-trying the network on every call.
     */
    private suspend fun catalog(): List<TranslationInfo> = TranslationCatalogCache.mutex.withLock {
        TranslationCatalogCache.entries?.let { cached ->
            if (!TranslationCatalogCache.isStale) return@withLock cached
        }

        fetchHostedCatalog()?.let { (entries, raw) ->
            // Best effort: losing the write only costs us the offline fallback.
            runCatching { dbHelper.writeDatabaseBytes(TranslationCatalogCache.Key, raw.encodeToByteArray()) }
            return@withLock TranslationCatalogCache.put(entries, TranslationCatalogCache.HostedTtl)
        }
        cachedHostedCatalog()?.let {
            return@withLock TranslationCatalogCache.put(it, TranslationCatalogCache.RetryTtl)
        }
        return@withLock TranslationCatalogCache.put(
            metadataSource.loadBundled(),
            TranslationCatalogCache.RetryTtl,
        )
    }

    /** The hosted catalog and its raw text, or null if unreachable, unparseable or unusable. */
    private suspend fun fetchHostedCatalog(): Pair<List<TranslationInfo>, String>? {
        val url = remoteCatalogUrl ?: return null
        return runCatching {
            val raw = httpClient.get(url) {
                // Much tighter than the database-download timeouts configured above:
                // a document this small is either served or it isn't, and an offline
                // user must not sit through a 20s connect before the bundled list
                // appears.
                timeout {
                    requestTimeoutMillis = 8_000
                    connectTimeoutMillis = 5_000
                    socketTimeoutMillis = 8_000
                }
            }.bodyAsText()
            val entries = metadataSource.parse(raw)
            if (TranslationMetadataSource.isUsable(entries)) entries to raw else null
        }.getOrNull()
    }

    /** The last hosted catalog this device accepted, or null if there isn't one. */
    private suspend fun cachedHostedCatalog(): List<TranslationInfo>? = runCatching {
        val raw = dbHelper.readDatabaseBytes(TranslationCatalogCache.Key)?.decodeToString()
        if (raw == null) null
        else metadataSource.parse(raw).takeIf { TranslationMetadataSource.isUsable(it) }
    }.getOrNull()

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

        } catch (e: CancellationException) {
            // The collector went away - the screen was closed, or a `first()` /
            // `take()` was satisfied. Emitting from here would be an emission
            // after cancellation, which surfaces in the collector as
            // "Flow exception transparency is violated" instead of an ordinary
            // cancellation. Nothing is written to disk until the download
            // finishes, so there is no partial file to clean up; just record the
            // outcome outside the cancelled job and rethrow untouched.
            withContext(NonCancellable) {
                mutex.withLock { statuses[translationId] = DownloadStatus.CANCELLED }
            }
            throw e
        } catch (e: Exception) {
            mutex.withLock { statuses[translationId] = DownloadStatus.FAILED }
            emit(DownloadProgress(translationId, 0, 0, 0f, DownloadStatus.FAILED))
            dbHelper.deleteDatabase(translationId)
        }
    }

    override suspend fun autoDownloadTranslation(languageCode: String): Result<TranslationInfo> {
        val match = catalog().firstOrNull { it.languageCode == languageCode }
            ?: return Result.failure(IllegalArgumentException("No translation for $languageCode"))
        return Result.success(match)
    }

    override suspend fun deleteTranslation(translationId: String): Result<Unit> {
        mutex.withLock { statuses.remove(translationId) }
        val success = dbHelper.deleteDatabase(translationId)
        return if (success) Result.success(Unit) else Result.failure(Exception("Failed to delete database"))
    }

    override suspend fun checkForUpdates(): List<TranslationInfo> = catalog()

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

    override suspend fun getAvailableTranslations(): List<TranslationInfo> = catalog()

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


/**
 * Process-wide cache of the resolved translation catalog.
 *
 * Deliberately not per-instance: the app constructs three
 * [DefaultTranslationManager]s (QuranApiFactory, DefaultQuranRepository and the
 * downloads Koin module) and they must agree on one list and fetch it once
 * between them rather than three times.
 *
 * Freshness is tracked with a monotonic mark, so it is immune to the wall clock
 * moving; the cache does not survive process death, which is exactly what we
 * want - every cold start re-checks the hosted catalog.
 */
internal object TranslationCatalogCache {
    val mutex = Mutex()

    /** How long an accepted hosted catalog is served before we look again. */
    val HostedTtl: Duration = 6.hours

    /** How long a fallback is served before re-trying the network. Short on purpose. */
    val RetryTtl: Duration = 5.minutes

    /**
     * Storage key for the persisted hosted catalog. It shares the namespace
     * translation databases use, so the leading underscores keep it clear of any
     * real translationId (all of which look like `quran.*`).
     */
    const val Key: String = "__catalog"

    var entries: List<TranslationInfo>? = null
        private set

    private var mark: TimeMark? = null
    private var ttl: Duration = Duration.ZERO

    val isStale: Boolean get() = mark?.let { it.elapsedNow() >= ttl } ?: true

    fun put(value: List<TranslationInfo>, forTtl: Duration): List<TranslationInfo> {
        entries = value
        ttl = forTtl
        mark = TimeSource.Monotonic.markNow()
        return value
    }

    /** Test hook: forget everything, so one test is never answered from another's fetch. */
    fun clear() {
        entries = null
        mark = null
        ttl = Duration.ZERO
    }
}

/**
 * The client used for real downloads. Generous timeouts because translation
 * databases run to several megabytes; the catalog lookup overrides them per
 * request with much tighter ones.
 */
private fun defaultTranslationHttpClient(): HttpClient = HttpClient {
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
