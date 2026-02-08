package com.qamar.quran.audio.config


data class NetworkConfig(
    val validateUrls: Boolean = false,
    val connectTimeoutMs: Long = 15_000,
    val requestTimeoutMs: Long = 30_000,
)
