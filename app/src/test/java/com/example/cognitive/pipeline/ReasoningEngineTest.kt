package com.example.cognitive.pipeline

import com.example.brain.BrainContext
import com.example.persona.Persona
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReasoningEngineTest {

    private lateinit var engine: ReasoningEngine
    private lateinit var selfCorrector: SelfCorrector

    @Before
    fun setup() {
        selfCorrector = SelfCorrector()
        engine = ReasoningEngine(selfCorrector)
    }

    @Test
    fun testReasonStepByStep() = runBlocking {
        val mockContext = BrainContext()
        val steps = engine.reasonStepByStep(listOf("Task 1", "Task 2"), mockContext)
        assertEquals(2, steps.size)
        assertEquals("Reasoning about: Task 1", steps[0].stepText)
    }
}
