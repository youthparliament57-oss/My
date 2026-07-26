package com.example.agent

import com.example.agent.automation.AutomationEngine
import com.example.agent.automation.AccessibilityPermissionManager
import com.example.agent.guard.BankingAppGuard
import com.example.agent.services.ProbeServices
import com.example.agent.services.TelephonyServices
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentFacade @Inject constructor(
    val automation: AutomationEngine,
    val telephony: TelephonyServices,
    val probes: ProbeServices,
    val guard: BankingAppGuard,
    val permissionManager: AccessibilityPermissionManager
) {
    // Top-level entry points for Module 9 Agent actions
    
    suspend fun getStatusSummary(): String {
        val battery = probes.battery.getBatteryLevel()
        val thermal = probes.thermal.getThermalStatus()
        val inCall = telephony.call.isCallActive()
        val sim = telephony.sim.getSimInfo()
        val movement = probes.accelerometer.getMovementStatus()
        
        return "Agent Ready. Battery: $battery%, Thermal: $thermal, In-Call: $inCall, SIM: $sim, Status: $movement"
    }
}
