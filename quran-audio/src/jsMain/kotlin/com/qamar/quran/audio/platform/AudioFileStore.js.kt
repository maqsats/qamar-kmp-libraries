package com.qamar.quran.audio.platform

actual class AudioFileStore actual constructor(
    actual val platformContext: Any?,
) {
    actual val supportsCache: Boolean = false

    actual fun cacheDir(subdirectory: String): String? = null

    actual fun exists(path: String): Boolean = false

    actual fun createDirectories(path: String) {
        // No-op for JS target.
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        throw UnsupportedOperationException("Audio caching is not supported on JS targets")
    }
}
