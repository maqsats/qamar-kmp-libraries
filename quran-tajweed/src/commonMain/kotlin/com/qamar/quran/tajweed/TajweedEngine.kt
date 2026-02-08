package com.qamar.quran.tajweed

// Precompiled regex patterns (moved to top level for reuse)
private val GUNNAH_REGEX = Regex("[$NUN|$MIM]$SHADDA")

private val QALQALA_REGEX = Regex(
    "[$QAF$TOA$BA$ZIM$DAL](?:[$SUKUN$CURVY_SUKUN]|[^$SUKUN]?[^$EMPTY_ALIF$EMPTY_YA$ALIF_HAMZA]$)"
)

private val IDGHAM_REGEX = Regex(
    "[$NUN$FATHATAIN$DAMMATAIN$KASRATAIN$MINE_FATHATAN$MINE_DAMMATAN$MINE_KASRATAN]" +
            "[$SUKUN$CURVY_SUKUN$EMPTY_YA$EMPTY_ALIF]?" +
            "[$UTHMANI_STOP_SIGNS]?" +
            "$SPACE[$NUN$MIM$ANOTHER_YA$WOW]$SHADDA?" +
            "|$MIM[$UTHMANI_STOP_SIGNS$SUKUN$CURVY_SUKUN]?$SPACE$MIM"
)

private val IDGHAM_WITHOUT_GHUNNA_REGEX = Regex(
    "[$NUN$KASRATAIN$FATHATAIN$DAMMATAIN$MINE_FATHATAN$MINE_DAMMATAN$MINE_KASRATAN]" +
            "[$SUKUN$CURVY_SUKUN$EMPTY_YA$EMPTY_ALIF]?" +
            "[$UTHMANI_STOP_SIGNS]?" +
            "$SPACE[$RA$LAM]"
)

private val IKHFA_REGEX = Regex(
    "[$NUN$KASRATAIN$FATHATAIN$DAMMATAIN$MINE_FATHATAN$MINE_DAMMATAN$MINE_KASRATAN]" +
            "[$SUKUN$CURVY_SUKUN$EMPTY_YA$EMPTY_ALIF]?" +
            "[$UTHMANI_STOP_SIGNS]?" +
            "$SPACE?" +
            "[$SOAD$ZAAL$THA$KAF$ZIM$SHIN$QAF$SEEN$DAL$TOA$ZHA$FA$TA$DOAD$ZOA$INDOPAK_KAF]" +
            "|$MIM[$SUKUN$CURVY_SUKUN]?$SPACE?$BA"
)

private val ONE_MAD_REGEX = Regex("[$NUN|$MIM]$EMPTY_YA")
private val MAD_REGEX = Regex("[$MAD]")
private val MAD_HEY_REGEX = Regex("[$HEY]")

// Use arrays instead of sets for small collections (faster lookup for 2-6 items)
private val TOP_CHARS_THAT_CAUSE_ERROR = charArrayOf(MINE_FATHATAN, MINE_KASRATAN)
private val TANWEEN_CHARS = charArrayOf(
    FATHATAIN, DAMMATAIN, KASRATAIN,
    MINE_DAMMATAN, MINE_FATHATAN, MINE_KASRATAN
)
private val DIACRITICS_CHARS = charArrayOf(
    DHAMMA, SHADDA, KASRA, FATHAH,
    FATHATAIN, DAMMATAIN, KASRATAIN,
    MINE_DAMMATAN, MINE_FATHATAN, MINE_KASRATAN, SUKUN
)

// Inline helper for array contains check
@Suppress("NOTHING_TO_INLINE")
private inline fun CharArray.containsChar(c: Char): Boolean {
    for (element in this) {
        if (element == c) return true
    }
    return false
}

private fun adjustedStart(verse: String, startIndex: Int): Int {
    return if (startIndex in verse.indices && TOP_CHARS_THAT_CAUSE_ERROR.containsChar(verse[startIndex])) {
        getStart(verse, startIndex) - 1
    } else {
        getStart(verse, startIndex)
    }
}

