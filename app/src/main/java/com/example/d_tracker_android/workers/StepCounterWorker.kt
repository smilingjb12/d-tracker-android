package com.example.d_tracker_android.workers

import android.content.Context
import android.os.HandlerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.d_tracker_android.StepSensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class StepCounterWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val stepSensorManager = StepSensorManager(applicationContext)
        
        // Create a HandlerThread to provide a Looper for sensor events
        val sensorThread = HandlerThread("StepSensorThread")
        sensorThread.start()
        
        // Update the startListening method (if needed) so that it registers the sensor listener on the provided looper.
        stepSensorManager.startListening(sensorThread.looper)
        
        // Wait long enough for the sensor event to be delivered.
        // (Feel free to adjust the delay if needed.)
        delay(9000)
        
        stepSensorManager.stopListening()
        sensorThread.quitSafely()
        
        Result.success()
    }
} 