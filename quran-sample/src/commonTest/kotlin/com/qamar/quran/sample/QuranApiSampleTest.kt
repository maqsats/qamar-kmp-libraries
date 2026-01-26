package com.qamar.quran.sample

import com.qamar.quran.test.runTest
import kotlin.test.Test

/**
 * Test that runs the comprehensive sample to demonstrate all functionality.
 * 
 * Run this test with:
 * ./gradlew :quran-sample:desktopTest --tests "com.qamar.quran.sample.QuranApiSampleTest.testRunAllSamples"
 */
class QuranApiSampleTest {
    
    @Test
    fun testRunAllSamples() = runTest {
        val sample = QuranApiSample()
        sample.runAll()
    }
}
