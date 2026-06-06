@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.qamar.quran.translations.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise

private const val DB_NAME = "qamar-translations-v1"
private const val STORE_NAME = "databases"

/**
 * Wasm/web actual. Same design as the JS actual (downloaded translation DBs are
 * cached in IndexedDB and read through a synchronous sql.js-backed [SqlDriver]),
 * rewritten for Kotlin/Wasm: there is no `dynamic`, so the callback-based
 * IndexedDB + sql.js interop is expressed through typed `external` interfaces and
 * `js(...)` helpers. The async IndexedDB calls are pushed into JS Promises and
 * awaited, which keeps the Kotlin side free of JS event-handler plumbing.
 */
actual class TranslationDatabaseHelper actual constructor(platformContext: Any?) {

    actual suspend fun getDriver(translationId: String): SqlDriver? {
        cachedDrivers[translationId]?.let { return it }
        val stored = idbGet(DB_NAME, STORE_NAME, translationId).await() ?: return null
        ensureSqlJs()
        val database = newSqlJsDatabase(asUint8(stored))
        val driver = SqlJsDriver(database)
        cachedDrivers[translationId] = driver
        return driver
    }

    actual suspend fun deleteDatabase(translationId: String): Boolean {
        cachedDrivers.remove(translationId)?.runCatching { close() }
        return runCatching { idbDelete(DB_NAME, STORE_NAME, translationId).await() }.isSuccess
    }

    actual suspend fun isDatabaseDownloaded(translationId: String): Boolean =
        runCatching { idbHas(DB_NAME, STORE_NAME, translationId).await().toBoolean() }.getOrDefault(false)

    actual fun getDatabasePath(translationId: String): String =
        "indexeddb://$DB_NAME/$STORE_NAME/$translationId"

    actual suspend fun writeDatabaseBytes(translationId: String, bytes: ByteArray) {
        idbPut(DB_NAME, STORE_NAME, translationId, byteArrayToUint8(bytes)).await()
    }

    actual suspend fun decompressIfZip(bytes: ByteArray): ByteArray {
        if (!bytes.isZip()) return bytes
        ensureFflate()
        return runCatching { unzipFirstDb(bytes) }.getOrDefault(bytes)
    }

    private companion object {
        val cachedDrivers = mutableMapOf<String, SqlDriver>()
    }
}

// ---------- IndexedDB (callback API wrapped in JS Promises, then awaited) ----------

private fun idbGet(dbName: String, store: String, key: String): Promise<JsAny?> = js(
    """new Promise(function(resolve, reject){
        if (typeof indexedDB === 'undefined') { reject(new Error('IndexedDB not available')); return; }
        var open = indexedDB.open(dbName, 1);
        open.onupgradeneeded = function(){ var db = open.result; if(!db.objectStoreNames.contains(store)){ db.createObjectStore(store); } };
        open.onsuccess = function(){
            var db = open.result;
            var req = db.transaction(store, 'readonly').objectStore(store).get(key);
            req.onsuccess = function(){ var r = req.result; resolve(r == null ? null : (r instanceof Uint8Array ? r : new Uint8Array(r))); };
            req.onerror = function(){ reject(req.error); };
        };
        open.onerror = function(){ reject(open.error); };
    })"""
)

private fun idbPut(dbName: String, store: String, key: String, value: Uint8Array): Promise<JsAny?> = js(
    """new Promise(function(resolve, reject){
        var open = indexedDB.open(dbName, 1);
        open.onupgradeneeded = function(){ var db = open.result; if(!db.objectStoreNames.contains(store)){ db.createObjectStore(store); } };
        open.onsuccess = function(){
            var db = open.result;
            var req = db.transaction(store, 'readwrite').objectStore(store).put(value, key);
            req.onsuccess = function(){ resolve(null); };
            req.onerror = function(){ reject(req.error); };
        };
        open.onerror = function(){ reject(open.error); };
    })"""
)

