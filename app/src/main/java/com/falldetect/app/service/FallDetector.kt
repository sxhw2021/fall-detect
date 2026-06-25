package com.falldetect.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class FallDetector {
    private val _fallDetected = MutableStateFlow(false)
    val fallDetected: StateFlow<Boolean> = _fallDetected

    private var sensitivityLevel = 5
    private var lastImpactTime = 0L
    private val impactCooldown = 2000L

    private var accelerationHistory = mutableListOf<FloatArray>()
    private val historySize = 50

    fun updateSensitivity(level: Int) {
        sensitivityLevel = level.coerceIn(1, 10)
    }

    fun processData(
        acceleration: FloatArray,
        isFreeFall: Boolean,
        gyroscope: FloatArray
    ) {
        val currentTime = System.currentTimeMillis()
        
        accelerationHistory.add(acceleration.clone())
        if (accelerationHistory.size > historySize) {
            accelerationHistory.removeAt(0)
        }

        val magnitude = sqrt(
            (acceleration[0] * acceleration[0] +
            acceleration[1] * acceleration[1] +
            acceleration[2] * acceleration[2]).toDouble()
        ).toFloat()

        val threshold = calculateThreshold()

        if (magnitude > threshold && 
            currentTime - lastImpactTime > impactCooldown) {
            
            val hasRotation = checkRotation(gyroscope)
            val wasFreeFall = checkFreeFallPattern()

            if (hasRotation || wasFreeFall || magnitude > threshold * 1.5f) {
                _fallDetected.value = true
                lastImpactTime = currentTime
            }
        }

        if (_fallDetected.value && currentTime - lastImpactTime > impactCooldown) {
            _fallDetected.value = false
        }
    }

    private fun calculateThreshold(): Float {
        val baseThreshold = 25.0f
        val sensitivityFactor = 1.0f + (sensitivityLevel - 5) * 0.1f
        return baseThreshold / sensitivityFactor
    }

    private fun checkRotation(gyroscope: FloatArray): Boolean {
        val rotationMagnitude = sqrt(
            (gyroscope[0] * gyroscope[0] +
            gyroscope[1] * gyroscope[1] +
            gyroscope[2] * gyroscope[2]).toDouble()
        ).toFloat()
        return rotationMagnitude > 5.0f
    }

    private fun checkFreeFallPattern(): Boolean {
        if (accelerationHistory.size < 10) return false
        
        val recentAccelerations = accelerationHistory.takeLast(10)
        val lowAccelCount = recentAccelerations.count { accel ->
            val mag = sqrt(
                (accel[0] * accel[0] +
                accel[1] * accel[1] +
                accel[2] * accel[2]).toDouble()
            ).toFloat()
            mag < 3.0f
        }
        return lowAccelCount >= 5
    }
}