internal fun getTajweedSpansInternal(verse: String): List<TajweedSpan> {
    if (verse.isEmpty()) {
        return emptyList()
    }

    // Pre-allocate list with estimated capacity to reduce allocations
    val spans = ArrayList<TajweedSpan>(verse.length / 4)
    val length = verse.length

    // Process MAD patterns
    MAD_REGEX.findAll(verse).forEach { match ->
        addSpan(spans, match.range.first, match.range.last + 1, TajweedRule.MAD, length)
    }

    // Process GUNNAH patterns
    GUNNAH_REGEX.findAll(verse).forEach { match ->
        val start = getStart(verse, match.range.first)
        val end = getEnd(verse, match.range.last + 1)
        addSpan(spans, start, end, TajweedRule.GHUNNA, length)
    }

    // Process IQLAB patterns - consolidated logic
    processIqlabPatterns(verse, spans, length)

    // Process HAMZAT_WASL patterns
    processHamzatWaslPatterns(verse, spans, length)

    // Process MAD_HEY patterns
    MAD_HEY_REGEX.findAll(verse).forEach { match ->
        val start = if (match.range.first > 0) match.range.first - 1 else 0
        addSpan(spans, start, match.range.last + 1, TajweedRule.MAD_HEY, length)
    }

    // Process QALQALA patterns
    QALQALA_REGEX.findAll(verse).forEach { match ->
        addSpan(spans, match.range.first, match.range.last + 1, TajweedRule.QALQALA, length)
    }

    // Process IDGHAM patterns
    IDGHAM_REGEX.findAll(verse).forEach { match ->
        addSpan(
            spans,
            adjustedStart(verse, match.range.first),
            getEnd(verse, match.range.last + 1),
            TajweedRule.IDGHAM,
            length
        )
    }

    // Process IDGHAM_WITHOUT_GHUNNA patterns
    IDGHAM_WITHOUT_GHUNNA_REGEX.findAll(verse).forEach { match ->
        addSpan(
            spans,
            adjustedStart(verse, match.range.first),
            (match.range.last + 3).coerceAtMost(length),
            TajweedRule.IDGHAM_WITHOUT_GHUNNA,
            length
        )
    }

    // Process IKHFA patterns
    IKHFA_REGEX.findAll(verse).forEach { match ->
        addSpan(
            spans,
            adjustedStart(verse, match.range.first),
            getEnd(verse, match.range.last + 1),
            TajweedRule.IKHFA,
            length
        )
    }

    // Process ONE_MAD patterns
    ONE_MAD_REGEX.findAll(verse).forEach { match ->
        val startIndex = getStart(verse, match.range.first)
        val end = getEnd(verse, match.range.last + 1)
        addSpan(spans, startIndex, end, TajweedRule.ONE_MAD, length)
    }

    // Process MADDAH patterns
    processMaddahPatterns(verse, spans, length)

    return spans
}

// Consolidated IQLAB pattern processing
private fun processIqlabPatterns(verse: String, spans: MutableList<TajweedSpan>, length: Int) {
    // IQLABUN pattern
    var start = verse.indexOf(IQLABUN)
    while (start >= 0) {
        if (start > 0 && start < length - 2) {
            val previous = verse[start - 1]
            val previousMatches = previous == NUN || TANWEEN_CHARS.containsChar(previous)

            if (previousMatches) {
                when {
                    start + 1 < length && verse[start + 1] == BA -> {
                        addSpan(spans, start - 1, start + 3, TajweedRule.IQLAB, length)
                    }

                    start + 2 < length && verse[start + 1] == SPACE && verse[start + 2] == BA -> {
                        addSpan(spans, start - 1, start + 4, TajweedRule.IQLAB, length)
                    }

                    start + 3 < length && verse[start + 1] == ALIF_HAMZA &&
                            verse[start + 2] == SPACE && verse[start + 3] == BA -> {
                        addSpan(spans, start - 1, start + 4, TajweedRule.IQLAB, length)
                    }
                }
            }
        }
        start = verse.indexOf(IQLABUN, start + 1)
    }

    // IQLABUN2 pattern
    start = verse.indexOf(IQLABUN2)
    while (start >= 0) {
        if (start > 0 && start < length - 2) {
            if (start + 2 < length && verse[start + 1] == SPACE && verse[start + 2] == BA) {
                addSpan(spans, start, start + 4, TajweedRule.IQLAB, length)
            }

            val previous = verse[start - 1]
            val previousMatches = previous == NUN || DIACRITICS_CHARS.containsChar(previous)

            if (previousMatches && start + 2 < length &&
                verse[start + 1] == SPACE && verse[start + 2] == BA
            ) {
                addSpan(spans, start - 1, start + 4, TajweedRule.IQLAB, length)
            }
        }
        start = verse.indexOf(IQLABUN2, start + 1)
    }

    // ALSO_IQLAB pattern
    start = verse.indexOf(ALSO_IQLAB)
    while (start >= 0) {
        if (start > 0 && start < length - 2) {
            addSpan(spans, start, start + 4, TajweedRule.IQLAB, length)
        }
        start = verse.indexOf(ALSO_IQLAB, start + 1)
    }
}

