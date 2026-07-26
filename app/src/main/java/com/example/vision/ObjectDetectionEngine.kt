package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectDetectionEngine @Inject constructor() {

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val detector = ObjectDetection.getClient(options)

    data class DetectedObject(
        val label: String,
        val confidence: Float,
        val trackingId: Int?
    )

    suspend fun detectObjects(bitmap: Bitmap): List<DetectedObject> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val results = detector.process(image).await()
            results.map { obj ->
                val label = obj.labels.maxByOrNull { it.confidence }?.text ?: "Unknown"
                val confidence = obj.labels.maxByOrNull { it.confidence }?.confidence ?: 0f
                DetectedObject(label, confidence, obj.trackingId)
            }
        } catch (e: Exception) {
            Log.e("ObjectDetection", "Detection failed", e)
            emptyList()
        }
    }
}
