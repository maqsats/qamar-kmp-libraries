package com.qamar.quran.core.model

data class Verse(
    val sura: Int,
    val ayah: Int,
    val arabicText: String,
    val translation: String? = null,
    val transliteration: String? = null,
    val page: Int? = null,
)
