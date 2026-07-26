package com.example.cognitive.pipeline

import com.example.cognitive.models.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfidenceModulatorTest {

    private lateinit var modulator: ConfidenceModulator

    @Before
    fun setup() {
        modulator = ConfidenceModulator()
    }

    @Test
    fun testConfidentHighConfidence() {
        val result = modulator.modulate("Yes.", 0.9f, Tone.CONFIDENT)
        assertTrue(result.contains("I am certain"))
    }

    @Test
    fun testConfidentLowConfidence() {
        val result = modulator.modulate("Yes.", 0.4f, Tone.CONFIDENT)
        assertTrue(result.contains("I think"))
    }

    @Test
    fun testTentative() {
        val result = modulator.modulate("Yes.", 0.8f, Tone.TENTATIVE)
        assertTrue(result.contains("Based on my analysis"))
    }
}
