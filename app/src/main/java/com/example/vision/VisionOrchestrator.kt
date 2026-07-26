package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import com.example.brain.BrainFacade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisionOrchestrator @Inject constructor(
    private val visionFacade: VisionFacade,
    private val brainFacade: BrainFacade,
    private val thermalGovernor: ThermalGovernor,
    private val batteryController: BatteryAdaptiveController,
    private val lmkEvictionOrchestrator: LmkEvictionOrchestrator
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    
    private val _isVisionActive = MutableStateFlow(false)
    val isVisionActive = _isVisionActive.asStateFlow()

    private val _lastSceneResult = MutableStateFlow<VisionFacade.VisionSceneResult?>(null)
    val lastSceneResult = _lastSceneResult.asStateFlow()

    fun startVision() {
        _isVisionActive.value = true
        Log.i("VisionOrchestrator", "Vision system activated.")
    }

    fun stopVision() {
        _isVisionActive.value = false
        Log.i("VisionOrchestrator", "Vision system deactivated.")
    }

    private var lastProcessTime = 0L

    fun processFrame(bitmap: Bitmap) {
        if (!_isVisionActive.value) return

        val now = System.currentTimeMillis()
        val throttleMs = if (thermalGovernor.shouldThrottled()) 500L else 100L
        
        if (now - lastProcessTime < throttleMs) return
        lastProcessTime = now

        scope.launch {
            val result = visionFacade.analyzeScene(bitmap)
            _lastSceneResult.value = result

            // Strategy 6.1: Visual Context Integration
            if (result.objects.isNotEmpty() || !result.ocrText.isNullOrBlank()) {
                val contextSummary = buildContextSummary(result)
                Log.d("VisionOrchestrator", "Updating Brain with visual context: $contextSummary")
                // In a real implementation, this would be injected into the LLM's dynamic context
            }
        }
    }

    private fun buildContextSummary(result: VisionFacade.VisionSceneResult): String {
        val sb = StringBuilder("Visual Context: ")
        if (!result.ocrText.isNullOrBlank()) sb.append("Text detected: ${result.ocrText.take(50)}. ")
        if (result.objects.isNotEmpty()) sb.append("Objects: ${result.objects.joinToString { it.label }}. ")
        if (result.faces.count > 0) sb.append("${result.faces.count} faces present. ")
        return sb.toString()
    }
}
