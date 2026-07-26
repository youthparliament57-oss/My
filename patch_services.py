import sys

content1 = """package com.example.agent.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AccessibilityServiceRegistry {
    private var service: NousAccessibilityService? = null
    
    fun setService(s: NousAccessibilityService?) {
        service = s
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
        // Handle events if needed for UI understanding
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
open("app/src/main/java/com/example/agent/automation/NousAccessibilityService.kt", "w").write(content1)

content2 = """package com.example.agent.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class NotificationMetadata(
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long
)

class NotificationRegistry {
    private var service: NousNotificationListenerService? = null
    
    private val _notifications = MutableSharedFlow<NotificationMetadata>(extraBufferCapacity = 50)
    val notifications: SharedFlow<NotificationMetadata> = _notifications
    
    fun setService(s: NousNotificationListenerService?) {
        service = s
    }
    
    fun isEnabled() = service != null
    
    fun emitNotification(metadata: NotificationMetadata) {
        _notifications.tryEmit(metadata)
    }
}

class NousNotificationListenerService : NotificationListenerService() {
    companion object {
        val registry = NotificationRegistry()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("NousNotification", "Notification Listener Connected")
        registry.setService(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title")
            val text = extras.getCharSequence("android.text")?.toString()
            
            val metadata = NotificationMetadata(
                packageName = packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            Log.d("NousNotification", "New Notification from $packageName: $title - $text")
            registry.emitNotification(metadata)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        registry.setService(null)
    }
}
"""
open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt", "w").write(content2)
print("Patched Services")
