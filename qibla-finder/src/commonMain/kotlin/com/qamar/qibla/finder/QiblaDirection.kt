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
    // Circular (shortest-arc) distance on the compass. A plain
    // abs(az - dir) breaks across the 0deg/360deg seam: 359deg and 1deg are 2deg
    // apart, not 358deg. Without the wrap, users whose qibla points near
    // due north (e.g. East Africa -- Kenya, Tanzania, Somalia, Comoros)
    // never see the aligned state even when pointing correctly. The
    // `% 360` guards against unnormalized inputs before folding.
    val raw = abs(round(deviceAzimuthDegrees) - qiblaDirectionDegrees) % 360.0
    val delta = minOf(raw, 360.0 - raw)
    return delta < toleranceDegrees
}

private fun toRadians(degrees: Double): Double = degrees * DEG_TO_RAD

private fun toDegrees(radians: Double): Double = radians * RAD_TO_DEG
