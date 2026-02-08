package com.qamar.quran.tajweed

/**
 * Public API for Quran tajweed (recitation rules) detection.
 *
 * Use with any Arabic verse text (e.g. from your Quran API or verse.arabicText).
 * Platform code applies colors/spans from the returned [TajweedSpan] list.
 */
object TajweedApi {

    /**
     * Returns tajweed spans for the given Arabic verse text.
     *
     * @param verse Uthmani or Indopak Arabic verse string.
     * @return List of [TajweedSpan] (start, end, rule). Ranges are [start, end) (end exclusive).
     *         Platforms apply their own styling (e.g. ForegroundColorSpan, SpanStyle) per [TajweedRule].
     */
    fun getTajweedSpans(verse: String): List<TajweedSpan> = getTajweedSpansInternal(verse)
}
