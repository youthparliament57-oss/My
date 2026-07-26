package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceDetectionEngine @Inject constructor() {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    data class FaceMetadata(
        val count: Int,
        val smileProbabilities: List<Float>,
        val blinkProbabilities: List<Float>
    )

    suspend fun detectFaces(bitmap: Bitmap): FaceMetadata {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            
            // Privacy Guard: We only return metadata, not the face data itself.
            FaceMetadata(
                count = faces.size,
                smileProbabilities = faces.map { it.smilingProbability ?: 0f },
                blinkProbabilities = faces.map { 
                    (it.leftEyeOpenProbability ?: 0f) * (it.rightEyeOpenProbability ?: 0f) 
                }
            )
        } catch (e: Exception) {
            Log.e("FaceDetection", "Detection failed", e)
            FaceMetadata(0, emptyList(), emptyList())
        }
    }
}
