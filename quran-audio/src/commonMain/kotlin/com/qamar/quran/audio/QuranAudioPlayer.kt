package com.qamar.quran.audio

import com.qamar.quran.audio.model.AudioFetchResult

/**
 * Lightweight cross-platform audio player abstraction for Quran audio.
 *
 * The [source] passed to [load] can be a remote URL (http/https) or a local file path.
 */
expect class QuranAudioPlayer(platformContext: Any?) {
    val platformContext: Any?
    val isSupported: Boolean

    val state: AudioPlaybackState
    val isPlaying: Boolean
    val durationMs: Long?
    val positionMs: Long
    val playbackRate: Float

    fun load(source: String, autoPlay: Boolean = false)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setPlaybackRate(rate: Float)
    fun release()
}

enum class AudioPlaybackState {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR,
}

/** Convenience: load a fetched result using its effective playback path. */
fun QuranAudioPlayer.load(result: AudioFetchResult, autoPlay: Boolean = true) {
    load(result.effectivePlaybackPath(), autoPlay)
}
