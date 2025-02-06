package com.example.d_tracker_android.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.d_tracker_android.R
import com.example.d_tracker_android.StepSensorManager
import com.example.d_tracker_android.data.TrackerDataCollector
import com.example.d_tracker_android.network.TrackerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

class DataSenderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DataSenderWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "tracker_service_channel"
        private const val CHANNEL_NAME = "Tracker Service Channel"
        private const val SENSOR_WARMUP_DELAY = 10000L // 10 seconds to warm up sensors
        private const val STEP_SENSOR_DELAY = 9000L // 9 seconds for step sensor reading
    }

    private val dataCollector = TrackerDataCollector(applicationContext)
    private val apiService = TrackerApiService(applicationContext)
    private val stepSensorManager = StepSensorManager(applicationContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Tracking Active")
            .setContentText("Collecting and sending data...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for tracking service"
            }
            
            val notificationManager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            setForeground(getForegroundInfo())
            collectSensorData()
            return@withContext sendDataToServer()
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker execution", e)
            return@withContext handleError()
        }
    }

    private suspend fun collectSensorData() {
        val sensorThread = HandlerThread("StepSensorThread").apply { start() }
        
        try {
            stepSensorManager.startListening(sensorThread.looper)
            delay(SENSOR_WARMUP_DELAY)
            delay(STEP_SENSOR_DELAY)
        } finally {
            stepSensorManager.stopListening()
            sensorThread.quitSafely()
        }
    }

    private suspend fun sendDataToServer(): Result {
        val trackerData = dataCollector.collectData()
        Log.d(TAG, "Collected data: $trackerData")

        val responseCode = apiService.sendData(trackerData)
        return when (responseCode) {
            HttpURLConnection.HTTP_OK -> {
                Log.d(TAG, "Data sent successfully")
                Result.success()
            }
            in 500..599 -> {
                Log.e(TAG, "Server error, will retry. Response code: $responseCode")
                Result.retry()
            }
            else -> {
                Log.e(TAG, "Failed to send data, response code: $responseCode")
                handleError()
            }
        }
    }

    private fun handleError(): Result {
        return if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
    }
} 