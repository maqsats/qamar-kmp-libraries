package com.qamar.quran.audio.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class AudioFileStore actual constructor(
    actual val platformContext: Any?,
) {
    actual val supportsCache: Boolean = true

    actual fun cacheDir(subdirectory: String): String? {
        val base = System.getProperty("user.home")
            ?: System.getProperty("java.io.tmpdir")
            ?: return null
        val dir = File(base, subdirectory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    actual fun exists(path: String): Boolean = File(path).exists()

    actual fun createDirectories(path: String) {
        File(path).mkdirs()
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            File(path).outputStream().use { it.write(bytes) }
        }
    }
}
