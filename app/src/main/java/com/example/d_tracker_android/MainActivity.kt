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
import com.example.d_tracker_android.utils.PermissionManager
import com.example.d_tracker_android.utils.WorkManagerHelper

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var permissionManager: PermissionManager
    private lateinit var workManagerHelper: WorkManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionManager = PermissionManager(this)
        workManagerHelper = WorkManagerHelper(applicationContext)

        permissionManager.requestNotificationPermission()
        requestBatteryOptimizationExemption()

        setupManualTrigger()

        if (permissionManager.checkLocationPermissions()) {
            workManagerHelper.schedulePeriodicWork()
        } else {
            permissionManager.requestLocationPermissions()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PermissionManager.NOTIFICATION_PERMISSION_REQUEST_CODE)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionManager.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    workManagerHelper.schedulePeriodicWork()
                } else {
                    Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
                }
            }
            PermissionManager.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission is required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupManualTrigger() {
        findViewById<Button>(R.id.sendDataButton).setOnClickListener {
            if (permissionManager.checkLocationPermissions()) {
                workManagerHelper.triggerOneTimeWork()
                Toast.makeText(this, "Data sending job triggered", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show()
            }
        }
    }
}