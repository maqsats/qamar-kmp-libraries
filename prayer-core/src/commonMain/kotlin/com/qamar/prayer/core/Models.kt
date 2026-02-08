package com.qamar.prayer.core

/** Simple latitude/longitude holder. */
data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

/** Calendar date components, 1-based month. Platform-specific Calendar conversion is in androidMain/desktopMain. */
data class DateComponents(val year: Int, val month: Int, val day: Int) {
    init {
        require(month in 1..12) { "Month must be 1..12" }
        require(day in 1..31) { "Day must be 1..31" }
        require(year in 1900..2100) { "Year must be within a reasonable range" }
        require(day <= daysInMonth(year, month)) { "Day $day is not valid for $month/$year" }
    }
}

internal fun daysInMonth(year: Int, month: Int): Int {
    val base = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> 28
        else -> 30
    }
    if (month != 2) return base
    val leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    return if (leap) base + 1 else base
}

/** Per-prayer minute offsets (Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha). */
data class PrayerOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val sunset: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    internal fun asMinutesArray(): IntArray =
        intArrayOf(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha)
}

/** Raw times expressed in decimal hours. */
data class PrayerTimesRaw(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val sunset: Double,
    val maghrib: Double,
    val isha: Double,
)

/** Formatted prayer times suitable for display. */
data class PrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val sunset: String,
    val maghrib: String,
    val isha: String,
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
)
