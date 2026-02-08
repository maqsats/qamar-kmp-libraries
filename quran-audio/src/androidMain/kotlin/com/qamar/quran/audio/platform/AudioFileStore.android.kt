package com.qamar.quran.audio.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class AudioFileStore actual constructor(
    actual val platformContext: Any?,
) {
    private val context: Context? = platformContext as? Context

    actual val supportsCache: Boolean
        get() = context != null

    actual fun cacheDir(subdirectory: String): String? {
        val base = context?.getExternalFilesDir(null) ?: return null
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
