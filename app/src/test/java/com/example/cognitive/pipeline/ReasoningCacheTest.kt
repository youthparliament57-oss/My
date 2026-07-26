package com.example.cognitive.pipeline

import com.example.cognitive.models.ReasoningTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ReasoningCacheTest {

    private lateinit var cache: ReasoningCache

    @Before
    fun setup() {
        cache = ReasoningCache()
    }

    @Test
    fun testCachePutAndGet() {
        val key = cache.generateKey("test query", "user1", "ATLAS")
        val trace = ReasoningTrace(query = "test query", finalAnswer = "Test answer")
        
        cache.put(key, trace)
        
        val retrieved = cache.get(key)
        assertNotNull(retrieved)
        assertEquals("Test answer", retrieved?.finalAnswer)
    }

    @Test
    fun testCacheMiss() {
        val retrieved = cache.get("nonexistent")
        assertNull(retrieved)
    }
}
