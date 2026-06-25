package com.falldetect.app.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorDataProcessor : SensorEventListener {
    private val _acceleration = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val acceleration: StateFlow<FloatArray> = _acceleration

    private val _gyroscope = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscope: StateFlow<FloatArray> = _gyroscope

    private val _isFreeFall = MutableStateFlow(false)
    val isFreeFall: StateFlow<Boolean> = _isFreeFall

    private var lastAcceleration = floatArrayOf(0f, 0f, 0f)
    private val gravity = floatArrayOf(0f, 0f, 9.81f)

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                val x = event.values[0] - gravity[0]
                val y = event.values[1] - gravity[1]
                val z = event.values[2] - gravity[2]

                _acceleration.value = floatArrayOf(x, y, z)

                val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                _isFreeFall.value = magnitude < 2.0f
            }
            Sensor.TYPE_GYROSCOPE -> {
                _gyroscope.value = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
