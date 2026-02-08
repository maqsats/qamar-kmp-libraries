package com.qamar.quran.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build

actual class QuranAudioPlayer actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = true

    private var player: MediaPlayer? = null
    private var pendingAutoPlay: Boolean = false
    private var stateInternal: AudioPlaybackState = AudioPlaybackState.IDLE
    private var playbackRateInternal: Float = 1f
    private var volumeInternal: Float = 1f

    actual val state: AudioPlaybackState
        get() = stateInternal

    actual val isPlaying: Boolean
        get() = player?.isPlaying == true

    actual val durationMs: Long?
        get() = runCatching { player?.duration?.toLong() }.getOrNull()?.takeIf { it > 0 }

    actual val positionMs: Long
        get() = runCatching { player?.currentPosition?.toLong() }.getOrNull() ?: 0L

    actual val playbackRate: Float
        get() = playbackRateInternal

    actual fun load(source: String, autoPlay: Boolean) {
        if (source.isBlank()) {
            stateInternal = AudioPlaybackState.ERROR
            return
        }
        pendingAutoPlay = autoPlay
        val mp = ensurePlayer()
        stateInternal = AudioPlaybackState.LOADING
        runCatching {
            mp.reset()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            mp.setOnPreparedListener {
                stateInternal = AudioPlaybackState.READY
                applyVolume(it)
                applyPlaybackRate(it)
                if (pendingAutoPlay) {
                    it.start()
                    stateInternal = AudioPlaybackState.PLAYING
                }
            }
            mp.setOnCompletionListener {
                stateInternal = AudioPlaybackState.COMPLETED
            }
            mp.setOnErrorListener { _, _, _ ->
                stateInternal = AudioPlaybackState.ERROR
                true
            }
            mp.setDataSource(source)
            mp.prepareAsync()
        }.onFailure {
            stateInternal = AudioPlaybackState.ERROR
        }
    }

    actual fun play() {
        val mp = player ?: return
        runCatching {
            if (!mp.isPlaying) {
                mp.start()
                applyPlaybackRate(mp)
                stateInternal = AudioPlaybackState.PLAYING
            }
        }.onFailure {
            stateInternal = AudioPlaybackState.ERROR
        }
    }

    actual fun pause() {
        val mp = player ?: return
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
            }
            stateInternal = AudioPlaybackState.PAUSED
        }.onFailure {
            stateInternal = AudioPlaybackState.ERROR
        }
    }

    actual fun stop() {
        val mp = player ?: return
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
            }
            mp.seekTo(0)
            stateInternal = AudioPlaybackState.STOPPED
        }.onFailure {
            stateInternal = AudioPlaybackState.ERROR
        }
    }

    actual fun seekTo(positionMs: Long) {
        val mp = player ?: return
        runCatching {
            mp.seekTo(positionMs.coerceAtLeast(0L).toInt())
        }.onFailure {
            stateInternal = AudioPlaybackState.ERROR
        }
    }

    actual fun setVolume(volume: Float) {
        volumeInternal = volume.coerceIn(0f, 1f)
        player?.let { applyVolume(it) }
    }

    actual fun setPlaybackRate(rate: Float) {
        playbackRateInternal = rate.coerceAtLeast(0.5f)
        player?.let { applyPlaybackRate(it) }
    }

    actual fun release() {
        player?.release()
        player = null
        pendingAutoPlay = false
        stateInternal = AudioPlaybackState.IDLE
    }

    private fun ensurePlayer(): MediaPlayer {
        return player ?: MediaPlayer().also { player = it }
    }

    private fun applyVolume(mp: MediaPlayer) {
        mp.setVolume(volumeInternal, volumeInternal)
    }

    private fun applyPlaybackRate(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT < 23) return
        runCatching {
            val params = mp.playbackParams
            params.speed = playbackRateInternal
            mp.playbackParams = params
        }
    }
}
