package com.qamar.quran.audio.model

data class AudioUrlCandidate(
    val url: String,
    val sourceId: String,
    val isOriginal: Boolean = false,
)

data class AudioUrlResult(
    val url: String,
    val candidates: List<AudioUrlCandidate>,
    val usedSourceId: String,
)

data class AudioFetchResult(
    val url: String,
    val localPath: String?,
    val candidates: List<AudioUrlCandidate>,
    val usedSourceId: String,
    val fromCache: Boolean,
) {
    /**
     * Path to use for playback: local file path when cached, otherwise the remote URL.
     * Use this when feeding a player (e.g. MediaPlayer/AVPlayer) so it can play from file or stream.
     */
    fun effectivePlaybackPath(): String = localPath ?: url
}

enum class CachePolicy {
    REMOTE_ONLY,
    CACHE_IF_POSSIBLE,
    CACHE_ONLY,
}
