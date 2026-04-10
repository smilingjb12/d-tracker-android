package com.example.d_tracker_android.data.remote

import com.example.d_tracker_android.domain.model.TrackerData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface TrackerApi {
    @POST
    suspend fun sendTrackerData(
        @Url url: String,
        @Header("authorization-key") authorizationKey: String,
        @Body data: TrackerData
    ): Response<Unit>
}
