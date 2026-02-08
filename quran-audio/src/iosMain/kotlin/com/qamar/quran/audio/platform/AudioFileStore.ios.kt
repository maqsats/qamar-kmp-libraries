package com.qamar.quran.audio.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual class AudioFileStore actual constructor(
    actual val platformContext: Any?,
) {
    actual val supportsCache: Boolean = true

    actual fun cacheDir(subdirectory: String): String? {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val base = (paths as? List<*>)?.firstOrNull()?.toString() ?: return null
        val dirPath = base.trimEnd { it == '/' } + "/" + subdirectory.trimStart { it == '/' }
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(dirPath)) {
            manager.createDirectoryAtPath(dirPath, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return dirPath
    }

    actual fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun createDirectories(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray) {
        val data = bytes.toNSData()
        NSFileManager.defaultManager.createFileAtPath(path, data, null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
