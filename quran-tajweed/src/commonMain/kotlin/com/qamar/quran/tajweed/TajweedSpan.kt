package com.qamar.quran.tajweed

/**
 * Range is [start, end) where end is exclusive, matching Kotlin/Java substring conventions.
 */
data class TajweedSpan(
    val start: Int,
    val end: Int,
    val rule: TajweedRule,
)
