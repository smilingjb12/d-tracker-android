package com.example.d_tracker_android.feature.tracking

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrackingScreen(
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

        state.lastTrackerData?.let { data ->
            LastDataCard(data)
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
