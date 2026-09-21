package com.qamar.quran.translations

import com.qamar.quran.test.runTest
import com.qamar.quran.translations.model.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first

/**
 * The hosted catalog is what lets a translation-list change reach Android and iOS
 * without a store release, so these cover both halves of that bargain: the hosted
 * copy must win when it is good, and must never be able to make things worse when
 * it is not.
 */
class HostedCatalogTest {

    private val url = "https://example.test/translations.json"

    private val hostedJson = """
        {"data":[{"id":9001,"displayName":"Hosted Only","translator":"T",
        "languageCode":"zz","fileUrl":"https://example.test/quran.zz.hosted.db",
        "fileName":"quran.zz.hosted.db"}]}
    """.trimIndent()

    private fun manager(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): DefaultTranslationManager = DefaultTranslationManager(
        platformContext = null,
        metadataSource = TranslationMetadataSource(null),
        remoteCatalogUrl = url,
        httpClient = HttpClient(MockEngine { request -> handler(request) }) {
            install(HttpTimeout)
        },
    )

    private fun serving(body: String) = manager { respond(body) }

    /**
     * Runs [body] with no catalog cached in memory *or* on disk, and leaves none
     * behind either.
     *
     * Both halves matter. Before: a catalog persisted by a sibling test - or by a
     * real run of the desktop app on this machine, which shares
     * `~/.qamar/translations` - sits between "hosted" and "bundled" in the
     * fallback chain, so a test asserting the bundled floor would legitimately see
     * that instead. After: a leftover one-entry catalog would poison a locally-run
     * desktop app.
     */
    private fun hostedTest(body: suspend () -> Unit) = runTest {
        reset()
        try {
            body()
        } finally {
            reset()
        }
    }

    private suspend fun reset() {
        TranslationCatalogCache.clear()
        DefaultTranslationManager(null, TranslationMetadataSource(null), remoteCatalogUrl = null)
            .deleteTranslation(TranslationCatalogCache.Key)
    }

    private fun bundledSize(): Int = TranslationMetadataSource(null).loadBundled().size

    // ── the hosted copy wins ───────────────────────────────────────────────

    @Test
    fun hostedCatalogReplacesTheBundledOne() = hostedTest {
        val entries = serving(hostedJson).getAvailableTranslations()
        assertEquals(1, entries.size)
        assertEquals("quran.zz.hosted", entries.single().translationId)
    }

    @Test
    fun hostedCatalogAlsoDrivesCheckForUpdatesAndAutoDownload() = hostedTest {
        // All three entry points must agree, or the downloads screen and the reader
        // would disagree about which editions exist.
        val m = serving(hostedJson)
        assertEquals("quran.zz.hosted", m.checkForUpdates().single().translationId)
        assertEquals("zz", m.autoDownloadTranslation("zz").getOrNull()?.languageCode)
    }

    // ── a bad hosted copy must never make things worse ─────────────────────

    @Test
    fun anEmptyHostedCatalogFallsBackToBundled() = hostedTest {
        // A truncated deploy must not be able to empty the translation list.
        assertEquals(bundledSize(), serving("""{"data":[]}""").getAvailableTranslations().size)
    }

    @Test
    fun malformedHostedJsonFallsBackToBundled() = hostedTest {
        assertEquals(bundledSize(), serving("<html>404</html>").getAvailableTranslations().size)
    }

    @Test
    fun anEntryMissingItsDownloadUrlFallsBackToBundled() = hostedTest {
        val broken = """
            {"data":[{"id":1,"displayName":"x","languageCode":"zz",
            "fileUrl":"","fileName":"quran.zz.hosted.db"}]}
        """.trimIndent()
        assertEquals(bundledSize(), serving(broken).getAvailableTranslations().size)
    }

    @Test
    fun anHttpErrorFallsBackToBundled() = hostedTest {
        val m = manager { respondError(HttpStatusCode.InternalServerError) }
        assertEquals(bundledSize(), m.getAvailableTranslations().size)
    }

