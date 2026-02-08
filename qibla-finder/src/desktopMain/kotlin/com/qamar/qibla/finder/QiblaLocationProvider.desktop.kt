package com.qamar.qibla.finder

actual class QiblaLocationProvider actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = false

    actual fun getCurrentLocation(callback: (QiblaLocation?) -> Unit) {
        callback(null)
    }
}
