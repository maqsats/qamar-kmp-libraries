package com.qamar.quran.translations.model

/** A verse whose translation text matched a search query. */
data class TranslationSearchHit(
    val sura: Int,
    val ayah: Int,
    val text: String,
)
