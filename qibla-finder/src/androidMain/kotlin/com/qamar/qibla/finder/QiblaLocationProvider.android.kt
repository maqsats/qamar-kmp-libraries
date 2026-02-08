package com.qamar.qibla.finder

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

@SuppressLint("MissingPermission")
actual class QiblaLocationProvider actual constructor(
    actual val platformContext: Any?,
) {
    private val locationManager = (platformContext as? Context)
        ?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    actual val isSupported: Boolean = locationManager != null

    actual fun getCurrentLocation(callback: (QiblaLocation?) -> Unit) {
        val manager = locationManager ?: run {
            callback(null)
            return
        }
        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            val lastKnown = providers
                .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
                .maxByOrNull { location -> location.time }
            if (lastKnown != null) {
                callback(QiblaLocation(lastKnown.latitude, lastKnown.longitude))
                return
            }
            val provider = providers.firstOrNull { manager.isProviderEnabled(it) }
            if (provider == null) {
                callback(null)
                return
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    callback(QiblaLocation(location.latitude, location.longitude))
                    manager.removeUpdates(this)
                }

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            callback(null)
        }
    }
}
