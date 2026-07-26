import sys

content = open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt").read()
if "@Singleton\nclass NotificationRegistry" not in content:
    content = content.replace("class NotificationRegistry {", "@javax.inject.Singleton\nclass NotificationRegistry {\n    @Volatile")
open("app/src/main/java/com/example/agent/notifications/NousNotificationListenerService.kt", "w").write(content)
print("Patched NotificationRegistry")
