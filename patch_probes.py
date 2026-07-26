import sys

content = """package com.example.agent.services

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.BatteryManager
import android.os.Looper
import android.os.PowerManager
import android.telephony.SubscriptionManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.sqrt

@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        return try {
            suspendCancellableCoroutine { cont ->
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    cont.resume(location)
                }.addOnFailureListener {
                    cont.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<Location> = callbackFlow {
        val request = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            // Permission denied
        }
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}

@Singleton
class AndroidBatteryProbe @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryProbe {
    override fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    override fun isCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }
}

@Singleton
class AndroidThermalProbe @Inject constructor(
    @ApplicationContext private val context: Context
) : ThermalProbe {
    override fun getThermalStatus(): Int {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.currentThermalStatus
    }
}

@Singleton
class AndroidAccelerometerProbe @Inject constructor(
    @ApplicationContext private val context: Context
) : AccelerometerProbe, SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastMovementStatus = "Stationary"
    
    init {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun getMovementStatus(): String {
        return lastMovementStatus
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            val acceleration = sqrt((x * x + y * y + z * z).toDouble())
            
            // 9.8 is normal gravity. Threshold for movement could be > 10.5 or < 9.0
            lastMovementStatus = if (acceleration > 10.5 || acceleration < 9.0) {
                "Moving"
            } else {
                "Stationary"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Singleton
class AndroidSimService @Inject constructor(
    @ApplicationContext private val context: Context
) : SimService {
    @SuppressLint("MissingPermission")
    override fun getSimInfo(): String {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        return try {
            val subs = sm.activeSubscriptionInfoList
            if (subs.isNullOrEmpty()) {
                "No active SIM"
            } else {
                subs.joinToString(", ") { "SIM ${it.simSlotIndex}: ${it.carrierName}" }
            }
        } catch (e: Exception) {
            "SIM Info Unavailable due to permissions"
        }
    }
}
"""
open("app/src/main/java/com/example/agent/services/ProbeImpls.kt", "w").write(content)
print("Patched ProbeImpls.kt")
