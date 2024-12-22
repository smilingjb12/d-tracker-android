package com.example.d_tracker_android

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        )
    }
}
