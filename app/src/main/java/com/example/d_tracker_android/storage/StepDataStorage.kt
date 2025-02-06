package com.example.d_tracker_android.storage

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepDataStorage(context: Context) {
    companion object {
        private const val PREFS_NAME = "StepSensorPrefs"
        private const val KEY_BASELINE = "baseline"
        private const val KEY_BASELINE_DATE = "baseline_date"
        private const val KEY_LATEST_STEP_COUNT = "latest_step_count"
        private const val DEFAULT_BASELINE_VALUE = -1f
        private const val DEFAULT_STEP_COUNT = 0
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getBaseline(): Float = sharedPrefs.getFloat(KEY_BASELINE, DEFAULT_BASELINE_VALUE)
    
    fun saveBaseline(baseline: Float) {
        sharedPrefs.edit().putFloat(KEY_BASELINE, baseline).apply()
    }
    
    fun getSavedDate(): String? = sharedPrefs.getString(KEY_BASELINE_DATE, null)
    
    fun saveDate(date: String) {
        sharedPrefs.edit().putString(KEY_BASELINE_DATE, date).apply()
    }
    
    fun getCurrentDay(): String {
        val sdf = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
        return sdf.format(Date())
    }

    fun saveLatestStepCount(steps: Int) {
        sharedPrefs.edit()
            .putInt(KEY_LATEST_STEP_COUNT, steps)
            .apply()
    }

    fun getLatestStepCount(): Int {
        return sharedPrefs.getInt(KEY_LATEST_STEP_COUNT, DEFAULT_STEP_COUNT)
    }
} 