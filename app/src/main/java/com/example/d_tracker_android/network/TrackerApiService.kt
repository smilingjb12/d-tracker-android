package com.example.d_tracker_android.network

import android.content.Context
import com.example.d_tracker_android.R
import com.example.d_tracker_android.models.TrackerData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerApiService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    private val json = Json { encodeDefaults = true }

    suspend fun sendData(data: TrackerData): Int = withContext(Dispatchers.IO) {
        val url = context.getString(R.string.server_url)
        val body = json.encodeToString(data)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("authorization-key", context.getString(R.string.authorization_key))
            .build()

        client.newCall(request).execute().use { response ->
            response.code
        }
    }
}
