package com.example.d_tracker_android

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class StepCountManager(private val context: Context) {
    private val TAG = "StepCountManager"
    
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    suspend fun getTodaySteps(): Int = suspendCoroutine { continuation ->
        try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            
            if (!hasRequiredPermissions(account)) {
                continuation.resume(0)
                return@suspendCoroutine
            }

            val timeRange = getTimeRange()
            val readRequest = buildReadRequest(timeRange.first, timeRange.second)

            Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .addOnSuccessListener { response ->
                    val totalSteps = calculateTotalSteps(response)
                    continuation.resume(totalSteps)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to read step count data", e)
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting step count", e)
            continuation.resumeWithException(e)
        }
    }

    private fun hasRequiredPermissions(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    private fun getTimeRange(): Pair<Long, Long> {
        val endTime = Calendar.getInstance().timeInMillis
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return Pair(startTime, endTime)
    }

    private fun buildReadRequest(startTime: Long, endTime: Long): DataReadRequest {
        return DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun calculateTotalSteps(response: com.google.android.gms.fitness.result.DataReadResponse): Int {
        return response.buckets
            .flatMap { it.dataSets }
            .flatMap { it.dataPoints }
            .sumOf { dataPoint ->
                dataPoint.getValue(DataType.AGGREGATE_STEP_COUNT_DELTA.fields[0]).asInt()
            }
    }
} 