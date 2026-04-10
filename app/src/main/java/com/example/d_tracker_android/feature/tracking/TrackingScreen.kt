package com.example.d_tracker_android.feature.tracking

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.d_tracker_android.core.permissions.PermissionOrchestrator

@Composable
fun TrackingRoute(
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionOrchestrator = remember(context) { PermissionOrchestrator(context) }
    val activity = remember(context) { context.findActivity() }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionStateChanged(permissionOrchestrator.hasTrackingPermissions())
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionStateChanged(permissionOrchestrator.hasTrackingPermissions())
        viewModel.onBatteryOptimizationStateChanged(permissionOrchestrator.isBatteryOptimizationIgnored())
    }

    TrackingScreen(
        state = uiState,
        onRequestPermissions = {
            permissionsLauncher.launch(permissionOrchestrator.requiredRuntimePermissions().toTypedArray())
        },
        onRequestBatteryOptimizationDisable = {
            activity?.startActivity(permissionOrchestrator.createBatteryOptimizationIntent())
        },
        onManualSend = viewModel::onManualSendClicked
    )
}

@Composable
private fun TrackingScreen(
    state: TrackingUiState,
    onRequestPermissions: () -> Unit,
    onRequestBatteryOptimizationDisable: () -> Unit,
    onManualSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "D Tracker",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (state.periodicWorkScheduled) {
                        "Background Sync: Active"
                    } else {
                        "Background Sync: Pending"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = state.statusMessage)
            }
        }

        if (!state.hasTrackingPermissions) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Required Permissions")
            }
        }

        if (!state.batteryOptimizationIgnored) {
            Button(
                onClick = onRequestBatteryOptimizationDisable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disable Battery Optimization")
            }
        }

        Button(
            onClick = onManualSend,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Data Now")
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
