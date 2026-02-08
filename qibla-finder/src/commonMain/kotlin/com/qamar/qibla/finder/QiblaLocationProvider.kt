package com.qamar.qibla.finder

expect class QiblaLocationProvider(platformContext: Any?) {
    val platformContext: Any?
    val isSupported: Boolean

    fun getCurrentLocation(callback: (QiblaLocation?) -> Unit)
}
