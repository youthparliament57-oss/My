package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisionFacade @Inject constructor(
    private val ocrEngine: OcrEngine,
    private val barcodeScanner: BarcodeScannerEngine,
    private val faceDetection: FaceDetectionEngine,
    private val objectDetection: ObjectDetectionEngine,
    private val visualMemory: VisualEpisodicMemory,
    private val graphIntegrator: GraphRagIntegrator,
    private val facePrivacyGuard: FacePrivacyGuard,
    private val screenReader: ScreenReaderEngine,
    private val documentScanner: DocumentScannerEngine
) {

    suspend fun analyzeScene(bitmap: Bitmap): VisionSceneResult {
        Log.i("VisionFacade", "Starting comprehensive scene analysis...")
        
        val ocrText = ocrEngine.recognizeText(bitmap)
        val barcodes = barcodeScanner.scanBarcode(bitmap)
        val rawFaces = faceDetection.detectFaces(bitmap)
        val objects = objectDetection.detectObjects(bitmap)

        // Strategy 6.7: Privacy scrubbing
        val faces = facePrivacyGuard.scrubBiometrics(rawFaces)

        val result = VisionSceneResult(
            ocrText = ocrText,
            barcodes = barcodes,
            faces = faces,
            objects = objects
        )

        // Strategy 6.1: Graph-RAG Integration
        graphIntegrator.integrateVisionResult(result)

        // Store in episodic memory (Redact if sensitive)
        if (!facePrivacyGuard.isSensitiveContext(faces)) {
            visualMemory.storeFrame(bitmap, result.toString())
        } else {
            Log.w("VisionFacade", "Redacting frame from visual memory due to sensitive context (faces detected).")
        }

        return result
    }

    suspend fun analyzeScreenContent(bitmap: Bitmap): ScreenReaderEngine.ScreenContext {
        return screenReader.analyzeScreen(bitmap)
    }

    suspend fun scanDocument(bitmap: Bitmap): DocumentScannerEngine.ScannedDocument? {
        return documentScanner.scanDocument(bitmap)
    }

    data class VisionSceneResult(
        val ocrText: String?,
        val barcodes: List<BarcodeScannerEngine.BarcodeResult>,
        val faces: FaceDetectionEngine.FaceMetadata,
        val objects: List<ObjectDetectionEngine.DetectedObject>
    )
}
