package com.example.d_tracker_android.feature.tracking

import com.example.d_tracker_android.core.work.TrackingScheduler
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackingViewModelTest {
    @Test
    fun `schedules periodic work once after permissions granted`() {
        val scheduler = FakeTrackingWorkScheduler()
        val viewModel = TrackingViewModel(trackingScheduler = scheduler)

        viewModel.onPermissionStateChanged(true)
        viewModel.onPermissionStateChanged(true)

        assertThat(scheduler.periodicScheduleCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.periodicWorkScheduled).isTrue()
    }

    @Test
    fun `manual send enqueues one-time work when permissions granted`() {
        val scheduler = FakeTrackingWorkScheduler()
        val viewModel = TrackingViewModel(trackingScheduler = scheduler)

        viewModel.onPermissionStateChanged(true)
        viewModel.onManualSendClicked()

        assertThat(scheduler.manualTriggerCalls).isEqualTo(1)
    }

    private class FakeTrackingWorkScheduler : TrackingScheduler {
        var periodicScheduleCalls = 0
        var manualTriggerCalls = 0

        override fun schedulePeriodicWork() {
            periodicScheduleCalls += 1
        }

        override fun triggerOneTimeWork() {
            manualTriggerCalls += 1
        }
    }
}
