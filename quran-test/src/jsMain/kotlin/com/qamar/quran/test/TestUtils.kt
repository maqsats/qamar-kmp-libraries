package com.qamar.quran.test

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@Suppress("UNCHECKED_CAST")
actual fun <T> runTest(block: suspend () -> T): T {
    return GlobalScope.promise { block() } as T
}
