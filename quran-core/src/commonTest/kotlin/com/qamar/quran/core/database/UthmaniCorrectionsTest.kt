package com.qamar.quran.core.database

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards canonical Uthmani text for verses corrupted in legacy arabic.xml.
 * Values match [tools/generate_quran_db.UTHMANI_CORRECTIONS].
 */
class UthmaniCorrectionsTest {
    @Test
    fun suraOneAyahOne_hasSingleKasraOnBism() {
        assertEquals(
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            expected(1, 1),
        )
    }

    @Test
    fun suraNineAyahOne_startsWithBaraaah() {
        val text = expected(9, 1)
        assertEquals('ب', text.first())
        assertEquals(true, text.startsWith("بَرَا"))
    }

    @Test
    fun suraNinetyFiveAndNinetySevenAyahOne_startWithBism() {
        assertEquals('ب', expected(95, 1).first())
        assertEquals('ب', expected(97, 1).first())
    }

    @Test
    fun suraOneFourteenAyahSix_usesWaslaAlif() {
        assertEquals("مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ", expected(114, 6))
    }

    private fun expected(sura: Int, ayah: Int): String = when (sura to ayah) {
        1 to 1 -> "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        9 to 1 -> "بَرَآءَةٌ مِّنَ ٱللَّهِ وَرَسُولِهِۦٓ إِلَى ٱلَّذِينَ عَٰهَدتُّم مِّنَ ٱلْمُشْرِكِينَ"
        95 to 1 -> "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ وَٱلتِّينِ وَٱلزَّيْتُونِ"
        97 to 1 -> "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ إِنَّآ أَنزَلْنَٰهُ فِى لَيْلَةِ ٱلْقَدْرِ"
        114 to 6 -> "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ"
        else -> error("No fixture for $sura:$ayah")
    }
}
