package com.qamar.quran.api

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.qamar.quran.core.database.QuranDatabase
import org.w3c.dom.Worker

actual suspend fun createInMemoryDriver(): SqlDriver {
    val worker = Worker(
        js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""),
    )
    val driver = WebWorkerDriver(worker)
    QuranDatabase.Schema.awaitCreate(driver)
    return driver
}
