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

    private var lastMagnitude = 0f
    private var lastRotation = 0f
    private var lastFreeFallCount = 0

    private var freeFallDetected = false
    private var freeFallStartTime = 0L
    private val minFreeFallDuration = 100L

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

        lastMagnitude = magnitude
        lastRotation = checkRotation(gyroscope).let { if (it) 1f else 0f }
        lastFreeFallCount = countFreeFallInHistory()

        if (isStrongMotion()) {
            _fallDetected.value = false
            lastImpactTime = currentTime
            return
        }

        if (magnitude < 5.0f && !freeFallDetected) {
            freeFallDetected = true
            freeFallStartTime = currentTime
        }

        if (freeFallDetected && magnitude > threshold) {
            val freeFallDuration = currentTime - freeFallStartTime
            val hasStrongImpact = magnitude > threshold * 1.5f
            val hasRotation = checkRotation(gyroscope)
            
            if (freeFallDuration >= minFreeFallDuration && 
                (hasStrongImpact || hasRotation) &&
                currentTime - lastImpactTime > impactCooldown) {
                _fallDetected.value = true
                lastImpactTime = currentTime
            }
            freeFallDetected = false
        }

        if (!freeFallDetected && magnitude > threshold * 1.8f) {
            val hasRotation = checkRotation(gyroscope)
            if (hasRotation && currentTime - lastImpactTime > impactCooldown) {
                _fallDetected.value = true
                lastImpactTime = currentTime
            }
        }

        if (_fallDetected.value && currentTime - lastImpactTime > impactCooldown) {
            _fallDetected.value = false
            lastImpactTime = 0L
        }
    }

    private fun calculateThreshold(): Float {
        val baseThreshold = 15.0f
        val sensitivityFactor = 1.0f + (5 - sensitivityLevel) * 0.1f
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
        return countFreeFallInHistory() >= 5
    }

    private fun countFreeFallInHistory(): Int {
        if (accelerationHistory.size < 10) return 0
        
        val recentAccelerations = accelerationHistory.takeLast(10)
        return recentAccelerations.count { accel ->
            val mag = sqrt(
                (accel[0] * accel[0] +
                accel[1] * accel[1] +
                accel[2] * accel[2]).toDouble()
            ).toFloat()
            mag < 5.0f
        }
    }

    private fun isStrongMotion(): Boolean {
        if (accelerationHistory.size < 15) return false
        
        val recentAccelerations = accelerationHistory.takeLast(15)
        
        val magnitudes = recentAccelerations.map { accel ->
            sqrt(
                (accel[0] * accel[0] +
                accel[1] * accel[1] +
                accel[2] * accel[2]).toDouble()
            ).toFloat()
        }
        
        val mean = magnitudes.average().toFloat()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = sqrt(variance.toDouble()).toFloat()
        
        val highAccelCount = magnitudes.count { it > 20.0f }
        val isContinuousHighAccel = highAccelCount > 10
        
        val isCyclicMotion = stdDev > 8.0f && 
            highAccelCount > 5 &&
            mean > 12.0f
        
        return isContinuousHighAccel || isCyclicMotion
    }

    fun calculateConfidence(): Float {
        val threshold = calculateThreshold()
        
        val magnitudeScore = if (lastMagnitude > threshold) {
            (lastMagnitude / (threshold * 2)).coerceIn(0f, 1f)
        } else {
            0f
        }

        val rotationScore = lastRotation

        val freeFallScore = (lastFreeFallCount / 5f).coerceIn(0f, 1f)

        return (magnitudeScore * 0.5f + rotationScore * 0.3f + freeFallScore * 0.2f)
            .coerceIn(0f, 1f)
    }
}
