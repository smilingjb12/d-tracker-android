package com.example.d_tracker_android.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.d_tracker_android.workers.DataSenderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
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
