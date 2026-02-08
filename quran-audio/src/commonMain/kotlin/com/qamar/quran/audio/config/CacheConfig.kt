package com.qamar.quran.audio.config

import com.qamar.quran.audio.model.AudioKind
import com.qamar.quran.audio.model.CachePolicy


data class CacheConfig(
    val enabled: Boolean = true,
    val directoryName: String = "quran_audio",
    val includeReciterSubdir: Boolean = true,
    val fileNameTemplateByKind: Map<AudioKind, String> = DEFAULT_TEMPLATES,
    val fallbackToLastPathSegment: Boolean = true,
    val defaultPolicy: CachePolicy = CachePolicy.CACHE_IF_POSSIBLE,
) {
    companion object {
        val DEFAULT_TEMPLATES = mapOf(
            AudioKind.AYAH to "{sura}_{ayah}.{ext}",
            AudioKind.SURA to "{sura}.{ext}",
            AudioKind.PAGE to "page_{page}.{ext}",
        )
    }
}