private fun idbHas(dbName: String, store: String, key: String): Promise<JsBoolean> = js(
    """new Promise(function(resolve, reject){
        var open = indexedDB.open(dbName, 1);
        open.onupgradeneeded = function(){ var db = open.result; if(!db.objectStoreNames.contains(store)){ db.createObjectStore(store); } };
        open.onsuccess = function(){
            var db = open.result;
            var req = db.transaction(store, 'readonly').objectStore(store).getKey(key);
            req.onsuccess = function(){ var r = req.result; resolve(!(r == null || r === undefined)); };
            req.onerror = function(){ reject(req.error); };
        };
        open.onerror = function(){ reject(open.error); };
    })"""
)

private fun idbDelete(dbName: String, store: String, key: String): Promise<JsAny?> = js(
    """new Promise(function(resolve, reject){
        var open = indexedDB.open(dbName, 1);
        open.onupgradeneeded = function(){ var db = open.result; if(!db.objectStoreNames.contains(store)){ db.createObjectStore(store); } };
        open.onsuccess = function(){
            var db = open.result;
            var req = db.transaction(store, 'readwrite').objectStore(store).delete(key);
            req.onsuccess = function(){ resolve(null); };
            req.onerror = function(){ reject(req.error); };
        };
        open.onerror = function(){ reject(open.error); };
    })"""
)

// ---------- Uint8Array <-> ByteArray ----------

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

private var sqlJsReady = false

private suspend fun ensureSqlJs() {
    if (sqlJsReady) return
    initSqlJsAndAlias().await()
    sqlJsReady = true
}

// Kotlin/Wasm emits an ES module (loaded via `<link rel="modulepreload">`), where the
// CommonJS `require` global does not exist — a bare `require('sql.js')` throws
// `ReferenceError: require is not defined`. Load the npm modules with ESM dynamic
// `import()` instead. sql.js ships as CommonJS (`dist/sql-wasm.js`), so its init
// function lands on the namespace's `default` under webpack interop.
private fun initSqlJsAndAlias(): Promise<JsAny?> = js(
    """import('sql.js').then(function(mod){ var init = mod.default || mod; return init({ locateFile: function(file){ return '/' + file; } }); }).then(function(m){ globalThis.sqlJsModule = m; return null; })"""
)

private fun newSqlJsDatabase(data: Uint8Array): SqlJsDatabase =
    js("new globalThis.sqlJsModule.Database(data)")

// ---------- fflate unzip ----------

private fun unzipFirstDb(bytes: ByteArray): ByteArray {
    val files = fflateUnzipSync(byteArrayToUint8(bytes))
    val keys = jsObjectKeys(files)
    val count = jsArrayLength(keys)
    val names = (0 until count).map { jsArrayGetString(keys, it) }
    val preferred = names.firstOrNull { n ->
        val lower = n.lowercase()
        lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3")
    }
    val name = preferred
        ?: names.maxByOrNull { uint8Length(jsGetProp(files, it)) }
        ?: error("ZIP has no entries")
    return uint8ToByteArray(asUint8(jsGetProp(files, name)))
}

// fflate is consumed synchronously by [unzipFirstDb], so the ESM module must be
// loaded (and cached on `globalThis`) before the first unzip. Its browser ESM build
// uses named exports, so `unzipSync` sits directly on the namespace.
private var fflateReady = false

private suspend fun ensureFflate() {
    if (fflateReady) return
    loadFflate().await()
    fflateReady = true
}

private fun loadFflate(): Promise<JsAny?> = js(
    """import('fflate').then(function(mod){ globalThis.fflateModule = mod.default || mod; return null; })"""
)

private fun fflateUnzipSync(data: Uint8Array): JsAny = js("globalThis.fflateModule.unzipSync(data)")
private fun jsObjectKeys(obj: JsAny): JsAny = js("Object.keys(obj)")
private fun jsArrayLength(arr: JsAny): Int = js("arr.length")
private fun jsArrayGetString(arr: JsAny, index: Int): String = js("arr[index]")
private fun jsGetProp(obj: JsAny, key: String): JsAny = js("obj[key]")
private fun uint8Length(v: JsAny): Int = js("v.length")

// ---------- minimal SqlDriver backed by sql.js ----------

private external interface SqlJsStatement : JsAny {
    fun bind(values: JsAny): Boolean
    fun step(): Boolean
    fun get(): JsAny
    fun free(): Boolean
}

