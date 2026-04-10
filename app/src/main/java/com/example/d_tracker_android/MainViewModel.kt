package com.example.d_tracker_android

import androidx.lifecycle.ViewModel
import com.example.d_tracker_android.utils.WorkManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val workManagerHelper: WorkManagerHelper
) : ViewModel() {

    fun schedulePeriodicWork() {
        workManagerHelper.schedulePeriodicWork()
    }

    fun triggerOneTimeWork() {
        workManagerHelper.triggerOneTimeWork()
    }
}
