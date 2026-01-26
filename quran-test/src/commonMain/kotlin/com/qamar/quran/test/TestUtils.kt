package com.qamar.quran.test

/**
 * Multiplatform test utility to run suspend functions in tests.
 * On JVM/Android/Desktop/iOS: uses runBlocking
 * On JS: returns a Promise that test frameworks can handle
 */
expect fun <T> runTest(block: suspend () -> T): T
