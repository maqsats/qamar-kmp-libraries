package com.qamar.quran.core.database

import com.qamar.quran.core.resource.ResourceReader
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The bundled, prepopulated quran.db must declare a SQLite `PRAGMA user_version`
 * equal to [QuranDatabase.Schema] version.
 *
 * If it ships at the SQLite default of 0, the native (iOS) and Android SQLDelight
 * drivers treat the already-populated file as brand-new and run `Schema.create()`,
 * which executes `CREATE TABLE verses` on a DB that already has it and crashes with
 * "table verses already exists" (see tools/generate_quran_db.build_sqlite_db).
 *
 * user_version is a 4-byte big-endian integer at offset 60 of the SQLite header.
 */
class BundledDatabaseVersionTest {

    @Test
    fun bundledDb_userVersion_matchesSchemaVersion() {
        val header = ResourceReader(null).readBytes("databases/quran.db")
        val userVersion =
            ((header[60].toInt() and 0xFF) shl 24) or
                ((header[61].toInt() and 0xFF) shl 16) or
                ((header[62].toInt() and 0xFF) shl 8) or
                (header[63].toInt() and 0xFF)

        assertEquals(
            QuranDatabase.Schema.version.toInt(),
            userVersion,
            "Bundled quran.db user_version must equal the SQLDelight schema version, " +
                "otherwise the native/Android drivers re-run Schema.create() and crash.",
        )
    }
}
