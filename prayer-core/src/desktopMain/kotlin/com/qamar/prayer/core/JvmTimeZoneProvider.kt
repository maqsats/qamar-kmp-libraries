package com.qamar.prayer.core

import java.util.TimeZone
import kotlin.math.floor

/**
 * JVM implementation using java.util.TimeZone. Uses raw offset at the given instant.
 */
class JvmTimeZoneProvider(private val instantMillis: Long = System.currentTimeMillis()) :
    TimeZoneProvider {
    override fun timeZoneOffsetHours(lat: Double, lon: Double): Double {
        // No geolocation lookup; just use the default zone.
        val tz = TimeZone.getDefault()
        val offsetMillis = tz.getOffset(instantMillis).toDouble()
        val hours = floor(offsetMillis / 3_600_000.0)
        val minutes = (offsetMillis / 60_000.0) - (hours * 60.0)
        return hours + minutes / 60.0
    }
}