private external interface SqlJsDatabase : JsAny {
    fun prepare(sql: String): SqlJsStatement
    fun close()
}

private class SqlJsDriver(private val db: SqlJsDatabase) : SqlDriver {

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        val stmt = db.prepare(sql)
        try {
            if (binders != null) {
                val ps = SqlJsPreparedStatement()
                ps.binders()
                stmt.bind(ps.toJsArray())
            }
            val cursor = SqlJsCursor(stmt)
            return mapper(cursor)
        } finally {
            try { stmt.free() } catch (_: Throwable) { /* ignore */ }
        }
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> {
        val stmt = db.prepare(sql)
        try {
            if (binders != null) {
                val ps = SqlJsPreparedStatement()
                ps.binders()
                stmt.bind(ps.toJsArray())
            }
            stmt.step()
        } finally {
            try { stmt.free() } catch (_: Throwable) { /* ignore */ }
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
        try { db.close() } catch (_: Throwable) { /* ignore */ }
    }
}

private class SqlJsPreparedStatement : SqlPreparedStatement {
    private val params = mutableMapOf<Int, JsAny?>()
    private var maxIndex: Int = -1

    private fun set(index: Int, value: JsAny?) {
        params[index] = value
        if (index > maxIndex) maxIndex = index
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) { set(index, bytes?.let { byteArrayToUint8(it) }) }
    override fun bindBoolean(index: Int, boolean: Boolean?) { set(index, boolean?.let { (if (it) 1.0 else 0.0).toJsNumber() }) }
    override fun bindDouble(index: Int, double: Double?) { set(index, double?.toJsNumber()) }
    override fun bindLong(index: Int, long: Long?) { set(index, long?.toDouble()?.toJsNumber()) }
    override fun bindString(index: Int, string: String?) { set(index, string?.toJsString()) }

    fun toJsArray(): JsAny {
        val arr = newJsArray(maxIndex + 1)
        for ((idx, value) in params) jsArraySet(arr, idx, value)
        return arr
    }
}

private class SqlJsCursor(private val stmt: SqlJsStatement) : SqlCursor {
    private var currentRow: JsAny? = null

    override fun next(): QueryResult<Boolean> {
        val hasRow = stmt.step()
        currentRow = if (hasRow) stmt.get() else null
        return QueryResult.Value(hasRow)
    }

    override fun getBoolean(index: Int): Boolean? = getLong(index)?.let { it != 0L }

    override fun getBytes(index: Int): ByteArray? {
        val row = currentRow ?: return null
        val v = jsArrayGet(row, index) ?: return null
        return if (jsIsUint8Array(v)) uint8ToByteArray(asUint8(v)) else null
    }

    override fun getDouble(index: Int): Double? {
        val row = currentRow ?: return null
        val v = jsArrayGet(row, index) ?: return null
        return if (jsTypeofNumber(v)) jsToDouble(v) else null
    }

    override fun getLong(index: Int): Long? {
        val row = currentRow ?: return null
        val v = jsArrayGet(row, index) ?: return null
        return if (jsTypeofNumber(v)) jsToDouble(v).toLong() else null
    }

    override fun getString(index: Int): String? {
        val row = currentRow ?: return null
        val v = jsArrayGet(row, index) ?: return null
        return if (jsTypeofString(v)) jsToString(v) else null
    }
}

private fun newJsArray(size: Int): JsAny = js("new Array(size)")
private fun jsArraySet(arr: JsAny, index: Int, value: JsAny?): Unit = js("arr[index] = value")
private fun jsArrayGet(arr: JsAny, index: Int): JsAny? = js("arr[index]")
private fun jsIsUint8Array(v: JsAny): Boolean = js("v instanceof Uint8Array")
private fun jsTypeofNumber(v: JsAny): Boolean = js("typeof v === 'number'")
private fun jsTypeofString(v: JsAny): Boolean = js("typeof v === 'string'")
private fun jsToDouble(v: JsAny): Double = js("v")
private fun jsToString(v: JsAny): String = js("v")
private fun asUint8(v: JsAny): Uint8Array = js("v")
