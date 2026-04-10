package com.example.d_tracker_android.core.work

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

interface TrackingScheduler {
    fun schedulePeriodicWork()
    fun triggerOneTimeWork()
}

@Singleton
class TrackingWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : TrackingScheduler {
    private fun defaultConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun schedulePeriodicWork() {
        val request = PeriodicWorkRequestBuilder<DataSenderWorker>(
            TrackingWorkConstants.REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
            TrackingWorkConstants.FLEX_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(defaultConstraints())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                TrackingWorkConstants.BACKOFF_DELAY_MINUTES,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TrackingWorkConstants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun triggerOneTimeWork() {
        val request = OneTimeWorkRequestBuilder<DataSenderWorker>()
            .setConstraints(defaultConstraints())
            .addTag(TrackingWorkConstants.MANUAL_TRIGGER_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
