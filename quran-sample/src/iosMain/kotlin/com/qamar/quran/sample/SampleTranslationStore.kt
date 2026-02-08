package com.qamar.quran.sample

actual class SampleTranslationStore {
    actual fun isSupported(): Boolean = false
    actual suspend fun downloadTranslation(url: String, fileName: String): String =
        throw UnsupportedOperationException("Translation download not wired on iOS sample yet")
    actual suspend fun readVerse(path: String, sura: Int, ayah: Int): String? = null
}
