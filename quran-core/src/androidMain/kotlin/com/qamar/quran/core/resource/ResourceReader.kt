package com.qamar.quran.core.resource

import android.content.Context
import java.io.IOException

actual class ResourceReader actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val loader = this::class.java.classLoader

    private fun assetBytes(path: String): ByteArray? {
        val context = platformContext as? Context ?: return null
        return runCatching { context.assets.open(path).use { it.readBytes() } }.getOrNull()
    }

    actual fun readText(path: String): String =
        loader?.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: assetBytes(path)?.decodeToString()
            ?: throw IOException("Resource not found: $path")

    actual fun readBytes(path: String): ByteArray =
        loader?.getResourceAsStream(path)?.readBytes()
            ?: assetBytes(path)
            ?: throw IOException("Resource not found: $path")
}
