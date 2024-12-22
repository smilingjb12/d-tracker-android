package com.example.d_tracker_android

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class DataSenderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "DataSenderWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting periodic work execution")
            val batteryLevel = getBatteryLevel()
            val (latitude, longitude) = getLocation()

            var urlFromSettings = applicationContext.getString(R.string.server_url)
            val url = URL(urlFromSettings)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("authorization-key", applicationContext.getString(R.string.authorization_key))

            Log.d(TAG, "Sending data - Battery: $batteryLevel, Location: ($latitude, $longitude)")
            val postData = """
                {
                    "power": $batteryLevel,
                    "latitude": $latitude,
                    "longitude": $longitude
                }
            """.trimIndent()

            connection.outputStream.use { it.write(postData.toByteArray()) }
            val responseCode = connection.responseCode
            Log.d(TAG, "GOT RESPONSE ${connection.responseMessage}")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Data sent successfully")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send data, response code: $responseCode")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker execution", e)
            Result.retry()
        }
    }

    private fun getBatteryLevel(): Float {
        val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
    }

    private fun getLocation(): Pair<Double, Double> {
        var latitude = 0.0
        var longitude = 0.0
        try {
            val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return Pair(latitude, longitude)
    }
}
