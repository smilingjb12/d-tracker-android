package com.example.d_tracker_android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.d_tracker_android.StepSensorManager
import kotlinx.coroutines.delay

class StepCounterWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val stepSensorManager = StepSensorManager(applicationContext)
        stepSensorManager.startListening()
        
        // Wait a few seconds to allow the sensor to get a reading
        delay(3000)
        
        stepSensorManager.stopListening()
        return Result.success()
    }
} 