    @Test
    fun aNetworkFailureFallsBackToBundled() = hostedTest {
        val m = manager { throw RuntimeException("offline") }
        assertEquals(bundledSize(), m.getAvailableTranslations().size)
    }

    // ── the lookup is not repeated per call ────────────────────────────────

    @Test
    fun theCatalogIsFetchedOncePerProcessNotPerCall() = hostedTest {
        var requests = 0
        val m = manager { requests++; respond(hostedJson) }
        m.getAvailableTranslations()
        m.getAvailableTranslations()
        m.checkForUpdates()
        assertEquals(1, requests, "every list screen must not re-fetch the catalog")
    }

    @Test
    fun separateManagersShareOneLookup() = hostedTest {
        // The app builds three of these; they must not each hit the network.
        var requests = 0
        manager { requests++; respond(hostedJson) }.getAvailableTranslations()
        manager { requests++; respond(hostedJson) }.getAvailableTranslations()
        assertEquals(1, requests)
    }

    // ── offline resolvability ──────────────────────────────────────────────

    @Test
    fun theLastHostedCatalogSurvivesTheNetworkGoingAway() = hostedTest {
        // An edition only the hosted catalog knows about must still resolve to a
        // name once the network is gone, or the reader header falls back to
        // printing a raw id and the downloads screen loses its active row.
        serving(hostedJson).getAvailableTranslations()
        TranslationCatalogCache.clear() // simulate a cold start, disk cache intact

        val entries = manager { throw RuntimeException("offline") }.getAvailableTranslations()
        assertEquals("quran.zz.hosted", entries.single().translationId)
        assertNotEquals(bundledSize(), entries.size, "must use the persisted hosted catalog")
    }

    // ── cancellation is a cancellation, not a crash ────────────────────────

    @Test
    fun cancellingMidDownloadDoesNotViolateFlowTransparency() = hostedTest {
        // Regression: the blanket `catch (e: Exception)` used to swallow the
        // AbortFlowException that `first { }` throws to stop collection, then emit
        // from the catch block - which surfaces in the collecting scope as
        // IllegalStateException ("Flow exception transparency is violated").
        // Concretely: closing the downloads screen mid-download crashed it.
        val body = "x".repeat(64 * 1024)
        val m = manager { request ->
            if (request.url.toString() == url) respond(hostedJson) else respond(body)
        }
        val id = m.getAvailableTranslations().single().translationId

        // Aborts on an emission from *inside* the download loop, which is the case
        // the old catch block mishandled.
        val progress = m.downloadTranslation(id).first { it.bytesDownloaded > 0 }
        assertEquals(DownloadStatus.DOWNLOADING, progress.status)
        assertEquals(DownloadStatus.CANCELLED, m.getDownloadStatus(id))
    }

    @Test
    fun aFailedDownloadEmitsFailed() = hostedTest {
        val m = manager { request ->
            if (request.url.toString() == url) respond(hostedJson)
            else respondError(HttpStatusCode.NotFound)
        }
        val id = m.getAvailableTranslations().single().translationId
        val statuses = mutableListOf<DownloadStatus>()
        m.downloadTranslation(id).collect { statuses.add(it.status) }
        assertEquals(DownloadStatus.FAILED, statuses.last())
    }

    @Test
    fun aSuccessfulDownloadIsStoredAndReadable() = hostedTest {
        // The bytes must survive the round trip through platform storage, which is
        // also what the persisted catalog relies on.
        val sqlite = "SQLite format 3" + "x".repeat(2048)
        val m = manager { request ->
            if (request.url.toString() == url) respond(hostedJson) else respond(sqlite)
        }
        val id = m.getAvailableTranslations().single().translationId
        val statuses = mutableListOf<DownloadStatus>()
        m.downloadTranslation(id).collect { statuses.add(it.status) }
        assertEquals(DownloadStatus.COMPLETED, statuses.last())
        assertTrue(m.isTranslationDownloaded(id))
        assertTrue(m.deleteTranslation(id).isSuccess)
    }
}
