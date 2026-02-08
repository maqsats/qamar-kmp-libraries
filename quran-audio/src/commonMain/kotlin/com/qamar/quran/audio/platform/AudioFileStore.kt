package com.qamar.quran.audio.platform

expect class AudioFileStore(platformContext: Any?) {
    val platformContext: Any?
    val supportsCache: Boolean

    fun cacheDir(subdirectory: String): String?
    fun exists(path: String): Boolean
    fun createDirectories(path: String)
    suspend fun writeBytes(path: String, bytes: ByteArray)
}
