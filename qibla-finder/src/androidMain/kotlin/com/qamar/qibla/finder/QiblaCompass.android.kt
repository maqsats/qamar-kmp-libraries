package com.qamar.qibla.finder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

actual class QiblaCompass actual constructor(
    actual val platformContext: Any?,
) : SensorEventListener {
    private val sensorManager = (platformContext as? Context)
        ?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    actual val isSupported: Boolean = sensorManager != null && accelerometer != null && magnetometer != null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val inclinationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var hasGravity = false
    private var hasGeomagnetic = false
    private var azimuthFix: Float = 0f
    private var listener: QiblaAzimuthListener? = null

    actual fun setListener(listener: QiblaAzimuthListener?) {
        this.listener = listener
    }

    actual fun setAzimuthFix(fix: Float) {
        azimuthFix = fix
    }

    actual fun start() {
        val manager = sensorManager ?: return
        accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    actual fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                hasGravity = applyLowPass(event.values, gravity, hasGravity)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                hasGeomagnetic = applyLowPass(event.values, geomagnetic, hasGeomagnetic)
            }
            else -> return
        }

        if (hasGravity && hasGeomagnetic) {
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                inclinationMatrix,
                gravity,
                geomagnetic,
            )
            if (!success) return
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRadians = orientation[0].toDouble()
            var azimuth = Math.toDegrees(azimuthRadians).toFloat()
            azimuth = (azimuth + azimuthFix + 360f) % 360f
            listener?.invoke(azimuth)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun applyLowPass(input: FloatArray, output: FloatArray, initialized: Boolean): Boolean {
        if (!initialized) {
            System.arraycopy(input, 0, output, 0, input.size)
            return true
        }
        val alpha = 0.97f
        for (i in input.indices) {
            output[i] = output[i] * alpha + input[i] * (1 - alpha)
        }
        return true
    }
}
