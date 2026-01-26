package com.qamar.quran.core.resource

@Suppress("UnsafeCastFromDynamic")
actual class ResourceReader actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private fun requireText(path: String): String? {
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

    private fun requireBytes(path: String): ByteArray? {
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

    actual fun readText(path: String): String =
        requireText(path) ?: error("Resource not found on JS target: $path")

    actual fun readBytes(path: String): ByteArray =
        requireBytes(path) ?: error("Resource not found on JS target: $path")
}
