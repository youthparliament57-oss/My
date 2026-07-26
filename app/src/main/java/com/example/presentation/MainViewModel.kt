package com.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain.LlmPreloadManager
import com.example.brain.voice.VoiceOrchestrator
import com.example.brain.voice.ProactiveManager
import com.example.brain.memory.EpisodicEventEntity
import com.example.brain.memory.MemoryDao
import com.example.brain.memory.MemoryEncryption
import com.example.brain.memory.NativeCrashLogEntity
import com.example.brain.memory.UserPreferenceEntity
import com.example.brain.security.CredentialVault
import com.example.domain.model.Thought
import com.example.domain.model.ThoughtConnection
import com.example.domain.usecase.AddConnectionUseCase
import com.example.domain.usecase.AskNousUseCase
import com.example.domain.usecase.DeleteThoughtUseCase
import com.example.domain.usecase.GetConnectionsUseCase
import com.example.domain.usecase.GetThoughtsUseCase
import com.example.domain.usecase.SaveThoughtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceUiState(
    val thoughts: List<Thought> = emptyList(),
    val connections: List<ThoughtConnection> = emptyList(),
    val selectedThought: Thought? = null,
    val isLoading: Boolean = false,
    val nousInsight: String? = null,
    val isAskingNous: Boolean = false,
    val linkModeActive: Boolean = false,
    val linkSourceId: Long? = null,
    val cognitiveTraces: List<com.example.cognitive.models.ReasoningTrace> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getThoughtsUseCase: GetThoughtsUseCase,
    private val saveThoughtUseCase: SaveThoughtUseCase,
    private val deleteThoughtUseCase: DeleteThoughtUseCase,
    private val getConnectionsUseCase: GetConnectionsUseCase,
    private val addConnectionUseCase: AddConnectionUseCase,
    private val askNousUseCase: AskNousUseCase,
    private val credentialVault: CredentialVault,
    private val memoryDao: MemoryDao,
    private val preloadManager: LlmPreloadManager,
    private val voiceOrchestrator: VoiceOrchestrator,
    private val proactiveManager: ProactiveManager,
    private val visionOrchestrator: com.example.vision.VisionOrchestrator,
    private val traceStore: com.example.cognitive.pipeline.ReasoningTraceStore,
    val agentFacade: com.example.agent.AgentFacade
) : ViewModel() {

    val voiceState = voiceOrchestrator.voiceState
    val visionActive = visionOrchestrator.isVisionActive
    val lastVisionResult = visionOrchestrator.lastSceneResult

    private val _agentStatus = MutableStateFlow("Agent Ready")
    val agentStatus: StateFlow<String> = _agentStatus

    init {
        viewModelScope.launch {
            while(true) {
                _agentStatus.value = agentFacade.getStatusSummary()
                kotlinx.coroutines.delay(5000)
            }
        }
    }


    private val _selectedThought = MutableStateFlow<Thought?>(null)
    private val _nousInsight = MutableStateFlow<String?>(null)
    private val _isAskingNous = MutableStateFlow(false)
    private val _linkModeActive = MutableStateFlow(false)
    private val _linkSourceId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<WorkspaceUiState> = combine(
        getThoughtsUseCase(),
        getConnectionsUseCase(),
        _selectedThought,
        _nousInsight,
        _isAskingNous,
        _linkModeActive,
        _linkSourceId,
        traceStore.traces
    ) { args: Array<Any?> ->
        WorkspaceUiState(
            thoughts = args[0] as List<com.example.domain.model.Thought>,
            connections = args[1] as List<com.example.domain.model.ThoughtConnection>,
            selectedThought = args[2] as com.example.domain.model.Thought?,
            nousInsight = args[3] as String?,
            isAskingNous = args[4] as Boolean,
            linkModeActive = args[5] as Boolean,
            linkSourceId = args[6] as Long?,
            cognitiveTraces = args[7] as List<com.example.cognitive.models.ReasoningTrace>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkspaceUiState()
    )

    // Crash logs list stream from Room
    val crashLogs: StateFlow<List<NativeCrashLogEntity>> = memoryDao.getAllNativeCrashLogsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Episodic events stream for Space-Time Explorer
    val episodicEvents: StateFlow<List<EpisodicEventEntity>> = memoryDao.getAllEpisodicEventsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI configuration states
    private val _configuredProviders = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val configuredProviders: StateFlow<Map<String, Boolean>> = _configuredProviders.asStateFlow()

    private val _activeLlmProvider = MutableStateFlow("gemini")
    val activeLlmProvider: StateFlow<String> = _activeLlmProvider.asStateFlow()

    init {
        loadActiveLlmProvider()
        refreshConfiguredProviders()
        proactiveManager.startMonitoring()
    }

    fun startVoiceMode() {
        voiceOrchestrator.startVoiceMode()
    }

    fun stopVoiceMode() {
        voiceOrchestrator.stopEverything()
    }

    fun toggleVisionMode() {
        if (visionOrchestrator.isVisionActive.value) {
            visionOrchestrator.stopVision()
        } else {
            visionOrchestrator.startVision()
        }
    }

    fun onVisionFrame(bitmap: android.graphics.Bitmap) {
        visionOrchestrator.processFrame(bitmap)
    }


    // --- Active LLM Provider Management ---
    private fun loadActiveLlmProvider() {
        viewModelScope.launch {
            val pref = memoryDao.getUserPreference("active_llm_provider")
            if (pref != null) {
                _activeLlmProvider.value = try {
                    MemoryEncryption.decrypt(pref.encryptedValue)
                } catch (e: Exception) {
                    "gemini"
                }
            } else {
                _activeLlmProvider.value = "gemini"
            }
        }
    }

    fun updateActiveLlmProvider(provider: String) {
        viewModelScope.launch {
            val encrypted = MemoryEncryption.encrypt(provider)
            memoryDao.insertUserPreference(
                UserPreferenceEntity(
                    key = "active_llm_provider",
                    encryptedValue = encrypted,
                    timestamp = System.currentTimeMillis()
                )
            )
            _activeLlmProvider.value = provider
        }
    }

    // --- API Key Management ---
    fun refreshConfiguredProviders() {
        viewModelScope.launch {
            val map = mutableMapOf<String, Boolean>()
            val providers = listOf("gemini", "openai", "anthropic", "groq", "openrouter")
            for (p in providers) {
                val key = credentialVault.getApiKey(p)
                map[p] = !key.isNullOrEmpty()
            }
            _configuredProviders.value = map
        }
    }

    fun storeApiKey(provider: String, apiKey: String) {
        viewModelScope.launch {
            credentialVault.storeApiKey(provider, apiKey)
            refreshConfiguredProviders()
        }
    }

    fun deleteApiKey(provider: String) {
        viewModelScope.launch {
            credentialVault.deleteApiKey(provider)
            refreshConfiguredProviders()
        }
    }

    // --- Predictive Preloading Triggers ---
    fun triggerLocalLlmPreload() {
        preloadManager.preloadModelOnIntent()
    }

    // --- Core Workspace Node Manipulations ---
    fun selectThought(thought: Thought?) {
        _selectedThought.value = thought
    }

    fun addThought(title: String, content: String, importance: Int, x: Float, y: Float) {
        viewModelScope.launch {
            val newThought = Thought(
                title = title,
                content = content,
                importance = importance,
                xPosition = x,
                yPosition = y
            )
            saveThoughtUseCase(newThought)
        }
    }

    fun updateThoughtPosition(thought: Thought, x: Float, y: Float) {
        viewModelScope.launch {
            val updated = thought.copy(xPosition = x, yPosition = y)
            saveThoughtUseCase(updated)
            if (_selectedThought.value?.id == thought.id) {
                _selectedThought.value = updated
            }
        }
    }

    fun deleteThought(id: Long) {
        viewModelScope.launch {
            deleteThoughtUseCase(id)
            if (_selectedThought.value?.id == id) {
                _selectedThought.value = null
            }
        }
    }

    fun startLinking(sourceId: Long) {
        _linkModeActive.value = true
        _linkSourceId.value = sourceId
    }

    fun completeLinking(targetId: Long) {
        val sourceId = _linkSourceId.value
        if (sourceId != null && sourceId != targetId) {
            viewModelScope.launch {
                try {
                    addConnectionUseCase(sourceId, targetId)
                } catch (e: Exception) {
                    // Connection error
                } finally {
                    cancelLinking()
                }
            }
        } else {
            cancelLinking()
        }
    }

    fun cancelLinking() {
        _linkModeActive.value = false
        _linkSourceId.value = null
    }

    fun askNous(prompt: String) {
        viewModelScope.launch {
            _isAskingNous.value = true
            _nousInsight.value = null
            try {
                val thoughts = uiState.value.thoughts
                val result = askNousUseCase(prompt, thoughts)
                _nousInsight.value = result
            } catch (e: Exception) {
                _nousInsight.value = "Failed to synchronize with NOUS: ${e.message}"
            } finally {
                _isAskingNous.value = false
            }
        }
    }

    fun clearInsight() {
        _nousInsight.value = null
    }

    fun annotateTraceFeedback(query: String, feedback: String) {
        viewModelScope.launch {
            traceStore.annotateFeedback(query, feedback)
        }
    }
}
