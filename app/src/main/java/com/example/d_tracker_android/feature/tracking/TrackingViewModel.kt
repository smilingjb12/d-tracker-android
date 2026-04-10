package com.example.d_tracker_android.feature.tracking

import androidx.lifecycle.ViewModel
import com.example.d_tracker_android.core.work.TrackingScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TrackingUiState(
    val hasTrackingPermissions: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val periodicWorkScheduled: Boolean = false,
    val statusMessage: String = "Checking prerequisites..."
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val trackingScheduler: TrackingScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    fun onPermissionStateChanged(granted: Boolean) {
        _uiState.update { current ->
            current.copy(
                hasTrackingPermissions = granted,
                statusMessage = if (granted) {
                    "Permissions granted"
                } else {
                    "Location, activity, and notification permissions are required"
                }
            )
        }

        if (granted && !_uiState.value.periodicWorkScheduled) {
            trackingScheduler.schedulePeriodicWork()
            _uiState.update { it.copy(periodicWorkScheduled = true, statusMessage = "Background tracking is active") }
        }
    }

    fun onBatteryOptimizationStateChanged(ignored: Boolean) {
        _uiState.update { current ->
            current.copy(
                batteryOptimizationIgnored = ignored,
                statusMessage = if (ignored) {
                    current.statusMessage
                } else {
                    "Battery optimization should be disabled for reliable tracking"
                }
            )
        }
    }

    fun onManualSendClicked() {
        if (!_uiState.value.hasTrackingPermissions) {
            _uiState.update { it.copy(statusMessage = "Grant permissions before sending data") }
            return
        }

        trackingScheduler.triggerOneTimeWork()
        _uiState.update { it.copy(statusMessage = "Manual data sending job enqueued") }
    }
}
