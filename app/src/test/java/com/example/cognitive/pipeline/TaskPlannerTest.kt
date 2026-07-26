package com.example.cognitive.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskPlannerTest {

    private lateinit var planner: TaskPlanner
    private lateinit var interpreter: FuzzyConstraintInterpreter

    @Before
    fun setup() {
        interpreter = FuzzyConstraintInterpreter()
        planner = TaskPlanner(interpreter)
    }

    @Test
    fun testDecomposeComplexTask() {
        val tasks = planner.decompose("Plan a trip to Goa next weekend")
        assertEquals(5, tasks.size)
        assertTrue(tasks[0].contains("calendar"))
    }

    @Test
    fun testNoDecomposition() {
        val tasks = planner.decompose("What time is it?")
        assertEquals(1, tasks.size)
        assertEquals("What time is it?", tasks[0])
    }
}
