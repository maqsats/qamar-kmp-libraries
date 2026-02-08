package com.qamar.quran.audio

import com.qamar.quran.audio.model.AudioFetchResult
import com.qamar.quran.audio.model.AudioRequest
import com.qamar.quran.audio.model.AudioUrlResult
import com.qamar.quran.audio.model.CachePolicy
import com.qamar.quran.audio.model.Reciter

/**
 * API for Quran audio: reciters list and audio URL/fetch by ayah, sura, or page.
 * All endpoints and response formats are driven by [QuranAudioConfig]; the application
 * can change config (e.g. from remote config) so reciter list and audio URLs continue
 * to work when providers or formats change.
 */
interface QuranAudioApi {

    /** Fetches the list of available reciters from configured sources (tries each until one succeeds). */
    suspend fun getReciters(): List<Reciter>

    /** Resolves the audio URL for the given request (ayah, sura, or page). */
    suspend fun getAudioUrl(
        request: AudioRequest,
        validateUrls: Boolean = false,
    ): AudioUrlResult

    /**
     * Fetches audio with optional caching. Returns URL and, when cache is used, local file path.
     * Use [AudioFetchResult.effectivePlaybackPath] for the path/URL to pass to a player.
     */
    suspend fun fetchAudio(
        request: AudioRequest,
        cachePolicy: CachePolicy? = null,
    ): AudioFetchResult

    // --- Convenience overloads (by ayah, sura, or page) ---

    suspend fun getAudioUrl(sura: Int, ayah: Int, reciter: Reciter, validateUrls: Boolean = false): AudioUrlResult =
        getAudioUrl(AudioRequest.Ayah(sura, ayah, reciter), validateUrls)

    suspend fun getAudioUrlForSura(sura: Int, reciter: Reciter, validateUrls: Boolean = false): AudioUrlResult =
        getAudioUrl(AudioRequest.Sura(sura, reciter), validateUrls)

    suspend fun getAudioUrlForPage(page: Int, reciter: Reciter, validateUrls: Boolean = false): AudioUrlResult =
        getAudioUrl(AudioRequest.Page(page, reciter), validateUrls)

    suspend fun fetchAudio(sura: Int, ayah: Int, reciter: Reciter, cachePolicy: CachePolicy? = null): AudioFetchResult =
        fetchAudio(AudioRequest.Ayah(sura, ayah, reciter), cachePolicy)

    suspend fun fetchAudioForSura(sura: Int, reciter: Reciter, cachePolicy: CachePolicy? = null): AudioFetchResult =
        fetchAudio(AudioRequest.Sura(sura, reciter), cachePolicy)

    suspend fun fetchAudioForPage(page: Int, reciter: Reciter, cachePolicy: CachePolicy? = null): AudioFetchResult =
        fetchAudio(AudioRequest.Page(page, reciter), cachePolicy)
}
