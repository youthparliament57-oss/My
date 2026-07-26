import sys

content = """package com.example.agent.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import javax.inject.Singleton

@Singleton
class AccessibilityServiceRegistry {
    @Volatile private var service: NousAccessibilityService? = null
    
    fun setService(s: NousAccessibilityService?) {
        this.service = s
    }
    
    fun getService(): NousAccessibilityService? = service
    fun isServiceEnabled(): Boolean = service != null
}

class NousAccessibilityService : AccessibilityService() {
    companion object {
        val registry = AccessibilityServiceRegistry()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("NousAccessibility", "Accessibility Service Connected")
        registry.setService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.let { pkg ->
                Log.d("NousAccessibility", "Window changed: $pkg")
            }
        }
    }

    override fun onInterrupt() {
        Log.w("NousAccessibility", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        registry.setService(null)
    }
}
"""
open("app/src/main/java/com/example/agent/automation/NousAccessibilityService.kt", "w").write(content)
print("Patched NousAccessibilityService.kt")
