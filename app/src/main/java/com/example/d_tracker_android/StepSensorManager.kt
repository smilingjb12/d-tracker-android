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

class StepSensorManager(context: Context) : SensorEventListener {
    private val TAG = "StepSensorManager"
    
    companion object {
        private const val PREFS_NAME = "StepSensorPrefs"
        private const val KEY_BASELINE = "baseline"
        private const val KEY_BASELINE_DATE = "baseline_date"
        private const val KEY_LATEST_STEP_COUNT = "latest_step_count"
        private const val SAMPLING_PERIOD_MICROS = 5000000 // 5 second sampling period
        private const val DEFAULT_BASELINE_VALUE = -1f
        private const val DEFAULT_STEP_COUNT = 0
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    private var dailyBaseline: Float = DEFAULT_BASELINE_VALUE
    private var currentSteps: Float = 0f
    private val _stepCount = MutableStateFlow(DEFAULT_STEP_COUNT)
    val stepCount: Flow<Int> = _stepCount.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private fun getSavedBaseline(): Float = sharedPrefs.getFloat(KEY_BASELINE, DEFAULT_BASELINE_VALUE)
    
    private fun saveBaseline(baseline: Float) {
        sharedPrefs.edit().putFloat(KEY_BASELINE, baseline).apply()
    }
    
    private fun getSavedDate(): String? = sharedPrefs.getString(KEY_BASELINE_DATE, null)
    
    private fun saveDate(date: String) {
        sharedPrefs.edit().putString(KEY_BASELINE_DATE, date).apply()
    }
    
    private fun getCurrentDay(): String {
        val sdf = java.text.SimpleDateFormat(DATE_FORMAT_PATTERN, java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun saveLatestStepCount(steps: Int) {
        sharedPrefs.edit()
            .putInt(KEY_LATEST_STEP_COUNT, steps)
            .apply()
    }

    fun getLatestStepCount(): Int {
        return sharedPrefs.getInt(KEY_LATEST_STEP_COUNT, DEFAULT_STEP_COUNT)
    }

    fun startListening() {
        if (stepSensor == null) {
            Log.w(TAG, "No step sensor available on this device")
            return
        }
        
        sensorManager.registerListener(
            this,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            SAMPLING_PERIOD_MICROS
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val currentReading = event.values[0]
            val today = getCurrentDay()
            val savedDate = getSavedDate()

            // If there's no saved date or it's not today, update baseline for the new day
            if (savedDate == null || savedDate != today) {
                dailyBaseline = currentReading
                saveDate(today)
                saveBaseline(dailyBaseline)
            } else {
                // Ensure we use the persisted baseline if not already in memory
                if (dailyBaseline < 0) {
                    dailyBaseline = getSavedBaseline()
                }
            }

            currentSteps = currentReading
            val todaySteps = (currentSteps - dailyBaseline).toInt()
            val finalSteps = if (todaySteps < 0) 0 else todaySteps
            _stepCount.value = finalSteps
            saveLatestStepCount(finalSteps)  // Save the latest step count
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    fun hasSensor(): Boolean = stepSensor != null
} 