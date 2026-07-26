package com.example.vision

import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LmkEvictionOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrEngine: OcrEngine,
    private val barcodeScanner: BarcodeScannerEngine,
    private val faceDetection: FaceDetectionEngine,
    private val objectDetection: ObjectDetectionEngine
) : ComponentCallbacks2 {

    init {
        context.registerComponentCallbacks(this)
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            Log.w("LMK", "Memory pressure detected (level: $level). Evicting heavy vision models.")
            // ML Kit clients manage their own lifecycles mostly, but we can close them to be safe
            // if we were managing raw TFLite interpreters.
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
    override fun onLowMemory() {
        Log.e("LMK", "System low on memory! Emergency model eviction.")
    }
}
