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
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    private var dailyBaseline: Float = -1f
    private var currentSteps: Float = 0f
    private val _stepCount = MutableStateFlow(0)
    val stepCount: Flow<Int> = _stepCount.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("StepSensorPrefs", Context.MODE_PRIVATE)
    
    private fun getSavedBaseline(): Float = sharedPrefs.getFloat("baseline", -1f)
    
    private fun saveBaseline(baseline: Float) {
        sharedPrefs.edit().putFloat("baseline", baseline).apply()
    }
    
    private fun getSavedDate(): String? = sharedPrefs.getString("baseline_date", null)
    
    private fun saveDate(date: String) {
        sharedPrefs.edit().putString("baseline_date", date).apply()
    }
    
    private fun getCurrentDay(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun startListening() {
        if (stepSensor == null) {
            Log.w(TAG, "No step sensor available on this device")
            return
        }
        
        sensorManager.registerListener(
            this,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL
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
            _stepCount.value = if (todaySteps < 0) 0 else todaySteps
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    fun hasSensor(): Boolean = stepSensor != null
} 