package com.example.cognitive.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FuzzyConstraintInterpreterTest {

    private lateinit var interpreter: FuzzyConstraintInterpreter

    @Before
    fun setup() {
        interpreter = FuzzyConstraintInterpreter()
    }

    @Test
    fun testDateConstraint() {
        val result = interpreter.interpret("Let's meet sometime next week")
        assertTrue(result.containsKey("dateRange"))
        assertEquals("Mon-Fri Next Week", result["dateRange"])
    }

    @Test
    fun testBudgetConstraint() {
        val result = interpreter.interpret("Find a hotel around 500 rupees")
        assertTrue(result.containsKey("budgetRange"))
        assertEquals("450-550", result["budgetRange"])
    }

    @Test
    fun testTimeConstraint() {
        val result = interpreter.interpret("Wake me up not too early")
        assertTrue(result.containsKey("timeRange"))
        assertEquals("after 9am", result["timeRange"])
    }
}