// Consolidated HAMZAT_WASL pattern processing
private fun processHamzatWaslPatterns(verse: String, spans: MutableList<TajweedSpan>, length: Int) {
    var start = verse.indexOf(OQYLMAITYN)
    while (start >= 0) {
        if (start > 0 && start < length - 2) {
            val next = verse[start + 1]
            val nextNext = verse[start + 2]

            val spanLength = if (next == LAM && next != nextNext &&
                nextNext != SHADDA && nextNext != SUKUN
            ) 2 else 1

            addSpan(spans, start, start + spanLength, TajweedRule.HAMZAT_WASL, length)
        }
        start = verse.indexOf(OQYLMAITYN, start + 1)
    }

    start = verse.indexOf(OQYLMAITYN2)
    while (start >= 0) {
        addSpan(spans, start, start + 2, TajweedRule.HAMZAT_WASL, length)
        start = verse.indexOf(OQYLMAITYN2, start + 1)
    }
}

// Consolidated MADDAH pattern processing
private fun processMaddahPatterns(verse: String, spans: MutableList<TajweedSpan>, length: Int) {
    if (length > 4) {
        val checkChar = verse[length - 3]
        if (checkChar == ANOTHER_YA || checkChar == ALIF_HAMZA || checkChar == WOW) {
            addSpan(spans, length - 3, length - 2, TajweedRule.MADDAH, length)
        }
    }

    verse.forEachIndexed { i, c ->
        if (c == SOZU) {
            addSpan(spans, i, i + 1, TajweedRule.MADDAH, length)
        }
    }
}

private fun getEnd(text: String, endExclusive: Int): Int {
    val length = text.length

    // Early return for short text
    if (length <= 6) return length

    // Check current position for SHADDA
    if (endExclusive in 0 until length && text[endExclusive] == SHADDA) {
        val idx = endExclusive + 2
        return if (idx < length && text[idx] == SUPERCRIPT_ALIF_KHARA_FATHA) {
            (endExclusive + 3).coerceAtMost(length)
        } else {
            (endExclusive + 2).coerceAtMost(length)
        }
    }

    // Check next position
    val nextIndex = endExclusive + 1
    return if (nextIndex < length &&
        (text[nextIndex] == SUPERCRIPT_ALIF_KHARA_FATHA || text[nextIndex] == SHADDA)
    ) {
        (endExclusive + 2).coerceAtMost(length)
    } else {
        (endExclusive + 1).coerceAtMost(length)
    }
}

private fun getStart(text: String, start: Int): Int {
    if (start !in text.indices) return start

    return when (text[start]) {
        FATHATAIN, DAMMATAIN, KASRATAIN -> {
            val previous = start - 1
            if (previous >= 0 && text[previous] == SHADDA) {
                (start - 2).coerceAtLeast(0)
            } else {
                (start - 1).coerceAtLeast(0)
            }
        }

        else -> start
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun addSpan(
    spans: MutableList<TajweedSpan>,
    start: Int,
    end: Int,
    rule: TajweedRule,
    length: Int,
) {
    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(safeStart, length)

    if (safeStart < safeEnd) {
        spans.add(TajweedSpan(safeStart, safeEnd, rule))
    }
}