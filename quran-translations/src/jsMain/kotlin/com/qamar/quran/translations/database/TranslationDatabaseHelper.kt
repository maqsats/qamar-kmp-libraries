package com.qamar.quran.translations.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val DB_NAME = "qamar-translations-v1"
private const val STORE_NAME = "databases"

@Suppress("UnsafeCastFromDynamic")
actual class TranslationDatabaseHelper actual constructor(platformContext: Any?) {

    actual suspend fun getDriver(translationId: String): SqlDriver? {
        cachedDrivers[translationId]?.let { return it }
        val bytes = idbGet(translationId) ?: return null
        ensureSqlJs()
        val u8 = byteArrayToUint8(bytes)
        val database: dynamic = js("new sqlJsModule.Database(u8)")
        val driver = SqlJsDriver(database)
        cachedDrivers[translationId] = driver
        return driver
    }

    actual suspend fun deleteDatabase(translationId: String): Boolean {
        cachedDrivers.remove(translationId)?.runCatching { close() }
        return runCatching { idbDelete(translationId) }.isSuccess
    }

    actual suspend fun isDatabaseDownloaded(translationId: String): Boolean =
        runCatching { idbHas(translationId) }.getOrDefault(false)

    actual fun getDatabasePath(translationId: String): String =
        "indexeddb://$DB_NAME/$STORE_NAME/$translationId"

    actual suspend fun writeDatabaseBytes(translationId: String, bytes: ByteArray) {
        idbPut(translationId, bytes)
    }

    actual suspend fun decompressIfZip(bytes: ByteArray): ByteArray {
        if (!bytes.isZip()) return bytes
        return runCatching { unzipFirstDb(bytes) }.getOrDefault(bytes)
    }

    private companion object {
        val cachedDrivers = mutableMapOf<String, SqlDriver>()
    }
}

// ---------- IndexedDB ----------

private suspend fun openDb(): dynamic = suspendCancellableCoroutine { cont ->
    val indexedDB: dynamic = js("(typeof indexedDB !== 'undefined') ? indexedDB : null")
    if (indexedDB == null) {
        cont.resumeWithException(IllegalStateException("IndexedDB not available"))
        return@suspendCancellableCoroutine
    }
    val req: dynamic = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = {
        val db: dynamic = req.result
        if (!(db.objectStoreNames.contains(STORE_NAME) as Boolean)) {
            db.createObjectStore(STORE_NAME)
        }
    }
    req.onsuccess = { cont.resume(req.result) }
    req.onerror = { cont.resumeWithException(RuntimeException("IndexedDB open failed: ${req.error}")) }
    req.onblocked = { cont.resumeWithException(RuntimeException("IndexedDB open blocked")) }
}

private suspend fun idbPut(key: String, bytes: ByteArray) {
    val db = openDb()
    suspendCancellableCoroutine<Unit> { cont ->
        val tx: dynamic = db.transaction(STORE_NAME, "readwrite")
        val store: dynamic = tx.objectStore(STORE_NAME)
        val u8: Uint8Array = byteArrayToUint8(bytes)
        val req: dynamic = store.put(u8, key)
        req.onsuccess = { cont.resume(Unit) }
        req.onerror = { cont.resumeWithException(RuntimeException("idb put failed: ${req.error}")) }
    }
}

private suspend fun idbGet(key: String): ByteArray? {
    val db = openDb()
    return suspendCancellableCoroutine { cont ->
        val tx: dynamic = db.transaction(STORE_NAME, "readonly")
        val store: dynamic = tx.objectStore(STORE_NAME)
        val req: dynamic = store.get(key)
        req.onsuccess = {
            val result: dynamic = req.result
            if (result == null || result == undefined) {
                cont.resume(null)
            } else {
                val u8: Uint8Array = if (js("result instanceof Uint8Array") as Boolean) {
                    result.unsafeCast<Uint8Array>()
                } else {
                    js("new Uint8Array(result)").unsafeCast<Uint8Array>()
                }
                cont.resume(uint8ToByteArray(u8))
            }
        }
        req.onerror = { cont.resumeWithException(RuntimeException("idb get failed: ${req.error}")) }
    }
}

private suspend fun idbHas(key: String): Boolean {
    val db = openDb()
    return suspendCancellableCoroutine { cont ->
        val tx: dynamic = db.transaction(STORE_NAME, "readonly")
        val store: dynamic = tx.objectStore(STORE_NAME)
        val req: dynamic = store.getKey(key)
        req.onsuccess = {
            val result: dynamic = req.result
            cont.resume(!(result == null || result == undefined))
        }
        req.onerror = { cont.resumeWithException(RuntimeException("idb has failed: ${req.error}")) }
    }
}

private suspend fun idbDelete(key: String) {
    val db = openDb()
    suspendCancellableCoroutine<Unit> { cont ->
        val tx: dynamic = db.transaction(STORE_NAME, "readwrite")
        val store: dynamic = tx.objectStore(STORE_NAME)
        val req: dynamic = store.delete(key)
        req.onsuccess = { cont.resume(Unit) }
        req.onerror = { cont.resumeWithException(RuntimeException("idb delete failed: ${req.error}")) }
    }
}

private fun byteArrayToUint8(bytes: ByteArray): Uint8Array {
    val out = Uint8Array(bytes.size)
    for (i in bytes.indices) out[i] = bytes[i]
    return out
}

