import sys

content = open("app/src/main/java/com/example/brain/voice/VoiceOrchestrator.kt").read()
content = content.replace("private val accumulatedAudio = mutableListOf<ShortArray>()", "private val accumulatedAudio = mutableListOf<ByteArray>()")
content = content.replace("val fullBuffer = ShortArray(fullAudioSize)", "val fullBuffer = ByteArray(fullAudioSize)")
open("app/src/main/java/com/example/brain/voice/VoiceOrchestrator.kt", "w").write(content)
