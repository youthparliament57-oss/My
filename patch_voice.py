import sys

content = open("app/src/main/java/com/example/brain/voice/VoiceOrchestrator.kt").read()

target1 = """    init {
        scope.launch {
            audioCapture.audioChunks.collect { chunk ->
                if (_voiceState.value == VoiceState.LISTENING) {
                    isOwnerVerified = speakerRecognizer.verify(chunk)
                }
            }
        }"""
        
replacement1 = """    private val accumulatedAudio = mutableListOf<ShortArray>()
    private var monitorJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            audioCapture.audioChunks.collect { chunk ->
                if (_voiceState.value == VoiceState.LISTENING) {
                    accumulatedAudio.add(chunk)
                } else if (_voiceState.value == VoiceState.IDLE) {
                    accumulatedAudio.clear()
                }
            }
        }"""

if target1 in content:
    content = content.replace(target1, replacement1)
else:
    print("Target 1 not found")

target2 = """    private fun onSpeechCaptured(text: String) {
        Log.i("VoiceOrchestrator", "Speech captured: $text. Transitioning to THINKING.")
        _voiceState.value = VoiceState.THINKING
        
        // Play filler to manage user expectation
        conversationEngine.playFiller(activePersona)
        
        scope.launch(Dispatchers.Default) {
            val response = brainFacade.processQuery(text)
            launch(Dispatchers.Main) {
                _voiceState.value = VoiceState.SPEAKING
                ttsEngine.speak(
                    text = response.rawText,
                    persona = activePersona,
                    emotionName = response.detectedEmotion
                )
            }
        }
    }"""

replacement2 = """    private fun onSpeechCaptured(text: String) {
        Log.i("VoiceOrchestrator", "Speech captured: $text. Transitioning to THINKING.")
        _voiceState.value = VoiceState.THINKING
        
        // Combine audio and verify speaker
        val fullAudioSize = accumulatedAudio.sumOf { it.size }
        val fullBuffer = ShortArray(fullAudioSize)
        var offset = 0
        for (chunk in accumulatedAudio) {
            System.arraycopy(chunk, 0, fullBuffer, offset, chunk.size)
            offset += chunk.size
        }
        isOwnerVerified = speakerRecognizer.verify(fullBuffer)
        accumulatedAudio.clear()
        
        // Play filler to manage user expectation
        conversationEngine.playFiller(activePersona)
        
        scope.launch(Dispatchers.IO) {
            try {
                val response = brainFacade.processQuery(text)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _voiceState.value = VoiceState.SPEAKING
                    ttsEngine.speak(
                        text = response.rawText,
                        persona = activePersona,
                        emotionName = response.detectedEmotion
                    )
                }
            } catch (e: Exception) {
                Log.e("VoiceOrchestrator", "Failed to process query", e)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }"""

if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("Target 2 not found")

target3 = """        scope.launch {
            var lastSilenceTime = System.currentTimeMillis()
            while (true) {
                if (_voiceState.value == VoiceState.LISTENING) {
                    if (audioCapture.isUserSpeaking) {
                        lastSilenceTime = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastSilenceTime > 800) {
                            Log.i("VoiceOrchestrator", "EOS detected (800ms silence). Stopping STT.")
                            sttEngine.stopListening() // This will trigger onSpeechCaptured
                            lastSilenceTime = System.currentTimeMillis()
                        }
                    }
                } else {
                    lastSilenceTime = System.currentTimeMillis()
                }
                kotlinx.coroutines.delay(100)
            }
        }"""
        
replacement3 = """        monitorJob = scope.launch {
            var lastSilenceTime = System.currentTimeMillis()
            while (kotlinx.coroutines.isActive) {
                if (_voiceState.value == VoiceState.LISTENING) {
                    if (audioCapture.isUserSpeaking) {
                        lastSilenceTime = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastSilenceTime > 800) {
                            Log.i("VoiceOrchestrator", "EOS detected (800ms silence). Stopping STT.")
                            sttEngine.stopListening() // This will trigger onSpeechCaptured
                            lastSilenceTime = System.currentTimeMillis()
                        }
                    }
                } else {
                    lastSilenceTime = System.currentTimeMillis()
                }
                kotlinx.coroutines.delay(100)
            }
        }"""
        
if target3 in content:
    content = content.replace(target3, replacement3)
else:
    print("Target 3 not found")

target4 = """    fun stopEverything() {
        sttEngine.stopListening()
        ttsEngine.stop()
        wakeWordDetector.stopDetection()
        _voiceState.value = VoiceState.IDLE
    }"""

replacement4 = """    fun stopEverything() {
        monitorJob?.cancel()
        sttEngine.stopListening()
        ttsEngine.stop()
        wakeWordDetector.stopDetection()
        _voiceState.value = VoiceState.IDLE
    }"""
    
if target4 in content:
    content = content.replace(target4, replacement4)
else:
    print("Target 4 not found")

open("app/src/main/java/com/example/brain/voice/VoiceOrchestrator.kt", "w").write(content)
print("VoiceOrchestrator patched")
