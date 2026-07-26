import sys

content = open("app/src/main/java/com/example/brain/voice/SttEngine.kt").read()
target1 = """class SttEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioCapture: AudioCapturePipeline
) : RecognitionListener {"""
replacement1 = """class SttEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioCapture: AudioCapturePipeline,
    private val deltaUpdater: DeltaContextUpdater
) : RecognitionListener {"""

target2 = """    @Inject lateinit var deltaUpdater: DeltaContextUpdater"""
replacement2 = """    // DeltaContextUpdater is now constructor-injected"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)
open("app/src/main/java/com/example/brain/voice/SttEngine.kt", "w").write(content)
