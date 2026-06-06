package com.qamar.quran.audio.platform

/**
 * Wasm/web has no writable local filesystem for audio caching (same stance as
 * the JS target). Reports no cache support and no-ops directory creation.
 */
actual class AudioFileStore actual constructor(
    actual val platformContext: Any?,
) {
    actual val supportsCache: Boolean = false

    actual fun cacheDir(subdirectory: String): String? = null

    actual fun exists(path: String): Boolean = false

    actual fun createDirectories(path: String) {
        // No-op for wasm/web target.
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        throw UnsupportedOperationException("Audio caching is not supported on wasm/web targets")
    }
}
