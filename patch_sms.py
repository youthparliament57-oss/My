import sys

content = """package com.example.agent.services

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSmsService @Inject constructor(
    @ApplicationContext private val context: Context
) : SmsService {

    override fun sendSms(phoneNumber: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("SEND_SMS permission not granted")
            }
            
            val smsManager = context.getSystemService(SmsManager::class.java)
            
            val sentIntent = Intent("SMS_SENT")
            val deliveredIntent = Intent("SMS_DELIVERED")
            
            val sentPI = PendingIntent.getBroadcast(context, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE)
            val deliveredPI = PendingIntent.getBroadcast(context, 0, deliveredIntent, PendingIntent.FLAG_IMMUTABLE)

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentPIs = ArrayList<PendingIntent>()
                val deliveredPIs = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    sentPIs.add(sentPI)
                    deliveredPIs.add(deliveredPI)
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentPIs, deliveredPIs)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI)
            }
        } catch (e: SecurityException) {
            Log.e("SmsService", "SEND_SMS permission not granted")
        } catch (e: Exception) {
            Log.e("SmsService", "Failed to send SMS: ${e.message}")
        }
    }
}
"""
open("app/src/main/java/com/example/agent/services/AndroidSmsService.kt", "w").write(content)
print("Patched AndroidSmsService.kt")
