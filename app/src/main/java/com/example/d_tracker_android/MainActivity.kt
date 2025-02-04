package com.example.d_tracker_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.PowerManager
import android.widget.Button
import com.example.d_tracker_android.workers.DataSenderWorker
import com.example.d_tracker_android.workers.StepCounterWorker

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
        private const val WORK_REPEAT_INTERVAL = 30L // minutes
        private const val WORK_FLEX_INTERVAL = 15L // minutes
        private const val WORK_BACKOFF_DELAY = 10L // minutes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()
        requestBatteryOptimizationExemption()

        setupManualTrigger()

        if (checkPermissions()) {
            schedulePeriodicWork()
            scheduleStepCounterWork()
        } else {
            requestPermissions()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun checkPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Background location permission not required for Android 9 and below
        }

        return fineLocationGranted && backgroundLocationGranted
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                schedulePeriodicWork()
                scheduleStepCounterWork()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for notifications
            } else {
                Toast.makeText(this, "Notification permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getDefaultConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(false)
        .setRequiresDeviceIdle(false)
        .build()

    private fun schedulePeriodicWork() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<DataSenderWorker>(
            WORK_REPEAT_INTERVAL, TimeUnit.MINUTES,
            WORK_FLEX_INTERVAL, TimeUnit.MINUTES
        )
            .setConstraints(getDefaultConstraints())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WORK_BACKOFF_DELAY, TimeUnit.MINUTES
            )
            .addTag("location_tracking")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DataSenderWork",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )
    }

    private fun scheduleStepCounterWork() {
        val stepCounterWork = PeriodicWorkRequestBuilder<StepCounterWorker>(
            WORK_REPEAT_INTERVAL, TimeUnit.MINUTES,
            WORK_FLEX_INTERVAL, TimeUnit.MINUTES
        )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WORK_BACKOFF_DELAY, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "StepCounterWork",
            ExistingPeriodicWorkPolicy.KEEP,
            stepCounterWork
        )
    }

    private fun setupManualTrigger() {
        findViewById<Button>(R.id.sendDataButton).setOnClickListener {
            if (checkPermissions()) {
                triggerOneTimeWork()
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun triggerOneTimeWork() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DataSenderWorker>()
            .setConstraints(getDefaultConstraints())
            .addTag("manual_data_send")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(oneTimeWorkRequest)

        Toast.makeText(this, "Data sending job triggered", Toast.LENGTH_SHORT).show()
    }
}