package com.qamar.quran.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
actual class QuranAudioPlayer actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = true

    private var player: AVPlayer? = null
    private var endObserver: Any? = null
    private var stateInternal: AudioPlaybackState = AudioPlaybackState.IDLE
    private var playbackRateInternal: Float = 1f
    private var volumeInternal: Float = 1f

    actual val state: AudioPlaybackState
        get() = stateInternal

    actual val isPlaying: Boolean
        get() = (player?.rate ?: 0f) != 0f

    actual val durationMs: Long?
        get() = player?.currentItem?.duration?.let { duration ->
            val seconds = CMTimeGetSeconds(duration)
            if (seconds.isNaN() || seconds.isInfinite()) null else (seconds * 1000).toLong()
        }

    actual val positionMs: Long
        get() = player?.currentTime()?.let { time ->
            val seconds = CMTimeGetSeconds(time)
            if (seconds.isNaN() || seconds.isInfinite()) 0L else (seconds * 1000).toLong()
        } ?: 0L

    actual val playbackRate: Float
        get() = playbackRateInternal

    actual fun load(source: String, autoPlay: Boolean) {
        if (source.isBlank()) {
            stateInternal = AudioPlaybackState.ERROR
            return
        }
        val url = if (source.startsWith("http://") || source.startsWith("https://")) {
            NSURL.URLWithString(source)
        } else {
            NSURL.fileURLWithPath(source)
        } ?: run {
            stateInternal = AudioPlaybackState.ERROR
            return
        }

        val item = AVPlayerItem.playerItemWithURL(url)
        val currentPlayer = player
        if (currentPlayer == null) {
            player = AVPlayer.playerWithPlayerItem(item)
        } else {
            currentPlayer.replaceCurrentItemWithPlayerItem(item)
        }
        attachEndObserver(item)
        player?.volume = volumeInternal
        stateInternal = AudioPlaybackState.READY
        if (autoPlay) {
            play()
        }
    }

    actual fun play() {
        val currentPlayer = player ?: return
        currentPlayer.play()
        if (playbackRateInternal != 1f) {
            currentPlayer.rate = playbackRateInternal
        }
        stateInternal = AudioPlaybackState.PLAYING
    }

    actual fun pause() {
        player?.pause()
        stateInternal = AudioPlaybackState.PAUSED
    }

    actual fun stop() {
        val currentPlayer = player ?: return
        currentPlayer.pause()
        seekTo(0)
        stateInternal = AudioPlaybackState.STOPPED
    }

    actual fun seekTo(positionMs: Long) {
        val currentPlayer = player ?: return
        val seconds = positionMs.coerceAtLeast(0L).toDouble() / 1000.0
        val time = CMTimeMakeWithSeconds(seconds, 1000)
        currentPlayer.seekToTime(time)
    }

    actual fun setVolume(volume: Float) {
        volumeInternal = volume.coerceIn(0f, 1f)
        player?.volume = volumeInternal
    }

    actual fun setPlaybackRate(rate: Float) {
        playbackRateInternal = rate.coerceAtLeast(0.5f)
        val currentPlayer = player ?: return
        if (stateInternal == AudioPlaybackState.PLAYING) {
            currentPlayer.rate = playbackRateInternal
        }
    }

    actual fun release() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player?.pause()
        player = null
        stateInternal = AudioPlaybackState.IDLE
    }

    private fun attachEndObserver(item: AVPlayerItem) {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = null,
        ) { _ ->
            stateInternal = AudioPlaybackState.COMPLETED
        }
    }
}
