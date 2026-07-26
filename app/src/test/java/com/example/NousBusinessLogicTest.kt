package com.example

import com.example.brain.BrainInterface
import com.example.brain.BrainResponse
import com.example.brain.ConversationTurn
import com.example.domain.model.Thought
import com.example.domain.model.ThoughtConnection
import com.example.domain.repository.NousRepository
import com.example.domain.usecase.AddConnectionUseCase
import com.example.domain.usecase.AskNousUseCase
import com.example.domain.usecase.DeleteThoughtUseCase
import com.example.domain.usecase.GetConnectionsUseCase
import com.example.domain.usecase.GetThoughtsUseCase
import com.example.domain.usecase.SaveThoughtUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FakeBrainFacade : BrainInterface {
    var responseText = "Cognitive insight generated successfully."
    
    override suspend fun processQuery(rawQuery: String, history: List<ConversationTurn>): BrainResponse {
        return BrainResponse(
            rawText = responseText,
            cleanTextForTts = responseText,
            detectedEmotion = "CALM",
            layerUsed = "Fake",
            latencyMs = 10L,
            cost = 0.0,
            tokensUsed = 10
        )
    }
}

class FakeNousRepository : NousRepository {
    private val thoughtsState = MutableStateFlow<List<Thought>>(emptyList())
    private val connectionsState = MutableStateFlow<List<ThoughtConnection>>(emptyList())
    
    var askNousResponse: String = "Cognitive insight generated successfully."

    override fun getThoughts(): Flow<List<Thought>> = thoughtsState

    override suspend fun getThoughtById(id: Long): Thought? {
        return thoughtsState.value.find { it.id == id }
    }

    override suspend fun saveThought(thought: Thought): Long {
        val list = thoughtsState.value.toMutableList()
        val id = if (thought.id == 0L) (list.size + 1).toLong() else thought.id
        val newThought = thought.copy(id = id)
        list.removeIf { it.id == id }
        list.add(newThought)
        thoughtsState.value = list
        return id
    }

    override suspend fun deleteThought(id: Long) {
        val list = thoughtsState.value.toMutableList()
        list.removeIf { it.id == id }
        thoughtsState.value = list
    }

    override fun getConnections(): Flow<List<ThoughtConnection>> = connectionsState

    override suspend fun saveConnection(connection: ThoughtConnection) {
        val list = connectionsState.value.toMutableList()
        val id = if (connection.id == 0L) (list.size + 1).toLong() else connection.id
        list.add(connection.copy(id = id))
        connectionsState.value = list
    }

    override suspend fun deleteConnection(id: Long) {
        val list = connectionsState.value.toMutableList()
        list.removeIf { it.id == id }
        connectionsState.value = list
    }

    override suspend fun deleteConnectionsForThought(thoughtId: Long) {
        val list = connectionsState.value.toMutableList()
        list.removeIf { it.sourceId == thoughtId || it.targetId == thoughtId }
        connectionsState.value = list
    }

    override suspend fun askNousForInsight(prompt: String, contextThoughts: List<Thought>): String {
        return askNousResponse
    }
}

class NousBusinessLogicTest {

    private lateinit var repository: FakeNousRepository
    private lateinit var brainFacade: FakeBrainFacade
    private lateinit var getThoughtsUseCase: GetThoughtsUseCase
    private lateinit var saveThoughtUseCase: SaveThoughtUseCase
    private lateinit var deleteThoughtUseCase: DeleteThoughtUseCase
    private lateinit var getConnectionsUseCase: GetConnectionsUseCase
    private lateinit var addConnectionUseCase: AddConnectionUseCase
    private lateinit var askNousUseCase: AskNousUseCase

    @Before
    fun setUp() {
        repository = FakeNousRepository()
        brainFacade = FakeBrainFacade()
        getThoughtsUseCase = GetThoughtsUseCase(repository)
        saveThoughtUseCase = SaveThoughtUseCase(repository)
        deleteThoughtUseCase = DeleteThoughtUseCase(repository)
        getConnectionsUseCase = GetConnectionsUseCase(repository)
        addConnectionUseCase = AddConnectionUseCase(repository)
        askNousUseCase = AskNousUseCase(brainFacade)
    }

    @Test
    fun save_thought_with_empty_title_throws_exception() = runBlocking {
        val invalidThought = Thought(title = "   ", content = "No title, should fail.")
        try {
            saveThoughtUseCase(invalidThought)
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Thought title cannot be empty", e.message)
        }
    }

    @Test
    fun save_valid_thought_saves_successfully() = runBlocking {
        val validThought = Thought(title = "Idea Node 1", content = "Connecting thoughts.")
        val generatedId = saveThoughtUseCase(validThought)
        
        val thoughtsList = getThoughtsUseCase().first()
        assertEquals(1, thoughtsList.size)
        assertEquals("Idea Node 1", thoughtsList[0].title)
        assertEquals(generatedId, thoughtsList[0].id)
    }

    @Test
    fun delete_thought_cleans_up_cascaded_connections() = runBlocking {
        // Save two thoughts
        val id1 = saveThoughtUseCase(Thought(title = "Node A", content = "Details A"))
        val id2 = saveThoughtUseCase(Thought(title = "Node B", content = "Details B"))
        
        // Connect A and B
        addConnectionUseCase(id1, id2)
        
        var connections = getConnectionsUseCase().first()
        assertEquals(1, connections.size)
        
        // Delete Node A
        deleteThoughtUseCase(id1)
        
        // Connections should be cleaned up!
        connections = getConnectionsUseCase().first()
        assertTrue(connections.isEmpty())
        
        // Node B should remain
        val thoughts = getThoughtsUseCase().first()
        assertEquals(1, thoughts.size)
        assertEquals(id2, thoughts[0].id)
    }

    @Test
    fun add_connection_to_self_throws_exception() = runBlocking {
        try {
            addConnectionUseCase(1L, 1L)
            fail("Connecting to self should fail")
        } catch (e: IllegalArgumentException) {
            assertEquals("A node cannot be connected to itself", e.message)
        }
    }

    @Test
    fun ask_nous_with_empty_prompt_throws_exception() = runBlocking {
        try {
            askNousUseCase("  ", emptyList())
            fail("Empty prompt should fail")
        } catch (e: IllegalArgumentException) {
            assertEquals("Prompt cannot be empty", e.message)
        }
    }

    @Test
    fun ask_nous_returns_insights() = runBlocking {
        val response = askNousUseCase("Map these thoughts", emptyList())
        assertTrue(response.startsWith("Cognitive insight generated successfully."))
    }
}
