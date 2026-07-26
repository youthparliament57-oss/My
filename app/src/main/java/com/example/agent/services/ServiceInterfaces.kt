package com.example.agent.services

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject
import javax.inject.Singleton

interface CallService {
    fun placeCall(phoneNumber: String)
    fun endCall()
    fun isCallActive(): Boolean
}

interface SmsService {
    fun sendSms(phoneNumber: String, message: String)
}

interface SmsHistoryService {
    fun getSmsHistory(): List<SmsMessage>
}

interface CallLogService {
    fun getCallLogs(): List<CallLogEntry>
}

data class CallLogEntry(
    val number: String,
    val name: String?,
    val type: Int, // 1: Incoming, 2: Outgoing, 3: Missed
    val date: Long,
    val duration: Long
)

data class SmsMessage(
    val address: String,
    val body: String,
    val date: Long,
    val type: Int // 1 = inbox, 2 = sent
)

interface ContactService {
    fun findContactByName(name: String): List<ContactInfo>
    fun getAllContacts(): List<ContactInfo>
}

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>
)

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
    fun observeLocation(): Flow<Location>
}

interface BatteryProbe {
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
}

interface ThermalProbe {
    fun getThermalStatus(): Int // Using PowerManager.THERMAL_STATUS_*
}

interface AccelerometerProbe {
    fun getMovementStatus(): String
}

interface SimService {
    fun getSimInfo(): String
}

@Singleton
class TelephonyServices @Inject constructor(
    val call: CallService,
    val sms: SmsService,
    val contacts: ContactService,
    val callLog: CallLogService,
    val sim: SimService,
    val smsHistory: SmsHistoryService
)

@Singleton
class ProbeServices @Inject constructor(
    val location: LocationProvider,
    val battery: BatteryProbe,
    val thermal: ThermalProbe,
    val accelerometer: AccelerometerProbe
)
