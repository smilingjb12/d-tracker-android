package com.example.d_tracker_android.domain

import com.example.d_tracker_android.data.battery.BatteryProvider
import com.example.d_tracker_android.data.location.LocationProvider
import com.example.d_tracker_android.data.remote.TrackerApi
import com.example.d_tracker_android.data.sensor.StepSensorManager
import com.example.d_tracker_android.domain.model.SendOutcome
import com.example.d_tracker_android.domain.model.TrackerData
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor(
    private val locationProvider: LocationProvider,
    private val batteryProvider: BatteryProvider,
    private val stepSensorManager: StepSensorManager,
    private val trackerApi: TrackerApi,
    @Named("trackerServerUrl") private val serverUrl: String,
    @Named("trackerAuthorizationKey") private val authorizationKey: String
) {
    suspend fun sendLatestData(): SendOutcome {
        val payload = collectData()
        val response = trackerApi.sendTrackerData(
            url = serverUrl,
            authorizationKey = authorizationKey,
            data = payload
        )
        return response.code().toSendOutcome()
    }

    private suspend fun collectData(): TrackerData {
        val steps = stepSensorManager.readDailySteps()
        val coordinates = locationProvider.getLastLocation()
        return TrackerData(
            power = batteryProvider.getBatteryLevel(),
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            steps = steps
        )
    }

    private fun Int.toSendOutcome(): SendOutcome = when (this) {
        in 200..299 -> SendOutcome.Success
        in 500..599 -> SendOutcome.RetryableFailure
        429 -> SendOutcome.RetryableFailure
        else -> SendOutcome.PermanentFailure
    }
}
