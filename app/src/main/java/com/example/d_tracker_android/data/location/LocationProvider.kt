package com.example.d_tracker_android.data.location

import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class Coordinates(val latitude: Double, val longitude: Double) {
    companion object {
        val EMPTY = Coordinates(0.0, 0.0)
    }
}

@Singleton
class LocationProvider @Inject constructor(
    private val locationClient: FusedLocationProviderClient
) {
    suspend fun getLastLocation(): Coordinates {
        return try {
            val location = locationClient.lastLocation.await()
            if (location == null) Coordinates.EMPTY
            else Coordinates(location.latitude, location.longitude)
        } catch (_: Exception) {
            Coordinates.EMPTY
        }
    }
}
