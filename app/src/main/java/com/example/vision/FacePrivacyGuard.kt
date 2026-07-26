package com.example.vision

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacePrivacyGuard @Inject constructor() {

    /**
     * Strategy 6.7: Prevents face biometric storage.
     * This ensures that while we can detect faces and their states (smiling, blinking),
     * we never extract or store the underlying 128-d or 512-d embeddings that could 
     * uniquely identify a person.
     */
    fun scrubBiometrics(faceData: FaceDetectionEngine.FaceMetadata): FaceDetectionEngine.FaceMetadata {
        Log.d("FacePrivacyGuard", "Scrubbing potential biometric markers from vision payload.")
        // Currently, FaceMetadata only contains counts and probabilities, which are safe.
        // If FaceDetectionEngine ever added 'landmarks' or 'embeddings', they would be removed here.
        return faceData
    }

    /**
     * Prevents storing raw visual frames containing high-confidence faces in 
     * long-term semantic memory unless explicitly authorized by the user.
     */
    fun isSensitiveContext(faces: FaceDetectionEngine.FaceMetadata): Boolean {
        return faces.count > 0
    }
}
