package com.qamar.quran.api

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.qamar.quran.core.database.QuranDatabase
import kotlinx.coroutines.runBlocking

actual fun createInMemoryDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    runBlocking {
        QuranDatabase.Schema.awaitCreate(driver)
    }
    return driver
}
