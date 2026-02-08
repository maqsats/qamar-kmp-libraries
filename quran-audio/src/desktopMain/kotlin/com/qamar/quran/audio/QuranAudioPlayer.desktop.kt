package com.qamar.quran.audio

import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.io.File
import java.util.concurrent.atomic.AtomicReference

actual class QuranAudioPlayer actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = true

    private val mediaPlayerRef = AtomicReference<MediaPlayer?>(null)

    @Volatile
    private var stateInternal: AudioPlaybackState = AudioPlaybackState.IDLE

    @Volatile
    private var playbackRateInternal: Float = 1f

    @Volatile
    private var volumeInternal: Float = 1f

    @Volatile
    private var durationMsInternal: Long? = null

    @Volatile
    private var positionMsInternal: Long = 0L

    actual val state: AudioPlaybackState
        get() = stateInternal

    actual val isPlaying: Boolean
        get() = stateInternal == AudioPlaybackState.PLAYING

    actual val durationMs: Long?
        get() = durationMsInternal

    actual val positionMs: Long
        get() = mediaPlayerRef.get()?.currentTime?.let { (it.toMillis()).toLong() }
            ?: positionMsInternal

    actual val playbackRate: Float
        get() = playbackRateInternal

    init {
        ensureJavaFxStarted()
    }

    actual fun load(source: String, autoPlay: Boolean) {
        if (source.isBlank()) {
            stateInternal = AudioPlaybackState.ERROR
            return
        }
        stateInternal = AudioPlaybackState.LOADING
        val uri = when {
            source.startsWith("http://") || source.startsWith("https://") -> source
            else -> File(source).toURI().toString()
        }
        Platform.runLater {
            try {
                val media = Media(uri)
                val newPlayer = MediaPlayer(media).apply {
                    volume = volumeInternal.toDouble()
                    rate = playbackRateInternal.toDouble()
                    setOnReady {
                        stateInternal = AudioPlaybackState.READY
                        durationMsInternal = totalDuration?.let { (it.toMillis()).toLong() }
                        if (autoPlay) {
                            play()
                            stateInternal = AudioPlaybackState.PLAYING
                        }
                    }
                    setOnPlaying { stateInternal = AudioPlaybackState.PLAYING }
                    setOnPaused { stateInternal = AudioPlaybackState.PAUSED }
                    setOnStopped { stateInternal = AudioPlaybackState.STOPPED }
                    setOnEndOfMedia { stateInternal = AudioPlaybackState.COMPLETED }
                    setOnError {
                        stateInternal = AudioPlaybackState.ERROR
                    }
                    statusProperty().addListener { _, _, newStatus ->
                        when (newStatus) {
                            MediaPlayer.Status.UNKNOWN, MediaPlayer.Status.STALLED ->
                                if (stateInternal != AudioPlaybackState.READY &&
                                    stateInternal != AudioPlaybackState.PLAYING &&
                                    stateInternal != AudioPlaybackState.PAUSED
                                ) {
                                    stateInternal = AudioPlaybackState.LOADING
                                }

                            MediaPlayer.Status.HALTED -> stateInternal = AudioPlaybackState.ERROR
                            else -> { /* other states set by listeners */
                            }
                        }
                    }
                }
                mediaPlayerRef.getAndSet(null)?.dispose()
                mediaPlayerRef.set(newPlayer)
            } catch (e: Exception) {
                stateInternal = AudioPlaybackState.ERROR
            }
        }
    }

    actual fun play() {
        Platform.runLater {
            mediaPlayerRef.get()?.let { mp ->
                mp.rate = playbackRateInternal.toDouble()
                mp.volume = volumeInternal.toDouble()
                mp.play()
                stateInternal = AudioPlaybackState.PLAYING
            }
        }
    }

    actual fun pause() {
        Platform.runLater {
            mediaPlayerRef.get()?.pause()
            if (stateInternal == AudioPlaybackState.PLAYING) {
                stateInternal = AudioPlaybackState.PAUSED
            }
        }
    }

    actual fun stop() {
        Platform.runLater {
            mediaPlayerRef.get()?.stop()
            stateInternal = AudioPlaybackState.STOPPED
        }
    }

    actual fun seekTo(positionMs: Long) {
        Platform.runLater {
            mediaPlayerRef.get()?.seek(Duration.millis(positionMs.coerceAtLeast(0L).toDouble()))
        }
    }

    actual fun setVolume(volume: Float) {
        volumeInternal = volume.coerceIn(0f, 1f)
        Platform.runLater {
            mediaPlayerRef.get()?.volume = volumeInternal.toDouble()
        }
    }

    actual fun setPlaybackRate(rate: Float) {
        playbackRateInternal = rate.coerceAtLeast(0.5f)
        Platform.runLater {
            mediaPlayerRef.get()?.rate = playbackRateInternal.toDouble()
        }
    }

    actual fun release() {
        Platform.runLater {
            mediaPlayerRef.getAndSet(null)?.dispose()
            stateInternal = AudioPlaybackState.IDLE
            durationMsInternal = null
            positionMsInternal = 0L
        }
    }

    private fun ensureJavaFxStarted() {
        try {
            Platform.startup { }
        } catch (_: IllegalStateException) {
            // Already started
        }
    }
}
