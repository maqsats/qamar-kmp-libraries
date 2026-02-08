package com.qamar.qibla.finder

typealias QiblaAzimuthListener = (Float) -> Unit

expect class QiblaCompass(platformContext: Any?) {
    val platformContext: Any?
    val isSupported: Boolean

    fun setListener(listener: QiblaAzimuthListener?)
    fun setAzimuthFix(fix: Float)
    fun start()
    fun stop()
}
