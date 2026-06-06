package com.qamar.qibla.finder

/**
 * Wasm/web has no reliable device-orientation compass in this library's design
 * (same stance as the JS target). Reports unsupported and no-ops.
 */
actual class QiblaCompass actual constructor(
    actual val platformContext: Any?,
) {
    actual val isSupported: Boolean = false

    private var listener: QiblaAzimuthListener? = null
    private var azimuthFix: Float = 0f

    actual fun setListener(listener: QiblaAzimuthListener?) {
        this.listener = listener
    }

    actual fun setAzimuthFix(fix: Float) {
        azimuthFix = fix
    }

    actual fun start() = Unit

    actual fun stop() = Unit
}
