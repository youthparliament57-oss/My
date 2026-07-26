package com.example.agent.di

import com.example.agent.automation.*
import com.example.agent.services.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {

    @Binds
    @Singleton
    abstract fun bindCallService(impl: AndroidCallService): CallService

    @Binds
    @Singleton
    abstract fun bindSmsService(impl: AndroidSmsService): SmsService

    @Binds
    @Singleton
    abstract fun bindContactService(impl: AndroidContactService): ContactService

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindBatteryProbe(impl: AndroidBatteryProbe): BatteryProbe

    @Binds
    @Singleton
    abstract fun bindThermalProbe(impl: AndroidThermalProbe): ThermalProbe

    @Binds
    @Singleton
    abstract fun bindAccelerometerProbe(impl: AndroidAccelerometerProbe): AccelerometerProbe

    @Binds
    @Singleton
    abstract fun bindSimService(impl: AndroidSimService): SimService

    @Binds
    @Singleton
    abstract fun bindCallLogService(impl: AndroidCallLogService): CallLogService

    @Binds
    @Singleton
    abstract fun bindSmsHistoryService(impl: AndroidSmsHistoryService): SmsHistoryService

    @Binds
    @Singleton
    abstract fun bindAutomationProvider(impl: AutomationEngine): AutomationProvider

    companion object {
        @Provides
        @Singleton
        fun provideNotificationRegistry(): com.example.agent.notifications.NotificationRegistry {
            return com.example.agent.notifications.NousNotificationListenerService.registry
        }

        @Provides
        @Singleton
        fun provideAccessibilityServiceRegistry(): AccessibilityServiceRegistry {
            return NousAccessibilityService.registry
        }
    }
}
