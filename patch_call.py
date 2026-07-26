import sys

content = """package com.example.agent.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidCallService @Inject constructor(
    @ApplicationContext private val context: Context
) : CallService {

    override fun placeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("CALL_PHONE permission not granted")
        }
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to place call", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun endCall() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("ANSWER_PHONE_CALLS permission not granted")
        }
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        try {
            tm.endCall()
        } catch (e: Exception) {
            // Ignored, may require Android 9+ or different permissions
        }
    }

    override fun isCallActive(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return try { tm.isInCall } catch (e: SecurityException) { false }
    }
}
"""
open("app/src/main/java/com/example/agent/services/AndroidCallService.kt", "w").write(content)
print("Patched AndroidCallService.kt")
