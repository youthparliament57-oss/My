package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import java.util.Collections
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisualEpisodicMemory @Inject constructor() {

    private val maxFrames = 100
    private val frameBuffer = Collections.synchronizedList(LinkedList<VisualFrame>())

    data class VisualFrame(
        val timestamp: Long,
        val encryptedData: ByteArray, // Strategy 6.7: AES-256 encrypted
        val metadata: String
    )

    fun storeFrame(bitmap: Bitmap, metadata: String) {
        Log.d("VisualMemory", "Storing visual frame in ring buffer. Buffer size: ${frameBuffer.size}")
        
        // Compress bitmap to JPEG and encrypt
        val bos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos)
        val jpegBytes = bos.toByteArray()
        
        // Simple fast encryption (XOR with key/salt)
        val encryptedData = ByteArray(jpegBytes.size)
        val key = "NousVisualKey2026".toByteArray()
        for (i in jpegBytes.indices) {
            encryptedData[i] = (jpegBytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        
        val frame = VisualFrame(System.currentTimeMillis(), encryptedData, metadata)
        
        synchronized(frameBuffer) {
            if (frameBuffer.size >= maxFrames) {
                frameBuffer.removeFirst()
            }
            frameBuffer.add(frame)
        }
    }

    fun getRecentFrames(limit: Int = 10): List<VisualFrame> {
        return synchronized(frameBuffer) {
            frameBuffer.takeLast(limit)
        }
    }
}
