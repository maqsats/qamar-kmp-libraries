package com.qamar.quran.audio

import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event

actual class QuranAudioPlayer actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = true

    private var audio: HTMLAudioElement? = null
    private var pendingAutoPlay: Boolean = false
    private var stateInternal: AudioPlaybackState = AudioPlaybackState.IDLE
    private var playbackRateInternal: Float = 1f
    private var volumeInternal: Float = 1f

    actual val state: AudioPlaybackState
        get() = stateInternal

    actual val isPlaying: Boolean
        get() = audio?.paused == false

    actual val durationMs: Long?
        get() = audio?.duration?.takeIf { it.isFinite() && !it.isNaN() && it > 0 }
            ?.let { (it * 1000).toLong() }

    actual val positionMs: Long
        get() = ((audio?.currentTime ?: 0.0) * 1000).toLong()

    actual val playbackRate: Float
        get() = playbackRateInternal

    actual fun load(source: String, autoPlay: Boolean) {
        if (source.isBlank()) {
            stateInternal = AudioPlaybackState.ERROR
            return
        }
        pendingAutoPlay = autoPlay
        val element = ensureAudio()
        stateInternal = AudioPlaybackState.LOADING
        element.src = source
        element.load()
    }

    actual fun play() {
        val element = audio ?: return
        element.play()
        stateInternal = AudioPlaybackState.PLAYING
    }

    actual fun pause() {
        val element = audio ?: return
        element.pause()
        stateInternal = AudioPlaybackState.PAUSED
    }

    actual fun stop() {
        val element = audio ?: return
        element.pause()
        element.currentTime = 0.0
        stateInternal = AudioPlaybackState.STOPPED
    }

    actual fun seekTo(positionMs: Long) {
        val element = audio ?: return
        element.currentTime = positionMs.coerceAtLeast(0L).toDouble() / 1000.0
    }

    actual fun setVolume(volume: Float) {
        volumeInternal = volume.coerceIn(0f, 1f)
        audio?.volume = volumeInternal.toDouble()
    }

    actual fun setPlaybackRate(rate: Float) {
        playbackRateInternal = rate.coerceAtLeast(0.5f)
        audio?.playbackRate = playbackRateInternal.toDouble()
    }

    actual fun release() {
        audio?.pause()
        audio?.src = ""
        audio?.load()
        audio = null
        pendingAutoPlay = false
        stateInternal = AudioPlaybackState.IDLE
    }

    private fun ensureAudio(): HTMLAudioElement {
        return audio ?: (document.createElement("audio") as HTMLAudioElement).also { element ->
            audio = element
            element.oncanplay = { _: Event ->
                stateInternal = AudioPlaybackState.READY
                element.playbackRate = playbackRateInternal.toDouble()
                element.volume = volumeInternal.toDouble()
                if (pendingAutoPlay) {
                    element.play()
                    stateInternal = AudioPlaybackState.PLAYING
                }
                null
            }
            element.onplay = { _: Event ->
                stateInternal = AudioPlaybackState.PLAYING
                null
            }
            element.onpause = { _: Event ->
                if (stateInternal != AudioPlaybackState.STOPPED && stateInternal != AudioPlaybackState.COMPLETED) {
                    stateInternal = AudioPlaybackState.PAUSED
                }
                null
            }
            element.onended = { _: Event ->
                stateInternal = AudioPlaybackState.COMPLETED
                null
            }
            element.addEventListener("error", { _: Event ->
                stateInternal = AudioPlaybackState.ERROR
            })
        }
    }
}
