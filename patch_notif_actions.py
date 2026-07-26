import sys

content = open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt").read()

imports = """
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
"""

if "import android.app.RemoteInput" not in content:
    content = content.replace("import android.util.Log", "import android.util.Log\n" + imports)

actions_code = """
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
"""

if "fun dismissNotification" not in content:
    content = content.replace("fun emitNotification(metadata: NotificationMetadata) {\n        _notifications.tryEmit(metadata)\n    }", "fun emitNotification(metadata: NotificationMetadata) {\n        _notifications.tryEmit(metadata)\n    }\n" + actions_code)

# Let's add key to NotificationMetadata
if "val key: String" not in content:
    content = content.replace("val packageName: String,", "val key: String,\n    val packageName: String,")
    content = content.replace("packageName = packageName,", "key = it.key,\n                packageName = packageName,")

open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt", "w").write(content)
print("Patched Notification Actions")
