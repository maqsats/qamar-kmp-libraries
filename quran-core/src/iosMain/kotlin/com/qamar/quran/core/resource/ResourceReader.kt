package com.qamar.quran.core.resource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class ResourceReader actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private fun dataForResource(path: String): NSData? {
        val components = path.split("/")
        val nameWithExt = components.last()
        val dotIndex = nameWithExt.lastIndexOf('.')
        val name = if (dotIndex >= 0) nameWithExt.substring(0, dotIndex) else nameWithExt
        val ext = if (dotIndex >= 0) nameWithExt.substring(dotIndex + 1) else null
        return NSBundle.mainBundle.pathForResource(name, ext)?.let { fullPath ->
            NSData.dataWithContentsOfFile(fullPath)
        }
    }

    actual fun readText(path: String): String {
        val data = dataForResource(path)
            ?: error("Resource not found: $path")
        val nsString =
            NSString.create(data = data, encoding = platform.Foundation.NSUTF8StringEncoding)
                ?: error("Failed to decode resource: $path")
        return nsString as String
    }

    actual fun readBytes(path: String): ByteArray {
        val data = dataForResource(path) ?: error("Resource not found: $path")
        val length = data.length.toInt()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }
}
