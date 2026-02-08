package com.qamar.prayer.core

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn

/**
 * TimeZoneProvider that uses the device's current system time zone (via kotlinx-datetime).
 * Works on all platforms: Android, iOS, JS, Desktop.
 */
class DefaultTimeZoneProvider : TimeZoneProvider {
    override fun timeZoneOffsetHours(lat: Double, lon: Double): Double {
        val tz = TimeZone.currentSystemDefault()
        val now = kotlin.time.Clock.System.now()
        val offset = now.offsetIn(tz)
        return offset.totalSeconds.toDouble() / 3600.0
    }
}
