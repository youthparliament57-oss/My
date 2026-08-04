package com.example.cognitive.pipeline

import com.example.cognitive.models.AmbiguityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ClarificationEngineTest {

    private lateinit var engine: ClarificationEngine

    @Before
    fun setup() {
        engine = ClarificationEngine(FakeNousRepository())
    }

    @Test
    fun testVagueReference() = kotlinx.coroutines.runBlocking {
        val result = engine.checkAmbiguity("Can you call her?")
        assertNotNull(result)
        assertEquals(AmbiguityType.VAGUE_REFERENCE, result?.ambiguityType)
    }

    @Test
    fun testMissingParameter() = kotlinx.coroutines.runBlocking {
        val result = engine.checkAmbiguity("Set an alarm")
        assertNotNull(result)
        assertEquals(AmbiguityType.MISSING_PARAMETER, result?.ambiguityType)
    }

    @Test
    fun testConflictingIntent() = kotlinx.coroutines.runBlocking {
        val result = engine.checkAmbiguity("Turn off the lights but keep it on")
        assertNotNull(result)
        assertEquals(AmbiguityType.CONFLICTING_INTENT, result?.ambiguityType)
    }

    @Test
    fun testNoAmbiguity() = kotlinx.coroutines.runBlocking {
        val result = engine.checkAmbiguity("What is the capital of France?")
        assertNull(result)
    }
}
