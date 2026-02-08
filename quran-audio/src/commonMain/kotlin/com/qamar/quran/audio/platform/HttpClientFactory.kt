package com.qamar.quran.audio.platform

import io.ktor.client.HttpClient

/**
 * Creates a shared HTTP client. The engine is chosen automatically from the
 * platform dependency (OkHttp on Android, Darwin on iOS, Java on desktop, Js on JS).
 */
fun platformHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
}
