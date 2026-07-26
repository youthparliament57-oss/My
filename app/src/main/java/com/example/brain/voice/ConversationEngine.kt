package com.example.brain.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsEngine: TtsEngine
) {
    private val random = Random()
    
    private val fillers = listOf(
        "Hmm...",
        "Let me think...",
        "One moment...",
        "Analyzing...",
        "Processing context...",
        "Searching memory nodes..."
    )

    fun playFiller(persona: com.example.persona.Persona) {
        val filler = fillers[random.nextInt(fillers.size)]
        Log.d("ConversationEngine", "Playing filler: $filler")
        ttsEngine.speak(filler, persona, "THOUGHTFUL")
    }
}
