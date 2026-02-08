package com.qamar.qibla.finder

import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject

actual class QiblaCompass actual constructor(
    actual val platformContext: Any?,
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    private var listener: QiblaAzimuthListener? = null
    private var azimuthFix: Float = 0f

    actual val isSupported: Boolean = CLLocationManager.headingAvailable()

    init {
        locationManager.delegate = this
        locationManager.headingFilter = 1.0
    }

    actual fun setListener(listener: QiblaAzimuthListener?) {
        this.listener = listener
    }

    actual fun setAzimuthFix(fix: Float) {
        azimuthFix = fix
    }

    actual fun start() {
        if (!isSupported) return
        locationManager.startUpdatingHeading()
    }

    actual fun stop() {
        locationManager.stopUpdatingHeading()
    }

    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
        val rawHeading = if (didUpdateHeading.trueHeading > 0) {
            didUpdateHeading.trueHeading
        } else {
            didUpdateHeading.magneticHeading
        }
        val azimuth = ((rawHeading + azimuthFix + 360.0) % 360.0).toFloat()
        listener?.invoke(azimuth)
    }
}
