package com.falldetect.app

import com.falldetect.app.service.FallDetector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FallDetectorTest {
    private lateinit var fallDetector: FallDetector

    @Before
    fun setup() {
        fallDetector = FallDetector()
    }

    @Test
    fun `test fall detection with high acceleration`() {
        fallDetector.updateSensitivity(5)
        
        val acceleration = floatArrayOf(50f, 0f, 0f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(10) {
            fallDetector.processData(acceleration, false, gyroscope)
        }
        
        assertTrue(fallDetector.fallDetected.value)
    }

    @Test
    fun `test no fall with normal acceleration`() {
        fallDetector.updateSensitivity(5)
        
        val acceleration = floatArrayOf(1f, 0f, 0f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(10) {
            fallDetector.processData(acceleration, false, gyroscope)
        }
        
        assertFalse(fallDetector.fallDetected.value)
    }

    @Test
    fun `test sensitivity affects threshold`() {
        fallDetector.updateSensitivity(1)
        val highThresholdAccel = floatArrayOf(30f, 0f, 0f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(10) {
            fallDetector.processData(highThresholdAccel, false, gyroscope)
        }
        
        assertTrue(fallDetector.fallDetected.value)
    }

    @Test
    fun `test free fall detection`() {
        fallDetector.updateSensitivity(5)
        
        val lowAcceleration = floatArrayOf(0.5f, 0.5f, 0.5f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(15) {
            fallDetector.processData(lowAcceleration, true, gyroscope)
        }
    }
}
