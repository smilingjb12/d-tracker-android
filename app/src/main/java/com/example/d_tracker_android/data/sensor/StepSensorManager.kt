package com.example.d_tracker_android.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import com.example.d_tracker_android.core.work.TrackingWorkConstants
import com.example.d_tracker_android.data.local.StepPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class StepSensorManager @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStore: StepPreferencesDataStore
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun hasSensor(): Boolean = stepSensor != null

    suspend fun readDailySteps(): Int {
        if (!hasSensor()) return 0

        val sensorThread = HandlerThread("StepSensorThread").apply { start() }
        return try {
            val rawReading = withTimeout(TrackingWorkConstants.SENSOR_TIMEOUT_MS) {
                awaitSensorReading(sensorThread)
            }
            dataStore.updateFromReading(rawReading)
            dataStore.latestStepCount()
        } catch (_: Exception) {
            dataStore.latestStepCount()
        } finally {
            sensorThread.quitSafely()
        }
    }

    private suspend fun awaitSensorReading(
        sensorThread: HandlerThread
    ): Float = suspendCancellableCoroutine { cont ->
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
                val reading = event.values.firstOrNull() ?: return
                sensorManager.unregisterListener(this)
                cont.resume(reading)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            Handler(sensorThread.looper)
        )

        cont.invokeOnCancellation {
            sensorManager.unregisterListener(listener)
        }
    }
}
