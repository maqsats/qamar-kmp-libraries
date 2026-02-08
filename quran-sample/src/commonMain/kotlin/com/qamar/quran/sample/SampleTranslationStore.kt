package com.qamar.quran.sample

/**
 * Minimal helper used by the sample to download a translation DB and read verses from it.
 * Implementations may be a no-op on unsupported platforms.
 */
expect class SampleTranslationStore() {
    fun isSupported(): Boolean

    /**
     * Downloads the file at [url] to a local path (hint: cache dir) and returns the absolute path.
     */
    suspend fun downloadTranslation(url: String, fileName: String): String

    /**
     * Reads a single translation verse from the downloaded DB, or null if not available.
     */
    suspend fun readVerse(path: String, sura: Int, ayah: Int): String?
}
