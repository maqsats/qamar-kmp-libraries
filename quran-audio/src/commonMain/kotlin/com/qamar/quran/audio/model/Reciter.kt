package com.qamar.quran.audio.model

data class Reciter(
    val id: String,
    val name: String,
    val meta: Map<String, String> = emptyMap(),
)
