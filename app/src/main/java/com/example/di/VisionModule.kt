package com.example.di

import com.example.vision.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VisionModule {
    // Singleton engines are automatically provided via @Inject @Singleton
}
