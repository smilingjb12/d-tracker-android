package com.example.d_tracker_android

import android.app.Application
import androidx.work.*

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setJobSchedulerJobIdRange(1000, 20000)
            .setMaxSchedulerLimit(50)
            .build()

        WorkManager.initialize(this, config)
    }
}
