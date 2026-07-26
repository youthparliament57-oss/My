package com.example.agent.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class NotificationMetadata(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long
)

@javax.inject.Singleton
class NotificationRegistry {
    @Volatile
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

    fun dismissNotification(key: String) {
        service?.cancelNotification(key)
    }

    fun dismissAllFromPackage(packageName: String) {
        val sbnList = service?.activeNotifications ?: return
        for (sbn in sbnList) {
            if (sbn.packageName == packageName) {
                service?.cancelNotification(sbn.key)
            }
        }
    }

    fun replyToNotification(key: String, replyText: String): Boolean {
        val sbnList = service?.activeNotifications ?: return false
        val sbn = sbnList.find { it.key == key } ?: return false
        val actions = sbn.notification.actions ?: return false
        
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                val intent = Intent()
                val bundle = Bundle()
                bundle.putCharSequence(remoteInput.resultKey, replyText)
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                try {
                    action.actionIntent.send(service, 0, intent)
                    return true
                } catch (e: Exception) {
                    Log.e("NousNotification", "Failed to send reply", e)
                }
            }
        }
        return false
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
            val title = extras.getString(android.app.Notification.EXTRA_TITLE)
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
            
            val metadata = NotificationMetadata(
                key = it.key,
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
