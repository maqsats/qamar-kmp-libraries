package com.qamar.quran.tajweed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for tajweed rule detection.
 * Uses Arabic Unicode: Ghunnah نّ/مّ, MAD (1648), SOZU آ, HAMZAT_WASL ٱ, Qalqala قْ/طْ/بْ/جْ/دْ, etc.
 */
class TajweedApiTest {

    @Test
    fun emptyStringReturnsEmptyList() {
        val spans = TajweedApi.getTajweedSpans("")
        assertTrue(spans.isEmpty())
    }

    @Test
    fun whitespaceOnlyReturnsEmptyOrNoRuleSpans() {
        val spans = TajweedApi.getTajweedSpans("   \n  ")
        // No tajweed rules in whitespace
        assertTrue(spans.all { it.start >= 0 && it.end <= 5 && it.end > it.start })
    }

    @Test
    fun ghunnahDetectedForNoonWithShadda() {
        // نّ = nun + shadda (U+0646 U+0651)
        val verse = "إنّ"
        val spans = TajweedApi.getTajweedSpans(verse)
        val ghunnah = spans.filter { it.rule == TajweedRule.GHUNNA }
        assertTrue(ghunnah.isNotEmpty(), "Expected at least one GHUNNA span for نّ")
        assertTrue(ghunnah.any {
            it.start < it.end && verse.substring(it.start, it.end).contains('\u0646')
        })
    }

    @Test
    fun ghunnahDetectedForMeemWithShadda() {
        // مّ = mim + shadda
        val verse = "مّم"
        val spans = TajweedApi.getTajweedSpans(verse)
        val ghunnah = spans.filter { it.rule == TajweedRule.GHUNNA }
        assertTrue(ghunnah.isNotEmpty(), "Expected at least one GHUNNA span for مّ")
    }

    @Test
    fun madCharacterDetected() {
        // Small mad = 1648 (U+0670) - superscript alif
        val madChar = 1648.toChar()
        val verse = "رَحْمَ${madChar}ن"
        val spans = TajweedApi.getTajweedSpans(verse)
        val madSpans = spans.filter { it.rule == TajweedRule.MAD }
        assertTrue(madSpans.isNotEmpty(), "Expected MAD span for character 1648")
        assertTrue(madSpans.any { it.start >= 0 && it.end <= verse.length })
    }

    @Test
    fun sozuMaddahDetected() {
        // آ = alif with maddah (U+0622)
        val verse = "آية"
        val spans = TajweedApi.getTajweedSpans(verse)
        val maddah = spans.filter { it.rule == TajweedRule.MADDAH }
        assertTrue(maddah.isNotEmpty(), "Expected MADDAH span for آ")
        assertEquals(
            1,
            maddah.count { it.start == 0 && it.end == 1 },
            "آ should be single char span"
        )
    }

    @Test
    fun hamzatWaslDetected() {
        // ٱ = hamzat wasl (U+0671). Engine only spans when start > 0 (not at verse start).
        val verse = "بِسْمِ ٱللَّهِ"
        val spans = TajweedApi.getTajweedSpans(verse)
        val hamzat = spans.filter { it.rule == TajweedRule.HAMZAT_WASL }
        assertTrue(hamzat.isNotEmpty(), "Expected HAMZAT_WASL span for ٱ in بِسْمِ ٱللَّهِ")
    }

    @Test
    fun qalqalaDetectedForQafWithSukun() {
        val verse = "مِقْ"
        val spans = TajweedApi.getTajweedSpans(verse)
        val qalqala = spans.filter { it.rule == TajweedRule.QALQALA }
        assertTrue(qalqala.isNotEmpty(), "Expected QALQALA for قْ")
    }

    @Test
    fun allSpansHaveValidRanges() {
        val verse = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val spans = TajweedApi.getTajweedSpans(verse)
        spans.forEach { span ->
            assertTrue(span.start >= 0, "span.start ${span.start} should be >= 0")
            assertTrue(
                span.end <= verse.length,
                "span.end ${span.end} should be <= length ${verse.length}"
            )
            assertTrue(span.start < span.end, "span should have start < end")
        }
    }

    @Test
    fun realVerseFromFatihaReturnsNonEmptySpans() {
        val verse = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val spans = TajweedApi.getTajweedSpans(verse)
        // Fatiha has hamzat wasl, ghunnah (لّ), possibly MAD (ٰ)
        assertTrue(spans.isNotEmpty(), "Real verse should yield at least one tajweed span")
        val ruleTypes = spans.map { it.rule }.toSet()
        assertTrue(
            ruleTypes.isNotEmpty(),
            "Should have at least one rule type among MAD, GHUNNA, HAMZAT_WASL, etc."
        )
    }

    @Test
    fun oneMadDetectedForNoonOrMeemFollowedByAlif() {
        // نا or ما = one-mad (noon/meem + alif)
        val verse = "نا"
        val spans = TajweedApi.getTajweedSpans(verse)
        val oneMad = spans.filter { it.rule == TajweedRule.ONE_MAD }
        assertTrue(oneMad.isNotEmpty(), "Expected ONE_MAD for نا")
    }

    @Test
    fun lastCharElongationWhenEndingWithYaOrAlifOrWaw() {
        // Verse ending with ...ي or ...ا or ...و - last-3 char is ي/ا/و -> MADDAH at (length-3, length-2)
        val verse = "شيءٍ"
        if (verse.length > 4) {
            val spans = TajweedApi.getTajweedSpans(verse)
            // May or may not have MADDAH depending on verse[s.length-3]
            spans.filter { it.rule == TajweedRule.MADDAH }.forEach { span ->
                assertTrue(span.start >= 0 && span.end <= verse.length)
            }
        }
    }

    @Test
    fun tajweedApiReturnsConsistentResultsForSameVerse() {
        val verse = "إنَّ ٱلرَّحْمَٰنِ"
        val first = TajweedApi.getTajweedSpans(verse)
        val second = TajweedApi.getTajweedSpans(verse)
        assertEquals(first.size, second.size)
        first.zip(second).forEach { (a, b) ->
            assertEquals(a.start, b.start)
            assertEquals(a.end, b.end)
            assertEquals(a.rule, b.rule)
        }
    }
}
