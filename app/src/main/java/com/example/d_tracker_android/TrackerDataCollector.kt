package com.example.d_tracker_android.data

import android.content.Context
import android.os.BatteryManager
import com.example.d_tracker_android.StepSensorManager
import com.example.d_tracker_android.models.Location
import com.example.d_tracker_android.models.TrackerData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class TrackerDataCollector(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    private val stepSensorManager = StepSensorManager(context)

    init {
        stepSensorManager.startListening()
    }

    suspend fun collectData(): TrackerData {
        val location = getLocation()
        return TrackerData(
            power = getBatteryLevel(),
            steps = stepSensorManager.getLatestStepCount(),
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    private fun getBatteryLevel(): Float {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
    }

    private fun getLocation(): Location {
        return try {
            val locationTask = fusedLocationClient.lastLocation
            val location = Tasks.await(locationTask)
            if (location != null) {
                Location(location.latitude, location.longitude)
            } else {
                Location(0.0, 0.0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Location(0.0, 0.0)
        }
    }
} 