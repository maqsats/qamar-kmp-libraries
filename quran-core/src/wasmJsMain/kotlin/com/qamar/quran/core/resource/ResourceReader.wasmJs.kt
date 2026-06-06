package com.qamar.quran.core.resource

import org.w3c.xhr.XMLHttpRequest

/**
 * Wasm/web resource reader. Mirrors the JS browser path but uses kotlinx-browser's
 * typed [XMLHttpRequest] instead of `dynamic` (which does not exist on Kotlin/Wasm).
 *
 * Binary reads use the classic `charset=x-user-defined` trick so each response
 * char maps 1:1 to a byte. wasm runs in the browser for this library, so there is
 * no Node `require` fallback (that path is JS-target only).
 */
actual class ResourceReader actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext

    private fun browserUrl(path: String): String =
        if (path.startsWith("/") || path.startsWith("http")) path else "/$path"

    private fun xhrGet(path: String, asBinary: Boolean): XMLHttpRequest? {
        val xhr = XMLHttpRequest()
        xhr.open("GET", browserUrl(path), false)
        if (asBinary) {
            xhr.overrideMimeType("text/plain; charset=x-user-defined")
        }
        xhr.send()
        val status = xhr.status.toInt()
        return if (status == 200 || status == 0) xhr else null
    }

    actual fun readText(path: String): String {
        val xhr = xhrGet(path, asBinary = false)
            ?: error("Resource not found on wasm target: $path")
        return xhr.responseText
    }

    actual fun readBytes(path: String): ByteArray {
        val xhr = xhrGet(path, asBinary = true)
            ?: error("Resource not found on wasm target: $path")
        val text: String = xhr.responseText
        return ByteArray(text.length) { i -> (text[i].code and 0xFF).toByte() }
    }
}
