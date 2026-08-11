package com.example.myapplication.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var shakeTimestamp: Long = 0
    private var shakeCount: Int = 0

    var thresholdForce = 10.0f
    private val shakeSlopTimeMs = 300
    private val shakeCountResetTimeMs = 3000

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > thresholdForce) {
            val now = System.currentTimeMillis()
            if (shakeTimestamp + shakeSlopTimeMs > now) return
            if (shakeTimestamp + shakeCountResetTimeMs < now) shakeCount = 0

            shakeTimestamp = now
            shakeCount++

            if (shakeCount >= 2) {
                onShake()
                shakeCount = 0
            }
        }
    }
}
