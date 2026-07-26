package com.example.agent.automation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: AccessibilityServiceRegistry
) {
    enum class PermissionState {
        NOT_GRANTED, RATIONALE_SHOWN, SETTINGS_OPENED, GRANTED
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)

    private var currentState: PermissionState
        get() {
            val stateName = prefs.getString("current_state", PermissionState.NOT_GRANTED.name)
            return PermissionState.valueOf(stateName ?: PermissionState.NOT_GRANTED.name)
        }
        set(value) {
            prefs.edit().putString("current_state", value.name).apply()
        }

    private var lastRefusalTimeMs: Long
        get() = prefs.getLong("last_refusal_time", 0L)
        set(value) = prefs.edit().putLong("last_refusal_time", value).apply()

    fun isPermissionGranted(): Boolean {
        return registry.isServiceEnabled()
    }

    fun startTripleOptInFlow(
        onRationale: () -> Unit,
        onOpenSettings: () -> Unit,
        onFinalConfirm: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (now - lastRefusalTimeMs < 24 * 60 * 60 * 1000) {
            Log.i("PermissionManager", "In 24h cooldown. Not prompting.")
            return
        }

        when (currentState) {
            PermissionState.NOT_GRANTED -> {
                onRationale()
                currentState = PermissionState.RATIONALE_SHOWN
            }
            PermissionState.RATIONALE_SHOWN -> {
                onOpenSettings()
                openAccessibilitySettings()
                currentState = PermissionState.SETTINGS_OPENED
            }
            PermissionState.SETTINGS_OPENED -> {
                onFinalConfirm()
            }
            PermissionState.GRANTED -> {
                Log.i("PermissionManager", "Permission already granted")
            }
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun confirmGrant(): Boolean {
        return if (isPermissionGranted()) {
            currentState = PermissionState.GRANTED
            true
        } else {
            false
        }
    }

    fun markRefused() {
        lastRefusalTimeMs = System.currentTimeMillis()
        reset()
    }

    fun reset() {
        currentState = PermissionState.NOT_GRANTED
    }
}
