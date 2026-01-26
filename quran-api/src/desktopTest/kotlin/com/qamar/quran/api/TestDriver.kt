package com.qamar.quran.api

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.qamar.quran.core.database.QuranDatabase

actual suspend fun createInMemoryDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QuranDatabase.Schema.awaitCreate(driver)
    return driver
}
