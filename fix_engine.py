import sys

content = open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt").read()
content = content.replace("    override \n    override fun takeScreenshot", "    override fun takeScreenshot")
open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt", "w").write(content)
print("Fixed AutomationEngine.kt")
