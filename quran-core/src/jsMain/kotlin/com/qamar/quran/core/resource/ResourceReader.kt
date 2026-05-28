package com.qamar.quran.core.resource

@Suppress("UnsafeCastFromDynamic")
actual class ResourceReader actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext

    private val isBrowser: Boolean = try {
        js("typeof window !== 'undefined' && typeof window.document !== 'undefined'") as Boolean
    } catch (t: dynamic) {
        false
    }

    private fun browserUrl(path: String): String =
        if (path.startsWith("/") || path.startsWith("http")) path else "/$path"

    private fun xhrGet(path: String, asBinary: Boolean): dynamic {
        val xhr: dynamic = js("new XMLHttpRequest()")
        xhr.open("GET", browserUrl(path), false)
        if (asBinary) {
            xhr.overrideMimeType("text/plain; charset=x-user-defined")
        }
        xhr.send(null)
        return if ((xhr.status as Int) == 200 || (xhr.status as Int) == 0) xhr else null
    }

    private fun nodeRequireText(path: String): String? {
        return try {
            val req: dynamic = js("typeof require !== 'undefined' ? require : null")
            if (req != null) {
                val content: dynamic = req(path)
                when (content) {
                    is String -> content
                    else -> content?.toString()
                }
            } else null
        } catch (t: dynamic) {
            null
        }
    }

    private fun nodeRequireBytes(path: String): ByteArray? {
        return try {
            val req: dynamic = js("typeof require !== 'undefined' ? require : null")
            if (req != null) {
                val buf: dynamic = req(path)
                if (buf != null && js("Buffer").isBuffer(buf) as Boolean) {
                    val uint8: dynamic = js("new Uint8Array")(buf)
                    ByteArray(uint8.length as Int) { i -> uint8[i] as Byte }
                } else null
            } else null
        } catch (t: dynamic) {
            null
        }
    }

    actual fun readText(path: String): String {
        if (isBrowser) {
            val xhr = xhrGet(path, asBinary = false)
                ?: error("Resource not found on JS target: $path")
            return xhr.responseText as String
        }
        return nodeRequireText(path)
            ?: error("Resource not found on JS target: $path")
    }

    actual fun readBytes(path: String): ByteArray {
        if (isBrowser) {
            val xhr = xhrGet(path, asBinary = true)
                ?: error("Resource not found on JS target: $path")
            val text: String = xhr.responseText as String
            return ByteArray(text.length) { i -> (text[i].code and 0xFF).toByte() }
        }
        return nodeRequireBytes(path)
            ?: error("Resource not found on JS target: $path")
    }
}
