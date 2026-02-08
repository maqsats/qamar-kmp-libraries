package com.qamar.qibla.finder

data class QiblaInfo(
    val longitude: Double,
    val latitude: Double,
    val directionDegrees: Double,
)

data class QiblaLocation(
    val latitude: Double,
    val longitude: Double,
)

data class QiblaAlignmentState(
    val directionDegrees: Double,
    val currentAzimuth: Double,
    val isAligned: Boolean,
)
