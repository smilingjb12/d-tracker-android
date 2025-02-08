package com.example.d_tracker_android

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Handler
import android.os.Looper
import com.example.d_tracker_android.storage.StepDataStorage

class StepSensorManager(context: Context) : SensorEventListener {
    private val TAG = "StepSensorManager"
    
    companion object {
        private const val SAMPLING_PERIOD_MICROS = 5000000 // 5 second sampling period
        private const val DEFAULT_BASELINE_VALUE = -1f
        private const val DEFAULT_STEP_COUNT = 0
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val storage = StepDataStorage(context)
    
    private var dailyBaseline: Float = DEFAULT_BASELINE_VALUE
    private var currentSteps: Float = 0f
    private val _stepCount = MutableStateFlow(DEFAULT_STEP_COUNT)
    val stepCount: Flow<Int> = _stepCount.asStateFlow()

    fun getLatestStepCount(): Int = storage.getLatestStepCount()

    fun startListening(looper: Looper = Looper.getMainLooper()) {
        if (stepSensor == null) {
            Log.w(TAG, "No step sensor available on this device")
            return
        }
        
        // Use a Handler attached to the provided looper to receive sensor events
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
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val currentReading = event.values[0]
            val today = storage.getCurrentDay()
            val savedDate = storage.getSavedDate()

            // If there's no saved date or it's not today, update baseline for the new day
            if (savedDate == null || savedDate != today) {
                dailyBaseline = currentReading
                storage.saveDate(today)
                storage.saveBaseline(dailyBaseline)
            } else {
                // Ensure we use the persisted baseline if not already in memory
                if (dailyBaseline < 0) {
                    dailyBaseline = storage.getBaseline()
                }
                
                // Handle phone reboot scenario where current reading is much lower than baseline
                if (currentReading < dailyBaseline) {
                    Log.i(TAG, "Detected potential device reboot - resetting baseline")
                    dailyBaseline = currentReading
                    storage.saveBaseline(dailyBaseline)
                }
            }

            currentSteps = currentReading
            val todaySteps = (currentSteps - dailyBaseline).toInt()
            _stepCount.value = todaySteps
            storage.saveLatestStepCount(todaySteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    fun hasSensor(): Boolean = stepSensor != null
} 