package com.example.d_tracker_android.core.work

object TrackingWorkConstants {
    const val UNIQUE_WORK_NAME = "DataSenderWork"
    const val REPEAT_INTERVAL_MINUTES = 30L
    const val FLEX_INTERVAL_MINUTES = 15L
    const val BACKOFF_DELAY_MINUTES = 10L
    const val MANUAL_TRIGGER_TAG = "manual_data_send"
    const val SENSOR_TIMEOUT_MS = 30_000L
}
