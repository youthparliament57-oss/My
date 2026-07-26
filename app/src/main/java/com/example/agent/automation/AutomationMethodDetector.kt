package com.example.agent.automation

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
