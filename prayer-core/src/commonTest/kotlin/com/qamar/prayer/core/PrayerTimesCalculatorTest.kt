package com.qamar.prayer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrayerTimesCalculatorTest {

    @Test
    fun prayerTimesRaw_returnsValidOrder() {
        val calc = PrayerTimesCalculator(
            calculationMethod = CalculationMethod.MWL,
            timeFormat = TimeFormat.TIME_24,
        )
        val coords = Coordinates(51.5074, -0.1278) // London
        val date = DateComponents(2025, 6, 15)
        val tz = 1.0 // UTC+1
        val raw = calc.prayerTimesRaw(date, coords, tz)
        assertTrue(raw.fajr < raw.sunrise)
        assertTrue(raw.sunrise < raw.dhuhr)
        assertTrue(raw.dhuhr < raw.asr)
        assertTrue(raw.asr < raw.maghrib)
        assertTrue(raw.maghrib < raw.isha)
    }

    @Test
    fun prayerTimes_returnsFormattedStrings() {
        val calc = PrayerTimesCalculator(
            calculationMethod = CalculationMethod.MWL,
            timeFormat = TimeFormat.TIME_24,
        )
        val coords = Coordinates(51.5074, -0.1278)
        val date = DateComponents(2025, 6, 15)
        val times = calc.prayerTimes(date, coords, 1.0)
        assertEquals(7, listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.sunset, times.maghrib, times.isha).size)
        assertTrue(times.fajr.matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test
    fun prayerTimesForMonth_returnsCorrectDayCount() {
        val calc = PrayerTimesCalculator(timeFormat = TimeFormat.TIME_24)
        val list = calc.prayerTimesForMonth(2025, 6, Coordinates(40.0, -74.0), -4.0)
        assertEquals(30, list.size)
    }
}
