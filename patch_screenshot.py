import sys

# 1. Update AutomationProvider.kt
provider = open("app/src/main/java/com/example/agent/automation/AutomationProvider.kt").read()
provider = provider.replace("fun getUiTree(): UiTreeSnapshot\n}", "fun getUiTree(): UiTreeSnapshot\n    fun takeScreenshot(): Boolean\n}")
open("app/src/main/java/com/example/agent/automation/AutomationProvider.kt", "w").write(provider)

# 2. Update AutomationEngine.kt
engine = open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt").read()
engine_screenshot = """
    override fun takeScreenshot(): Boolean {
        if (!isSafeToAutomate()) return false
        return getProvider().takeScreenshot()
    }
"""
engine = engine.replace("fun getUiTree(): UiTreeSnapshot {", engine_screenshot + "\n    override fun getUiTree(): UiTreeSnapshot {")
open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt", "w").write(engine)

# 3. Update LadbAutomationProvider
ladb = open("app/src/main/java/com/example/agent/automation/LadbStack.kt").read()
ladb_screenshot = """
    override fun takeScreenshot(): Boolean {
        val res = connection.executeShellCommand("screencap -p /sdcard/screenshot.png")
        return res.startsWith("Success")
    }
"""
ladb = ladb.replace("override fun getUiTree(): UiTreeSnapshot {", ladb_screenshot + "\n    override fun getUiTree(): UiTreeSnapshot {")
open("app/src/main/java/com/example/agent/automation/LadbStack.kt", "w").write(ladb)

# 4. Update AccessibilityAutomationProvider
access = open("app/src/main/java/com/example/agent/automation/AccessibilityAutomationProvider.kt").read()
access_screenshot = """
    override fun takeScreenshot(): Boolean {
        val service = registry.getService() ?: return false
        // Android 11+ allows taking screenshots via accessibility
        var result = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            service.takeScreenshot(android.view.Display.DEFAULT_DISPLAY, java.util.concurrent.Executors.newSingleThreadExecutor(), object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) { result = true }
                override fun onFailure(errorCode: Int) { result = false }
            })
        }
        return result
    }
"""
access = access.replace("override fun getUiTree(): UiTreeSnapshot {", access_screenshot + "\n    override fun getUiTree(): UiTreeSnapshot {")
open("app/src/main/java/com/example/agent/automation/AccessibilityAutomationProvider.kt", "w").write(access)

print("Patched takeScreenshot")
