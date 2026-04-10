package com.example.d_tracker_android.core.work

interface TrackingScheduler {
    fun schedulePeriodicWork()
    fun triggerOneTimeWork()
}
