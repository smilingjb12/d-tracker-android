package com.example.d_tracker_android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.d_tracker_android.app.ui.theme.DTrackerTheme
import com.example.d_tracker_android.feature.tracking.TrackingRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DTrackerTheme {
                TrackingRoute()
            }
        }
    }
}
