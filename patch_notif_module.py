import sys

content = open("app/src/main/java/com/example/agent/di/AgentModule.kt").read()
if "provideNotificationRegistry" not in content:
    content = content.replace("fun provideAccessibilityServiceRegistry(): AccessibilityServiceRegistry {", "@Provides\n        @Singleton\n        fun provideNotificationRegistry(): com.example.agent.notifications.NotificationRegistry {\n            return com.example.agent.notifications.NousNotificationListenerService.registry\n        }\n\n        @Provides\n        @Singleton\n        fun provideAccessibilityServiceRegistry(): AccessibilityServiceRegistry {")
open("app/src/main/java/com/example/agent/di/AgentModule.kt", "w").write(content)
print("Patched AgentModule for NotificationRegistry")
