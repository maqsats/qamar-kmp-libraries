package com.qamar.quran.translations.database

// iOS ZIP extraction is not yet implemented. Translations served as raw .db files
// work on iOS; zipped ones (e.g. quran.kk.altai.db.zip) currently can't be opened
// without a third-party zip library or Compression.framework cinterop.
internal object IosZipExtractor {
    fun extractFirstEntry(bytes: ByteArray): ByteArray? = null
}
