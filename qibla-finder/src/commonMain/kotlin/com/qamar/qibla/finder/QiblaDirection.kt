package com.qamar.qibla.finder

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

const val KAABA_LATITUDE = 21.422517
const val KAABA_LONGITUDE = 39.826166

private const val DEG_TO_RAD = PI / 180.0
private const val RAD_TO_DEG = 180.0 / PI

fun qiblaDirectionFrom(longitudeDegrees: Double, latitudeDegrees: Double): Double {
    val a = toRadians(KAABA_LONGITUDE - longitudeDegrees)
    val b = toRadians(90.0 - latitudeDegrees)
    val c = toRadians(90.0 - KAABA_LATITUDE)
    val s = toDegrees(
        atan2(
            sin(a),
            sin(b) * cot(c) - cos(b) * cos(a),
        ),
    )
    return if (s < 0) 360.0 + s else s
}

fun cot(angleRadians: Double): Double = tan(PI / 2 - angleRadians)

fun isQiblaAligned(
    qiblaDirectionDegrees: Double,
    deviceAzimuthDegrees: Double,
    toleranceDegrees: Double = 14.0,
): Boolean {
    val delta = abs(round(deviceAzimuthDegrees) - qiblaDirectionDegrees)
    return delta < toleranceDegrees
}

private fun toRadians(degrees: Double): Double = degrees * DEG_TO_RAD

private fun toDegrees(radians: Double): Double = radians * RAD_TO_DEG
