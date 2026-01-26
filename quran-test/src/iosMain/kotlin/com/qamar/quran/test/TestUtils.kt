package com.qamar.quran.test

import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend () -> T): T = runBlocking { block() }
