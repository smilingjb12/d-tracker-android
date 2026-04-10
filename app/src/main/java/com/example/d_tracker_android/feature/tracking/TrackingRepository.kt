package com.example.d_tracker_android.feature.tracking

import android.content.Context
import android.os.BatteryManager
import android.os.HandlerThread
import com.example.d_tracker_android.BuildConfig
import com.example.d_tracker_android.data.local.StepPreferencesDataStore
import com.example.d_tracker_android.data.remote.TrackerApi
import com.example.d_tracker_android.data.sensor.StepSensorManager
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TrackerData(
    val power: Float,
    val latitude: Double,
    val longitude: Double,
    val steps: Int
)

sealed interface SendOutcome {
    data object Success : SendOutcome
    data object RetryableFailure : SendOutcome
    data object PermanentFailure : SendOutcome
}

@Singleton
class TrackingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationClient: FusedLocationProviderClient,
    private val stepSensorManager: StepSensorManager,
    private val stepDataStore: StepPreferencesDataStore,
    private val trackerApi: TrackerApi,
    private val json: Json
) {
    companion object {
        private const val SENSOR_WARMUP_DELAY = 10_000L
        private const val SENSOR_READ_DELAY = 9_000L
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    suspend fun collectData(): TrackerData {
        val steps = readStepCount()
        val (latitude, longitude) = readLocation()
        return TrackerData(
            power = readBatteryLevel(),
            latitude = latitude,
            longitude = longitude,
            steps = steps
        )
    }

    suspend fun sendLatestData(): SendOutcome {
        val payload = collectData()
        val body = json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE)
        val response = trackerApi.sendTrackerData(
            url = BuildConfig.TRACKER_SERVER_URL,
            authorizationKey = BuildConfig.TRACKER_AUTHORIZATION_KEY,
            requestBody = body
        )
        return response.code().toSendOutcome()
    }

    private suspend fun readStepCount(): Int {
        if (!stepSensorManager.hasSensor()) return 0

        val sensorThread = HandlerThread("StepSensorThread").apply { start() }
        return try {
            stepSensorManager.startListening(sensorThread.looper)
            delay(SENSOR_WARMUP_DELAY + SENSOR_READ_DELAY)
            stepDataStore.latestStepCount()
        } finally {
            stepSensorManager.stopListening()
            sensorThread.quitSafely()
        }
    }

    private suspend fun readLocation(): Pair<Double, Double> {
        return try {
            val location = locationClient.lastLocation.await()
            if (location == null) 0.0 to 0.0 else location.latitude to location.longitude
        } catch (_: Exception) {
            0.0 to 0.0
        }
    }

    private fun readBatteryLevel(): Float {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
    }

    private fun Int.toSendOutcome(): SendOutcome {
        return when (this) {
            in 200..299 -> SendOutcome.Success
            in 500..599 -> SendOutcome.RetryableFailure
            429 -> SendOutcome.RetryableFailure
            else -> SendOutcome.PermanentFailure
        }
    }
}
