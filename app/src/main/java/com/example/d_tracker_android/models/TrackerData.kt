package com.example.d_tracker_android.models

import kotlinx.serialization.Serializable

@Serializable
data class TrackerData(
    val power: Float,
    val latitude: Double,
    val longitude: Double,
    val steps: Int
)
