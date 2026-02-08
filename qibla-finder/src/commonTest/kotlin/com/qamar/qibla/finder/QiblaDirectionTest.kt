package com.qamar.qibla.finder

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QiblaDirectionTest {
    @Test
    fun alignmentUsesTolerance() {
        assertTrue(isQiblaAligned(qiblaDirectionDegrees = 100.0, deviceAzimuthDegrees = 112.4))
        assertFalse(isQiblaAligned(qiblaDirectionDegrees = 100.0, deviceAzimuthDegrees = 114.4))
    }
}
