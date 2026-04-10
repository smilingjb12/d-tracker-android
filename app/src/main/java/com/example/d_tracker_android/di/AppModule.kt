package com.example.d_tracker_android.di

import android.content.Context
import com.example.d_tracker_android.core.permissions.DefaultPermissionOrchestrator
import com.example.d_tracker_android.core.permissions.PermissionOrchestrator
import com.example.d_tracker_android.core.work.TrackingScheduler
import com.example.d_tracker_android.core.work.TrackingWorkScheduler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindTrackingScheduler(
        impl: TrackingWorkScheduler
    ): TrackingScheduler

    @Binds
    abstract fun bindPermissionOrchestrator(
        impl: DefaultPermissionOrchestrator
    ): PermissionOrchestrator
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidersModule {
    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}
