package com.example.d_tracker_android.feature.tracking

import android.content.Intent
import com.example.d_tracker_android.core.permissions.PermissionOrchestrator

class FakePermissionOrchestrator(
    var permissionsGranted: Boolean = false,
    var batteryOptimizationIgnored: Boolean = false
) : PermissionOrchestrator {
    override fun requiredRuntimePermissions(): List<String> = emptyList()
    override fun hasTrackingPermissions(): Boolean = permissionsGranted
    override fun isBatteryOptimizationIgnored(): Boolean = batteryOptimizationIgnored
    override fun createBatteryOptimizationIntent(): Intent = Intent()
}
