package com.example.d_tracker_android.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.d_tracker_android.data.local.StepPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepSensorManager @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStore: StepPreferencesDataStore
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun hasSensor(): Boolean = stepSensor != null

    fun startListening(looper: Looper = Looper.getMainLooper()) {
        if (stepSensor == null) {
            Log.w("StepSensorManager", "No step sensor available")
            return
        }
        val handler = Handler(looper)
        sensorManager.registerListener(
            this,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            handler
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val reading = event.values.firstOrNull() ?: return
        scope.launch {
            dataStore.updateFromReading(reading)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
