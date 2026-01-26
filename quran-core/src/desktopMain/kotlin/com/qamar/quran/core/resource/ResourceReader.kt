package com.qamar.quran.core.resource

import java.io.IOException

actual class ResourceReader actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val loader = this::class.java.classLoader

    actual fun readText(path: String): String =
        loader?.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Resource not found: $path")

    actual fun readBytes(path: String): ByteArray =
        loader?.getResourceAsStream(path)?.readBytes()
            ?: throw IOException("Resource not found: $path")
}
