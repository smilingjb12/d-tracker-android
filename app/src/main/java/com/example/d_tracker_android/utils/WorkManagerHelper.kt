package com.example.d_tracker_android.utils

import android.content.Context
import androidx.work.*
import com.example.d_tracker_android.workers.DataSenderWorker
import java.util.concurrent.TimeUnit

class WorkManagerHelper(private val context: Context) {

    companion object {
        private const val WORK_REPEAT_INTERVAL_IN_MINUTES = 30L
        private const val WORK_FLEX_INTERVAL_IN_MINUTES = 15L
        private const val WORK_BACKOFF_DELAY_IN_MINUTES = 10L
    }

    private fun getDefaultConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(false)
        .setRequiresDeviceIdle(false)
        .build()

    fun schedulePeriodicWork() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<DataSenderWorker>(
            WORK_REPEAT_INTERVAL_IN_MINUTES, TimeUnit.MINUTES,
            WORK_FLEX_INTERVAL_IN_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(getDefaultConstraints())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WORK_BACKOFF_DELAY_IN_MINUTES, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DataSenderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    fun triggerOneTimeWork() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DataSenderWorker>()
            .setConstraints(getDefaultConstraints())
            .addTag("manual_data_send")
            .build()

        WorkManager.getInstance(context)
            .enqueue(oneTimeWorkRequest)
    }
} 