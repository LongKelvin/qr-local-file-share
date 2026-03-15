package com.example.qlfs.di

import android.content.Context
import com.example.qlfs.network.HotspotManager
import com.example.qlfs.network.NetworkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager {
        return NetworkManager(context)
    }

    @Provides
    @Singleton
    fun provideHotspotManager(@ApplicationContext context: Context): HotspotManager {
        return HotspotManager(context)
    }
}
