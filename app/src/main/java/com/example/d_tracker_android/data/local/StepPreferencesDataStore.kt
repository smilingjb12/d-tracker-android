package com.example.d_tracker_android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stepDataStore by preferencesDataStore(name = "step_sensor_prefs")

@Singleton
class StepPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_BASELINE = floatPreferencesKey("baseline")
        private val KEY_BASELINE_DATE = stringPreferencesKey("baseline_date")
        private val KEY_LATEST_STEP_COUNT = intPreferencesKey("latest_step_count")
        private const val DEFAULT_BASELINE = -1f
        private const val DEFAULT_STEPS = 0
    }

    suspend fun updateFromReading(reading: Float) {
        val today = LocalDate.now().toString()
        context.stepDataStore.edit { prefs ->
            val savedDate = prefs[KEY_BASELINE_DATE]
            val savedBaseline = prefs[KEY_BASELINE] ?: DEFAULT_BASELINE

            val baseline = when {
                savedDate == null || savedDate != today -> {
                    prefs[KEY_BASELINE_DATE] = today
                    reading
                }
                reading < savedBaseline -> reading
                savedBaseline < 0f -> reading
                else -> savedBaseline
            }

            prefs[KEY_BASELINE] = baseline
            prefs[KEY_LATEST_STEP_COUNT] = (reading - baseline).toInt().coerceAtLeast(0)
        }
    }

    suspend fun latestStepCount(): Int {
        return context.stepDataStore.data
            .map { prefs -> prefs[KEY_LATEST_STEP_COUNT] ?: DEFAULT_STEPS }
            .first()
    }
}
