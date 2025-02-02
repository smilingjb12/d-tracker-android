package com.example.d_tracker_android

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.d_tracker_android.data.TrackerDataCollector
import com.example.d_tracker_android.network.TrackerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

class DataSenderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DataSenderWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val dataCollector = TrackerDataCollector(appContext)
    private val apiService = TrackerApiService(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val trackerData = dataCollector.collectData()
            Log.d(TAG, "Collected data: $trackerData")

            val responseCode = apiService.sendData(trackerData)
            
            return@withContext when (responseCode) {
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
                    if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker execution", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
