package com.qamar.qibla.finder

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

actual class QiblaLocationProvider actual constructor(
    actual val platformContext: Any?,
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    private var pendingCallback: ((QiblaLocation?) -> Unit)? = null

    actual val isSupported: Boolean = CLLocationManager.locationServicesEnabled()

    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    actual fun getCurrentLocation(callback: (QiblaLocation?) -> Unit) {
        if (!isSupported) {
            callback(null)
            return
        }
        pendingCallback = callback
        val status = CLLocationManager.authorizationStatus()
        if (status == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
        }
        locationManager.startUpdatingLocation()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        val result = location?.let {
            QiblaLocation(
                latitude = it.coordinate.useContents { latitude },
                longitude = it.coordinate.useContents { longitude },
            )
        }
        pendingCallback?.invoke(result)
        pendingCallback = null
        locationManager.stopUpdatingLocation()
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        pendingCallback?.invoke(null)
        pendingCallback = null
        locationManager.stopUpdatingLocation()
    }
}
