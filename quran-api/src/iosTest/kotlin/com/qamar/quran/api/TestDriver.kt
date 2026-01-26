package com.qamar.quran.api

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.qamar.quran.core.database.QuranDatabase

actual fun createInMemoryDriver(): SqlDriver {
    return NativeSqliteDriver(QuranDatabase.Schema.synchronous(), ":memory:")
}
