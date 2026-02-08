package com.qamar.prayer.core

/**
 * High-level API for prayer times: pass a [TimeZoneProvider] and get times for any date/location.
 * Each platform (androidMain, desktopMain, iosMain, jsMain) provides a default TimeZoneProvider implementation.
 */
class PrayerTimeApi(
    private val calculator: PrayerTimesCalculator = PrayerTimesCalculator(),
    private val timeZoneProvider: TimeZoneProvider,
) {
    /** Prayer times for one day at the given coordinates (timezone from provider). */
    fun getPrayerTimes(date: DateComponents, latitude: Double, longitude: Double): PrayerTimes {
        val tz = timeZoneProvider.timeZoneOffsetHours(latitude, longitude)
        return calculator.prayerTimes(date, Coordinates(latitude, longitude), tz)
    }

    /** Prayer times for each day of the month. */
    fun getPrayerTimesForMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
    ): List<PrayerTimes> {
        val tz = timeZoneProvider.timeZoneOffsetHours(latitude, longitude)
        return calculator.prayerTimesForMonth(year, month, Coordinates(latitude, longitude), tz)
    }

    /** Raw decimal-hour times for one day (for custom formatting or tuning). */
    fun getPrayerTimesRaw(
        date: DateComponents,
        latitude: Double,
        longitude: Double
    ): PrayerTimesRaw {
        val tz = timeZoneProvider.timeZoneOffsetHours(latitude, longitude)
        return calculator.prayerTimesRaw(date, Coordinates(latitude, longitude), tz)
    }

    /** Update calculation method, Asr juristic, high-latitude rule, time format, or offsets. */
    fun withCalculator(block: PrayerTimesCalculator.() -> Unit): PrayerTimeApi {
        calculator.block()
        return this
    }
}
