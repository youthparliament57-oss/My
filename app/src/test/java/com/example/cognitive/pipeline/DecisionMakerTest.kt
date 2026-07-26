package com.example.cognitive.pipeline

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DecisionMakerTest {

    private lateinit var decisionMaker: DecisionMaker

    @Before
    fun setup() {
        decisionMaker = DecisionMaker(
            CriteriaAnalyzer(),
            RiskAssessor(),
            PreferenceAligner()
        )
    }

    @Test
    fun testDecision() {
        val options = listOf("Phone A", "Phone B")
        val criteria = listOf("Camera", "Battery")
        val result = decisionMaker.decide(options, criteria, emptyList(), emptyMap())
        assertEquals("Phone A", result) // First option wins due to dummy logic
    }
}
