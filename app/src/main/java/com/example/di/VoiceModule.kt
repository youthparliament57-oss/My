package com.example.di

import com.example.brain.voice.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {
    // Standard provides are handled by @Inject @Singleton on the classes themselves
    // since they are not interfaces.
}
