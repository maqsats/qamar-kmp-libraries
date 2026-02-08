package com.qamar.quran.audio.model

enum class AudioKind {
    AYAH,
    SURA,
    PAGE,
}

sealed class AudioRequest {
    abstract val reciter: Reciter

    data class Ayah(
        val sura: Int,
        val ayah: Int,
        override val reciter: Reciter,
    ) : AudioRequest()

    data class Sura(
        val sura: Int,
        override val reciter: Reciter,
    ) : AudioRequest()

    data class Page(
        val page: Int,
        override val reciter: Reciter,
    ) : AudioRequest()

    val kind: AudioKind
        get() = when (this) {
            is Ayah -> AudioKind.AYAH
            is Sura -> AudioKind.SURA
            is Page -> AudioKind.PAGE
        }
}
