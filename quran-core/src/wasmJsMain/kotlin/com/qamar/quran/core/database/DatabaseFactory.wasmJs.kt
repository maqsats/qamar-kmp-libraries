package com.qamar.quran.core.database

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.qamar.quran.core.resource.ResourceReader
import org.w3c.dom.Worker

/**
 * Wasm/web driver backed by the sql.js Web Worker (same approach as the JS
 * actual). The worker URL is resolved through webpack's `new URL(..., import.meta.url)`
 * asset mechanism so the worker script is emitted into the bundle.
 */
actual class DatabaseFactory actual constructor(platformContext: Any?) {
    actual val platformContext: Any? = platformContext
    private val resourceReader = ResourceReader(null)

    actual suspend fun createDriver(config: DatabaseConfig): SqlDriver {
        val worker = Worker(sqljsWorkerUrl())
        val driver = WebWorkerDriver(worker)
        QuranDatabase.Schema.awaitCreate(driver)
        DatabaseSeeder(driver, resourceReader).seedIfEmpty()
        return driver
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun sqljsWorkerUrl(): String =
    js("""(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)).toString()""")
