package com.example.cognitive.pipeline

import com.example.cognitive.models.UncertaintyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UncertaintyAwarenessTest {

    private lateinit var awareness: UncertaintyAwareness

    @Before
    fun setup() {
        awareness = UncertaintyAwareness()
    }

    @Test
    fun testHighConfidence() {
        assertNull(awareness.checkConfidence(0.9f))
    }

    @Test
    fun testVeryLowConfidence() {
        val result = awareness.checkConfidence(0.2f)
        assertEquals(UncertaintyAction.Admit, result)
    }

    @Test
    fun testMediumConfidenceFactual() {
        val result = awareness.checkConfidence(0.5f, "factual")
        assertEquals(UncertaintyAction.Defer, result)
    }

    @Test
    fun testMediumConfidenceOpinion() {
        val result = awareness.checkConfidence(0.5f, "opinion")
        assertEquals(UncertaintyAction.Ask, result)
    }
}
