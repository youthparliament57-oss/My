import sys

content = open("app/src/main/java/com/example/brain/voice/SpeakerRecognizer.kt").read()

target = """    fun verify(audioData: ByteArray): Boolean {
        if (enrolledPrint == null) return true // No enrollment = everyone allowed

        val currentFeatures = extractFeatures(audioData)
        val similarity = cosineSimilarity(enrolledPrint!!, currentFeatures)
        
        Log.d("SpeakerRecognizer", "Speaker similarity: $similarity")
        return similarity > 0.85 // Threshold (Strategy 5.5)
    }"""

replacement = """    fun verify(audioData: ByteArray): Boolean {
        val print = enrolledPrint ?: return true // No enrollment = everyone allowed

        val currentFeatures = extractFeatures(audioData)
        val similarity = cosineSimilarity(print, currentFeatures)
        
        Log.d("SpeakerRecognizer", "Speaker similarity: $similarity")
        return similarity > 0.85 // Threshold (Strategy 5.5)
    }"""

content = content.replace(target, replacement)
open("app/src/main/java/com/example/brain/voice/SpeakerRecognizer.kt", "w").write(content)
