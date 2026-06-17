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

    /**
     * Invoked on every playback-state transition (LOADING, READY, PLAYING, PAUSED,
     * STOPPED, COMPLETED, ERROR, IDLE). Lets callers react to completion (e.g. auto-advance
     * to the next ayah) without polling. Callbacks may be delivered on a platform thread
     * (Android MediaPlayer looper, iOS notification queue, JavaFX thread, browser event loop),
     * so marshal to your own dispatcher before touching UI or non-thread-safe state.
     */
    var onStateChange: ((AudioPlaybackState) -> Unit)?

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
