import sys

content = open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt").read()
content = content.replace("extras.getString(\"android.title\")", "extras.getString(android.app.Notification.EXTRA_TITLE)")
content = content.replace("extras.getCharSequence(\"android.text\")", "extras.getCharSequence(android.app.Notification.EXTRA_TEXT)")
open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt", "w").write(content)
print("Patched Notification")
