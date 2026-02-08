package com.qamar.quran.audio.config

import com.qamar.quran.audio.model.AudioKind


data class AudioSourceSet(
    val ayah: List<AudioSourceConfig> = emptyList(),
    val sura: List<AudioSourceConfig> = emptyList(),
    val page: List<AudioSourceConfig> = emptyList(),
)

sealed class AudioSourceConfig {
    abstract val id: String
    abstract val kind: AudioKind

    data class Template(
        override val id: String,
        override val kind: AudioKind,
        val template: String,
    ) : AudioSourceConfig()

    data class JsonEndpoint(
        override val id: String,
        override val kind: AudioKind,
        val urlTemplate: String,
        val format: AudioResponseFormat,
        val headers: Map<String, String> = emptyMap(),
    ) : AudioSourceConfig()
}


enum class AudioResponseMode {
    OBJECT_MAP,
    ARRAY,
}

data class AudioResponseFormat(
    val mode: AudioResponseMode = AudioResponseMode.OBJECT_MAP,
    val listPath: String? = null,
    val reciterIdField: String = "id",
    val urlFields: List<String> = listOf("originalUrl", "url"),
)
