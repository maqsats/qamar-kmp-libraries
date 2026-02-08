package com.qamar.quran.audio.config

import com.qamar.quran.audio.internal.QuranAudioConfigParser


data class QuranAudioConfig(
    val reciters: List<RecitersSourceConfig>,
    val audio: AudioSourceSet,
    val cache: CacheConfig = CacheConfig(),
    val network: NetworkConfig = NetworkConfig(),
) {
    companion object {
        fun fromJson(text: String): QuranAudioConfig = QuranAudioConfigParser.fromJson(text)
    }
}
