package com.example.d_tracker_android

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.IBinder
import android.location.LocationManager
import android.location.LocationListener
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class DataSenderService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var isSendingData = false // Flag to track if the sending loop is active
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private val TAG = "DataSender"

    override fun onCreate() {
        Log.d(TAG, "onCreate DataSender")
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        setupLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isSendingData) {
            return START_STICKY
        }
        coroutineScope.launch {
            Log.d(TAG, "Starting the loop")
            while (true) {
                Log.d(TAG, "Attempting to send data")
                sendData()
                delay(20 * 60 * 1000) // 20 minutes
            }
        }
        return START_STICKY
    }

    private fun setupLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private suspend fun sendData() = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendData()")
        val batteryLevel = getBatteryLevel()
        val (latitude, longitude) = getLocation()

        var urlFromSettings = getString(R.string.server_url)
        val url = URL(urlFromSettings)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        val postData = """
            {
                "power": $batteryLevel,
                "latitude": $latitude,
                "longitude": $longitude
            }
        """.trimIndent()

        connection.outputStream.use { it.write(postData.toByteArray()) }

        val responseCode = connection.responseCode
        Log.d(TAG,"GOT RESPONSE ${connection.responseMessage}")
    }

    private fun getBatteryLevel(): Float {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
    }

    private fun getLocation(): Pair<Double, Double> {
        var latitude = 0.0
        var longitude = 0.0
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return Pair(latitude, longitude)
    }

    private fun createNotification(): Notification {
        val channelId = "DataSenderServiceChannel"
        val channelName = "d-tracker Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("d-tracker Service")
            .setContentText("Sending data in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        locationManager.removeUpdates(locationListener)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}