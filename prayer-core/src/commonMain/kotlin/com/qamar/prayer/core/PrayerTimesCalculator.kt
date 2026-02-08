package com.qamar.prayer.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure prayer time calculation engine ported from the legacy Java implementation.
 * It keeps the same defaults (QMDB, Shafii, midnight high-latitude rule, 24h format).
 */
class PrayerTimesCalculator(
    var calculationMethod: CalculationMethod = CalculationMethod.QMDB,
    var asrJuristic: JuristicMethod = JuristicMethod.SHAFII,
    var adjustHighLats: HighLatitudeRule = HighLatitudeRule.MID_NIGHT,
    var timeFormat: TimeFormat = TimeFormat.TIME_24,
    var dhuhrMinutes: Int = 0,
    var offsets: PrayerOffsets = PrayerOffsets(),
    var numIterations: Int = 1,
    var invalidTimeText: String = INVALID_TIME,
) {
    private var customParams: MethodParams = CalculationMethod.CUSTOM.params

    fun setCustomMethod(params: MethodParams) {
        customParams = params
        calculationMethod = CalculationMethod.CUSTOM
    }

    fun prayerTimesRaw(
        date: DateComponents,
        coordinates: Coordinates,
        timeZone: Double,
    ): PrayerTimesRaw {
        val jDate = julianDate(date.year, date.month, date.day)
        val lonDiff = coordinates.longitude / (15.0 * 24.0)
        val baseJd = jDate - lonDiff
        val times = computeDayTimes(baseJd, coordinates, timeZone)
        return PrayerTimesRaw(
            fajr = times[FAJR],
            sunrise = times[SUNRISE],
            dhuhr = times[DHUHR],
            asr = times[ASR],
            sunset = times[SUNSET],
            maghrib = times[MAGHRIB],
            isha = times[ISHA],
        )
    }

    fun prayerTimes(
        date: DateComponents,
        coordinates: Coordinates,
        timeZone: Double,
        format: TimeFormat = timeFormat,
    ): PrayerTimes {
        val raw = prayerTimesRaw(date, coordinates, timeZone)
        val formatted = adjustTimesFormat(raw.toArray(), format)
        return formatted.copy(day = date.day, month = date.month, year = date.year)
    }

    fun prayerTimesForMonth(
        year: Int,
        month: Int,
        coordinates: Coordinates,
        timeZone: Double,
        format: TimeFormat = timeFormat,
    ): List<PrayerTimes> {
        val days = daysInMonth(year, month)
        return (1..days).map { day ->
            prayerTimes(DateComponents(year, month, day), coordinates, timeZone, format)
        }
    }

    private fun currentParams(): MethodParams =
        if (calculationMethod == CalculationMethod.CUSTOM) customParams else calculationMethod.params

    private fun computeDayTimes(
        jDate: Double,
        coordinates: Coordinates,
        timeZone: Double
    ): DoubleArray {
        var times = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0, 18.0)
        repeat(numIterations) {
            times = computeTimes(jDate, coordinates, times)
        }
        times = adjustTimes(times, coordinates, timeZone)
        times = tuneTimes(times)
        return times
    }

    private fun computeTimes(
        jDate: Double,
        coordinates: Coordinates,
        times: DoubleArray
    ): DoubleArray {
        val t = dayPortion(times)
        val params = currentParams()

        val fajr = computeTime(180 - params.fajrAngle, jDate, coordinates, t[FAJR])
        val sunrise = computeTime(180 - SUNRISE_ANGLE, jDate, coordinates, t[SUNRISE])
        val dhuhr = computeMidDay(jDate, t[DHUHR])
        val asr = computeAsr(1 + asrJuristic.ordinal, jDate, coordinates, t[ASR])
        val sunset = computeTime(SUNRISE_ANGLE, jDate, coordinates, t[SUNSET])
        val maghrib = computeTime(params.maghribParameter, jDate, coordinates, t[MAGHRIB])
        val isha = computeTime(params.ishaParameter, jDate, coordinates, t[ISHA])

        return doubleArrayOf(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha)
    }

    private fun computeMidDay(jDate: Double, t: Double): Double {
        val T = equationOfTime(jDate + t)
        return fixHour(12 - T)
    }

    private fun computeTime(
        angle: Double,
        jDate: Double,
        coordinates: Coordinates,
        t: Double
    ): Double {
        val D = sunDeclination(jDate + t)
        val Z = computeMidDay(jDate, t)
        val Beg = -dsin(angle) - dsin(D) * dsin(coordinates.latitude)
        val Mid = dcos(D) * dcos(coordinates.latitude)
        val cosH = (Beg / Mid).coerceIn(-1.0, 1.0) // clamp to avoid NaN at high latitude
        val V = darccos(cosH) / 15.0
        return Z + if (angle > 90) -V else V
    }

    private fun computeAsr(step: Int, jDate: Double, coordinates: Coordinates, t: Double): Double {
        val D = sunDeclination(jDate + t)
        val G = -darccot(step + dtan(abs(coordinates.latitude - D)))
        return computeTime(G, jDate, coordinates, t)
    }

    private fun adjustTimes(
        times: DoubleArray,
        coordinates: Coordinates,
        timeZone: Double
    ): DoubleArray {
        val params = currentParams()
        val adjusted = times.copyOf()

        for (i in adjusted.indices) {
            adjusted[i] += timeZone - coordinates.longitude / 15.0
        }

        adjusted[DHUHR] += dhuhrMinutes / 60.0

        if (params.maghribType == ParameterType.MINUTES) {
            adjusted[MAGHRIB] = adjusted[SUNSET] + params.maghribParameter / 60.0
        }
        if (params.ishaType == ParameterType.MINUTES) {
            adjusted[ISHA] = adjusted[MAGHRIB] + params.ishaParameter / 60.0
        }

        return if (adjustHighLats == HighLatitudeRule.NONE) adjusted else adjustHighLatTimes(
            adjusted,
            params
        )
    }

    private fun adjustTimesFormat(times: DoubleArray, format: TimeFormat): PrayerTimes {
        val formatted = times.map { value ->
            when (format) {
                TimeFormat.TIME_24 -> floatToTime24(value)
                TimeFormat.TIME_12 -> floatToTime12(value, withSuffix = true)
                TimeFormat.TIME_12_NO_SUFFIX -> floatToTime12(value, withSuffix = false)
                TimeFormat.FLOATING -> if (value.isNaN()) invalidTimeText else value.toString()
            }
        }

        return PrayerTimes(
            fajr = formatted[FAJR],
            sunrise = formatted[SUNRISE],
            dhuhr = formatted[DHUHR],
            asr = formatted[ASR],
            sunset = formatted[SUNSET],
            maghrib = formatted[MAGHRIB],
            isha = formatted[ISHA],
        )
    }

    private fun adjustHighLatTimes(times: DoubleArray, params: MethodParams): DoubleArray {
        val adjusted = times.copyOf()
        val nightTime = timeDiff(times[SUNSET], times[SUNRISE])

        val fajrDiff = nightPortion(params.fajrAngle) * nightTime
        if (times[FAJR].isNaN() || timeDiff(times[FAJR], times[SUNRISE]) > fajrDiff) {
            adjusted[FAJR] = times[SUNRISE] - fajrDiff
        }

        val ishaAngle = if (params.ishaType == ParameterType.ANGLE) params.ishaParameter else 18.0
        val ishaDiff = nightPortion(ishaAngle) * nightTime
        if (times[ISHA].isNaN() || timeDiff(times[SUNSET], times[ISHA]) > ishaDiff) {
            adjusted[ISHA] = times[SUNSET] + ishaDiff
        }

        val maghribAngle =
            if (params.maghribType == ParameterType.ANGLE) params.maghribParameter else 4.0
        val maghribDiff = nightPortion(maghribAngle) * nightTime
        if (times[MAGHRIB].isNaN() || timeDiff(times[SUNSET], times[MAGHRIB]) > maghribDiff) {
            adjusted[MAGHRIB] = times[SUNSET] + maghribDiff
        }

        return adjusted
    }

    private fun tuneTimes(times: DoubleArray): DoubleArray {
        val result = times.copyOf()
        val offsetMinutes = offsets.asMinutesArray()
        for (i in result.indices) {
            result[i] += offsetMinutes[i] / 60.0
        }
        return result
    }

    private fun timeDiff(time1: Double, time2: Double): Double = fixHour(time2 - time1)

    private fun dayPortion(times: DoubleArray): DoubleArray =
        times.map { it / 24.0 }.toDoubleArray()

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = floor(y / 100.0)
        val B = 2 - A + floor(A / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5
    }

    private fun sunPosition(jd: Double): DoubleArray {
        val D = jd - 2451545
        val g = fixAngle(357.529 + 0.98560028 * D)
        val q = fixAngle(280.459 + 0.98564736 * D)
        val L = fixAngle(q + (1.915 * dsin(g)) + (0.020 * dsin(2 * g)))
        val e = 23.439 - (0.00000036 * D)
        val d = darcsin(dsin(e) * dsin(L))
        var RA = darctan2(dcos(e) * dsin(L), dcos(L)) / 15.0
        RA = fixHour(RA)
        val EqT = q / 15.0 - RA
        return doubleArrayOf(d, EqT)
    }

    private fun equationOfTime(jd: Double): Double = sunPosition(jd)[1]

    private fun sunDeclination(jd: Double): Double = sunPosition(jd)[0]

    private fun nightPortion(angle: Double): Double = when (adjustHighLats) {
        HighLatitudeRule.ANGLE_BASED -> angle / 60.0
        HighLatitudeRule.MID_NIGHT -> 0.5
        HighLatitudeRule.ONE_SEVENTH -> ONE_SEVENTH_FRACTION
        HighLatitudeRule.NONE -> 0.0
    }

    private fun floatToTime24(time: Double): String {
        if (time.isNaN()) return invalidTimeText
        val value = fixHour(time + 0.5 / 60.0)
        val hours = floor(value).toInt()
        val minutes = floor((value - hours) * 60).toInt()
        val h = hours.toString().padStart(2, '0')
        val m = minutes.toString().padStart(2, '0')
        return "$h:$m"
    }

    private fun floatToTime12(time: Double, withSuffix: Boolean): String {
        if (time.isNaN()) return invalidTimeText
        val value = fixHour(time + 0.5 / 60.0)
        var hours = floor(value).toInt()
        val minutes = floor((value - hours) * 60).toInt()
        val suffix = if (hours >= 12) "PM" else "AM"
        hours = ((hours + 12 - 1) % 12) + 1
        val h = hours.toString().padStart(2, '0')
        val m = minutes.toString().padStart(2, '0')
        val formatted = "$h:$m"
        return if (withSuffix) "$formatted $suffix" else formatted
    }

    private fun PrayerTimesRaw.toArray(): DoubleArray =
        doubleArrayOf(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha)

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour - 24.0 * floor(hour / 24.0)
        if (h < 0) h += 24.0
        return h
    }

    private fun radiansToDegrees(alpha: Double): Double = alpha * 180.0 / kotlin.math.PI

    private fun degreesToRadians(alpha: Double): Double = alpha * kotlin.math.PI / 180.0

    private fun dsin(d: Double): Double = sin(degreesToRadians(d))
    private fun dcos(d: Double): Double = cos(degreesToRadians(d))
    private fun dtan(d: Double): Double = tan(degreesToRadians(d))
    private fun darcsin(x: Double): Double = radiansToDegrees(kotlin.math.asin(x))
    private fun darccos(x: Double): Double = radiansToDegrees(acos(x))
    private fun darctan2(y: Double, x: Double): Double = radiansToDegrees(kotlin.math.atan2(y, x))
    private fun darccot(x: Double): Double = radiansToDegrees(kotlin.math.atan2(1.0, x))

    private companion object {
        private const val INVALID_TIME = "-----"
        private const val SUNRISE_ANGLE = 0.833
        private const val ONE_SEVENTH_FRACTION = 1.0 / 7.0

        private const val FAJR = 0
        private const val SUNRISE = 1
        private const val DHUHR = 2
        private const val ASR = 3
        private const val SUNSET = 4
        private const val MAGHRIB = 5
        private const val ISHA = 6
    }
}
