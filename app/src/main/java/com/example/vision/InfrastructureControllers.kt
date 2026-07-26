package com.example.vision

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalGovernor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun shouldThrottled(): Boolean {
        val thermalState = powerManager.currentThermalStatus
        return thermalState >= PowerManager.THERMAL_STATUS_MODERATE
    }

    fun getRecommendedFps(): Int {
        return if (shouldThrottled()) 10 else 30
    }
}

@Singleton
class BatteryAdaptiveController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isLowPowerMode(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level < 20
    }
}
