package com.qamar.prayer.core

/**
 * Minimal time zone lookup abstraction so host apps can plug their own source
 * (e.g., device default time zone on all platforms, or a custom implementation).
 */
fun interface TimeZoneProvider {
    /**
     * Return the raw offset from UTC in hours for the given coordinate.
     * Example: UTC+05:30 -> 5.5, UTC-04:00 -> -4.0
     */
    fun timeZoneOffsetHours(lat: Double, lon: Double): Double
}
