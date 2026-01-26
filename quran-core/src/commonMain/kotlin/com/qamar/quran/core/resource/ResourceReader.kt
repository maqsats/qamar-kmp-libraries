package com.qamar.quran.core.resource

/**
 * Minimal cross-platform resource reader for small bundled payloads (JSON, etc).
 * Implementations should throw if the resource cannot be found.
 */
expect class ResourceReader(platformContext: Any?) {
    val platformContext: Any?
    fun readText(path: String): String
    fun readBytes(path: String): ByteArray
}
