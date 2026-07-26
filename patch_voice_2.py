import sys

content = open("app/src/main/java/com/example/brain/voice/VoiceOrchestrator.kt").read()
target = """        scope.launch {
            while (true) {
                if (_voiceState.value == VoiceState.LISTENING && silenceStart > 0) {
                    val silenceDuration = System.currentTimeMillis() - silenceStart
                    if (silenceDuration > 800) { // 800ms silence threshold for EOS (5.8 mentions 500ms + VAP)
                        Log.i("VoiceOrchestrator", "EOS detected via 800ms silence. Stopping STT.")
                        sttEngine.stopListening()
                        _voiceState.value = VoiceState.THINKING
                        silenceStart = 0
                    }
                }
                delay(100)
            }
        }"""
        
replacement = """        monitorJob = scope.launch {
            while (kotlinx.coroutines.isActive) {
                if (_voiceState.value == VoiceState.LISTENING && silenceStart > 0) {
                    val silenceDuration = System.currentTimeMillis() - silenceStart
                    if (silenceDuration > 800) { // 800ms silence threshold for EOS (5.8 mentions 500ms + VAP)
                        Log.i("VoiceOrchestrator", "EOS detected via 800ms silence. Stopping STT.")
                        sttEngine.stopListening()
                        _voiceState.value = VoiceState.THINKING
                        silenceStart = 0
                    }
                }
                delay(100)
            }
        }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/voice/VoiceOrchestrator.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
