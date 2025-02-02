package com.example.d_tracker_android.network

import android.content.Context
import com.example.d_tracker_android.R
import com.example.d_tracker_android.models.TrackerData
import java.net.HttpURLConnection
import java.net.URL

class TrackerApiService(private val context: Context) {
    companion object {
        private const val CONNECT_TIMEOUT = 30000
        private const val READ_TIMEOUT = 30000
    }

    suspend fun sendData(data: TrackerData): Int {
        val urlFromSettings = context.getString(R.string.server_url)
        val url = URL(urlFromSettings)
        
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("authorization-key", context.getString(R.string.authorization_key))
            
            outputStream.use { 
                it.write(data.toJson().toByteArray()) 
            }
        }.responseCode
    }

    private fun TrackerData.toJson(): String = """
        {
            "power": $power,
            "latitude": $latitude,
            "longitude": $longitude,
            "steps": $steps
        }
    """.trimIndent()
} 