private fun uint8ToByteArray(u8: Uint8Array): ByteArray {
    val out = ByteArray(u8.length)
    for (i in 0 until u8.length) out[i] = u8[i]
    return out
}

// ---------- sql.js loader ----------

private var sqlJsModuleRef: dynamic = null

private suspend fun ensureSqlJs() {
    if (sqlJsModuleRef != null) {
        installModuleAlias(sqlJsModuleRef)
        return
    }
    val mod = suspendCancellableCoroutine<dynamic> { cont ->
        val initSqlJs: dynamic = js("require('sql.js')")
        val opts: dynamic = js("({})")
        opts.locateFile = { file: String, _: String -> "/$file" }
        val p: dynamic = initSqlJs(opts)
        p.then({ m: dynamic -> cont.resume(m) }, { err: dynamic -> cont.resumeWithException(RuntimeException("sql.js init failed: $err")) })
    }
    sqlJsModuleRef = mod
    installModuleAlias(mod)
}

private fun installModuleAlias(mod: dynamic) {
    js("globalThis.sqlJsModule = mod")
}

// ---------- fflate unzip ----------

private fun unzipFirstDb(bytes: ByteArray): ByteArray {
    val fflate: dynamic = js("require('fflate')")
    val u8: Uint8Array = byteArrayToUint8(bytes)
    val files: dynamic = fflate.unzipSync(u8)
    val names: Array<String> = js("Object.keys(files)").unsafeCast<Array<String>>()
    val preferred = names.firstOrNull { n ->
        val lower = n.lowercase()
        lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3")
    }
    val name = preferred
        ?: names.maxByOrNull { (files[it].length as Int) }
        ?: error("ZIP has no entries")
    val content: Uint8Array = files[name].unsafeCast<Uint8Array>()
    return uint8ToByteArray(content)
}

// ---------- minimal SqlDriver backed by sql.js ----------

private class SqlJsDriver(private val db: dynamic) : SqlDriver {

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        val stmt: dynamic = db.prepare(sql)
        try {
            if (binders != null) {
                val ps = SqlJsPreparedStatement()
                ps.binders()
                stmt.bind(ps.toJsArray())
            }
            val cursor = SqlJsCursor(stmt)
            return mapper(cursor)
        } finally {
            try { stmt.free() } catch (_: dynamic) { /* ignore */ }
        }
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> {
        val stmt: dynamic = db.prepare(sql)
        try {
            if (binders != null) {
                val ps = SqlJsPreparedStatement()
                ps.binders()
                stmt.bind(ps.toJsArray())
            }
            stmt.step()
        } finally {
            try { stmt.free() } catch (_: dynamic) { /* ignore */ }
        }
        return QueryResult.Value(0L)
    }

    override fun newTransaction(): QueryResult<Transacter.Transaction> =
        throw UnsupportedOperationException("Transactions not supported in SqlJsDriver")

    override fun currentTransaction(): Transacter.Transaction? = null

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit

    override fun close() {
        try { db.close() } catch (_: dynamic) { /* ignore */ }
    }
}

private class SqlJsPreparedStatement : SqlPreparedStatement {
    private val params = mutableMapOf<Int, Any?>()
    private var maxIndex: Int = -1

    private fun set(index: Int, value: Any?) {
        params[index] = value
        if (index > maxIndex) maxIndex = index
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) { set(index, bytes?.let { byteArrayToUint8(it) }) }
    override fun bindBoolean(index: Int, boolean: Boolean?) { set(index, boolean?.let { if (it) 1 else 0 }) }
    override fun bindDouble(index: Int, double: Double?) { set(index, double) }
    override fun bindLong(index: Int, long: Long?) { set(index, long?.toDouble()) }
    override fun bindString(index: Int, string: String?) { set(index, string) }

    fun toJsArray(): Array<Any?> {
        val arr = arrayOfNulls<Any?>(maxIndex + 1)
        for ((idx, value) in params) arr[idx] = value
        return arr
    }
}

private class SqlJsCursor(private val stmt: dynamic) : SqlCursor {
    private var currentRow: Array<dynamic>? = null

    override fun next(): QueryResult<Boolean> {
        val hasRow = stmt.step() as Boolean
        currentRow = if (hasRow) stmt.get().unsafeCast<Array<dynamic>>() else null
        return QueryResult.Value(hasRow)
    }

    override fun getBoolean(index: Int): Boolean? = getLong(index)?.let { it != 0L }

    override fun getBytes(index: Int): ByteArray? {
        val v = currentRow?.get(index)
        if (v == null || v == undefined) return null
        if (js("v instanceof Uint8Array") as Boolean) return uint8ToByteArray(v.unsafeCast<Uint8Array>())
        return null
    }

    override fun getDouble(index: Int): Double? {
        val v = currentRow?.get(index)
        if (v == null || v == undefined) return null
        return (v as? Number)?.toDouble()
    }

    override fun getLong(index: Int): Long? {
        val v = currentRow?.get(index)
        if (v == null || v == undefined) return null
        return (v as? Number)?.toLong()
    }

    override fun getString(index: Int): String? {
        val v = currentRow?.get(index)
        if (v == null || v == undefined) return null
        return v as? String
    }
}
