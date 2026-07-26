import sys

content = """package com.example.agent.automation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationMethodDetector @Inject constructor(
    private val ladbConnection: LadbConnection,
    private val accessibilityRegistry: AccessibilityServiceRegistry
) {
    enum class Method {
        LADB, ACCESSIBILITY, NONE
    }

    fun getBestMethod(): Method {
        if (ladbConnection.getState() == LadbConnection.State.READY) {
            return Method.LADB
        }
        if (accessibilityRegistry.isServiceEnabled()) {
            return Method.ACCESSIBILITY
        }
        return Method.NONE
    }
}
"""
open("app/src/main/java/com/example/agent/automation/AutomationMethodDetector.kt", "w").write(content)

engine = open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt").read()
# Add detector to AutomationEngine
engine = engine.replace("@Inject constructor(", "@Inject constructor(\n    private val detector: AutomationMethodDetector,")
engine = engine.replace("""    fun getProvider(): AutomationProvider {
        return if (ladbConnection.getState() == LadbConnection.State.READY) {
            ladbProvider
        } else {
            accessibilityProvider
        }
    }""", """    fun getProvider(): AutomationProvider {
        return when (detector.getBestMethod()) {
            AutomationMethodDetector.Method.LADB -> ladbProvider
            else -> accessibilityProvider
        }
    }""")
open("app/src/main/java/com/example/agent/automation/AutomationEngine.kt", "w").write(engine)
print("Patched detector")
