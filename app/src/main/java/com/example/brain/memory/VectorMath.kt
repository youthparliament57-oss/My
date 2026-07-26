package com.example.brain.memory

import org.json.JSONArray
import kotlin.math.sqrt

object VectorMath {

    fun quantize(floatVector: FloatArray): ByteArray {
        val byteArray = ByteArray(floatVector.size)
        for (i in floatVector.indices) {
            val scaled = (floatVector[i] * 127.0f).toInt()
            byteArray[i] = scaled.coerceIn(-128, 127).toByte()
        }
        return byteArray
    }

    fun byteVectorToJson(vector: ByteArray): String {
        val jsonArray = JSONArray()
        for (b in vector) {
            jsonArray.put(b.toInt())
        }
        return jsonArray.toString()
    }

    fun jsonToByteVector(jsonStr: String): ByteArray {
        if (jsonStr.isEmpty()) return ByteArray(0)
        return try {
            val jsonArray = JSONArray(jsonStr)
            val byteArray = ByteArray(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                byteArray[i] = jsonArray.getInt(i).toByte()
            }
            byteArray
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    fun cosineSimilarity(vectorA: ByteArray, vectorB: ByteArray): Float {
        if (vectorA.isEmpty() || vectorB.isEmpty() || vectorA.size != vectorB.size) return 0.0f
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in vectorA.indices) {
            val valA = vectorA[i].toDouble()
            val valB = vectorB[i].toDouble()
            dotProduct += valA * valB
            normA += valA * valA
            normB += valB * valB
        }
        
        if (normA == 0.0 || normB == 0.0) return 0.0f
        return (dotProduct / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    fun generateMockEmbedding(text: String, dimension: Int = 384): ByteArray {
        // Generates deterministic pseudo-random embedding based on string hash for testing and offline fallback
        val byteArray = ByteArray(dimension)
        val hash = text.hashCode()
        for (i in 0 until dimension) {
            val elementVal = (hash xor (i * 3333333)) % 127
            byteArray[i] = elementVal.toByte()
        }
        return byteArray
    }
}
