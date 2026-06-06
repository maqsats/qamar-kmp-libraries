package com.qamar.qibla.finder

/**
 * Wasm/web has no reliable device geolocation surface in this library's design
 * (same stance as the JS target). Reports unsupported; the host app supplies a
 * location another way when running on the web.
 */
actual class QiblaLocationProvider actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = false

    actual fun getCurrentLocation(callback: (QiblaLocation?) -> Unit) {
        callback(null)
    }
}
