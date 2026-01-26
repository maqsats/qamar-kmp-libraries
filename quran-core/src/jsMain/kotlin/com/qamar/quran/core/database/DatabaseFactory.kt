package com.qamar.quran.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.qamar.quran.core.database.QuranDatabase
import com.qamar.quran.core.resource.ResourceReader
import org.w3c.dom.Worker

actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(null)

    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val worker = Worker(
            js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""),
        )
        val driver = WebWorkerDriver(worker)
        QuranDatabase.Schema.awaitCreate(driver)
        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}
