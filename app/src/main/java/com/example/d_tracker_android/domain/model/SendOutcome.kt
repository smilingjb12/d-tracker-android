package com.example.d_tracker_android.domain.model

sealed interface SendOutcome {
    data object Success : SendOutcome
    data object RetryableFailure : SendOutcome
    data object PermanentFailure : SendOutcome
}
