package com.example.presentation.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle
import com.example.brain.voice.VoiceOrchestrator
import com.example.presentation.components.MemoryMapView
import com.example.presentation.components.MemoryTimelineView
import com.example.domain.model.Thought
import com.example.presentation.MainViewModel
import com.example.presentation.components.NodeCanvas
import com.example.presentation.components.NousLogoHeader
import com.example.presentation.components.CameraPreview
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.ui.theme.NousBlack
import com.example.ui.theme.NousBlue
import com.example.ui.theme.NousBlueGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAiConsole by remember { mutableStateOf(false) }
    var showMemoryExplorer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showReasoning by remember { mutableStateOf(false) }
    var showAgent by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var explorerTab by remember { mutableIntStateOf(0) } // 0: Timeline, 1: Map
    val voiceState by viewModel.voiceState.collectAsState()
    val visionActive by viewModel.visionActive.collectAsState()
    val lastVisionResult by viewModel.lastVisionResult.collectAsState()


    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            // Elegant Brand Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Programmatic brand representation in row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    NousLogoHeader(
                        showText = false,
                        iconSize = 32
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NOUS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        val agentStatus by viewModel.agentStatus.collectAsState()
                        Text(
                            text = agentStatus,
                            fontSize = 8.sp,
                            color = Color.Green.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Mode Mic Button
                    IconButton(
                        onClick = { 
                            if (voiceState == VoiceOrchestrator.VoiceState.IDLE) {
                                viewModel.startVoiceMode()
                            } else {
                                viewModel.stopVoiceMode()
                            }
                        },
                        modifier = Modifier.testTag("voice_mic_button")
                    ) {
                        val isActive = voiceState != VoiceOrchestrator.VoiceState.IDLE
                        Icon(
                            imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Mode",
                            tint = when (voiceState) {
                                VoiceOrchestrator.VoiceState.LISTENING -> Color.Red
                                VoiceOrchestrator.VoiceState.THINKING -> NousBlue
                                VoiceOrchestrator.VoiceState.SPEAKING -> Color.Green
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            }
                        )
                    }

                    // Space-Time Explorer Toggle

                    IconButton(
                        onClick = { showMemoryExplorer = !showMemoryExplorer },
                        modifier = Modifier.testTag("explorer_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = "Memory Explorer",
                            tint = if (showMemoryExplorer) NousBlue else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Quick Action: AI Console
                    IconButton(
                        onClick = { showAiConsole = !showAiConsole },
                        modifier = Modifier.testTag("ai_console_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask Nous",
                            tint = NousBlue
                        )
                    }

                    // Vision Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleVisionMode() },
                        modifier = Modifier.testTag("vision_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (visionActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Vision Mode",
                            tint = if (visionActive) Color.Cyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = { showAgent = !showAgent },
                        modifier = Modifier.testTag("agent_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Agent Controls",
                            tint = if (showAgent) NousBlue else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Theme Toggle (Light/Dark Support)
                    IconButton(
                        onClick = { onThemeToggle(!isDarkTheme) },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.linkModeActive) {
                    // Link Mode active cancellation indicator
                    FloatingActionButton(
                        onClick = { viewModel.cancelLinking() },
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("cancel_link_fab")
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = "Cancel linking")
                    }
                }

                // Add New thought node
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_thought_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add node")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Mind Canvas
            NodeCanvas(
                thoughts = uiState.thoughts,
                connections = uiState.connections,
                selectedThought = uiState.selectedThought,
                linkModeActive = uiState.linkModeActive,
                linkSourceId = uiState.linkSourceId,
                onThoughtSelected = { viewModel.selectThought(it) },
                onThoughtMoved = { thought, x, y -> viewModel.updateThoughtPosition(thought, x, y) },
                onNodeClicked = { clickedThought ->
                    if (uiState.linkModeActive) {
                        viewModel.completeLinking(clickedThought.id)
                    } else {
                        viewModel.selectThought(clickedThought)
                    }
                }
            )

            // Voice Status Overlay
            if (voiceState != VoiceOrchestrator.VoiceState.IDLE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                        .padding(8.dp)
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceState == VoiceOrchestrator.VoiceState.LISTENING) Color.Red else NousBlue
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (voiceState) {
                                VoiceOrchestrator.VoiceState.LISTENING -> "NOUS is listening..."
                                VoiceOrchestrator.VoiceState.THINKING -> "NOUS is reasoning..."
                                VoiceOrchestrator.VoiceState.SPEAKING -> "NOUS is speaking..."
                                else -> ""
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Vision Preview & Overlay
            if (visionActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onFrameAnalyzed = { viewModel.onVisionFrame(it) }
                    )
                    
                    // Vision Intelligence Overlay
                    lastVisionResult?.let { result ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (result.objects.isNotEmpty()) {
                                Text("Objects:", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                result.objects.take(3).forEach {
                                    Text("${it.label} (${(it.confidence * 100).toInt()}%)", color = Color.White, fontSize = 9.sp)
                                }
                            }
                            if (!result.ocrText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Text Detected", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (result.faces.count > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Faces: ${result.faces.count}", color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (result.barcodes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("QR/Barcode!", color = Color.Magenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Exit Vision Button
                    IconButton(
                        onClick = { viewModel.toggleVisionMode() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Vision", tint = Color.White)
                    }
                }
            }


            // Canvas Floating Banner for Connection Mode
            if (uiState.linkModeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = "Connecting nodes... Tap another node to complete the link",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom drawer detail card when a thought is selected
            AnimatedVisibility(
                visible = uiState.selectedThought != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.selectedThought?.let { selected ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                RoundedCornerShape(16.dp)
                            )
                            .testTag("thought_detail_drawer"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selected.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                                        .format(Date(selected.timestamp))
                                    Text(
                                        text = "Created: $dateStr",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }

                                IconButton(onClick = { viewModel.selectThought(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close details")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = selected.content,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Details Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Connect Action
                                    Button(
                                        onClick = { viewModel.startLinking(selected.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NousBlue),
                                        modifier = Modifier.testTag("link_nodes_button")
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = "Link")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Link", fontSize = 12.sp)
                                    }

                                    // Ask Nous specific query about this node
                                    Button(
                                        onClick = {
                                            showAiConsole = true
                                            aiPrompt = "Expand on this idea, suggest relations, and provide insights: \"${selected.title}\" -> ${selected.content}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier.testTag("node_expand_ai_button")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Analyze", fontSize = 12.sp)
                                    }
                                }

                                // Delete node Action
                                IconButton(
                                    onClick = { viewModel.deleteThought(selected.id) },
                                    modifier = Modifier.testTag("delete_node_button")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Node",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Slide-out Side AI Companion Console (Dialogue/Panel)
            AnimatedVisibility(
                visible = showAiConsole,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("ai_console_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = NousBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (showSettings) "LLM Stack Settings" else if (showReasoning) "Cognitive Reasoning Traces" else "NOUS Cognitive Companion",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!showSettings) {
                                    IconButton(onClick = { showReasoning = !showReasoning }) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Toggle Reasoning",
                                            tint = if (showReasoning) NousBlue else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                IconButton(onClick = { showSettings = !showSettings }) {
                                    Icon(
                                        imageVector = if (showSettings) Icons.Default.AutoAwesome else Icons.Default.Settings,
                                        contentDescription = "Toggle Settings",
                                        tint = if (showSettings) NousBlue else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                IconButton(onClick = { showAiConsole = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close AI console")
                                }
                            }
                        }

                        if (showSettings) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    "Active LLM Core Provider",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NousBlue,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                val providers = listOf("gemini", "openai", "anthropic", "groq", "openrouter")
                                val activeProvider by viewModel.activeLlmProvider.collectAsState()
                                val configuredProviders by viewModel.configuredProviders.collectAsState()

                                providers.forEach { provider ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.updateActiveLlmProvider(provider) }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.RadioButton(
                                                selected = activeProvider == provider,
                                                onClick = { viewModel.updateActiveLlmProvider(provider) },
                                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = NousBlue)
                                            )
                                            Text(provider.replaceFirstChar { it.uppercase() }, fontSize = 14.sp)
                                        }
                                        if (configuredProviders[provider] == true) {
                                            Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Configured", tint = Color.Green, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Not Set", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "API Key Configuration",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NousBlue,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                providers.forEach { provider ->
                                    var keyInput by remember { mutableStateOf("") }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = keyInput,
                                            onValueChange = { keyInput = it },
                                            label = { Text("$provider Key", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { 
                                            if (keyInput.isNotBlank()) {
                                                viewModel.storeApiKey(provider, keyInput)
                                                keyInput = ""
                                            }
                                        }) {
                                            Icon(androidx.compose.material.icons.Icons.Default.Save, contentDescription = "Save")
                                        }
                                        IconButton(onClick = { viewModel.deleteApiKey(provider) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        } else if (showReasoning) {
                            // Reasoning Traces View
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (uiState.cognitiveTraces.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No reasoning traces generated yet. Ask NOUS a complex question (e.g., 'Compare my thoughts' or 'Evaluate options') to trigger the cognitive stack.",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    uiState.cognitiveTraces.reversed().forEach { trace ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                            border = BorderStroke(1.dp, NousBlue.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Cognitive Trace", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NousBlue)
                                                    Text(
                                                        "Confidence: ${(trace.confidenceScore * 100).toInt()}%",
                                                        fontSize = 10.sp,
                                                        color = if (trace.confidenceScore > 0.8f) Color.Green else if (trace.confidenceScore > 0.5f) Color.Yellow else Color.Red
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Query: ${trace.query}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                
                                                if (trace.subTasks.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Task Decomposition:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                    trace.subTasks.forEach { subTask ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                                        ) {
                                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(NousBlue))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(subTask, fontSize = 10.sp)
                                                        }
                                                    }
                                                }

                                                if (trace.steps.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text("Reasoning Steps (CoT):", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                    trace.steps.forEachIndexed { index, step ->
                                                        Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                                                            Text("Step ${index + 1}: ${step.stepText}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                            Text("Evidence: ${step.evidence}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                                            Text("Conclusion: ${step.conclusion}", fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text("Final Synthesis:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                                Text(trace.finalAnswer ?: "No answer generated.", fontSize = 12.sp)

                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (trace.userFeedback != null) {
                                                        Text(
                                                            "Feedback: ${trace.userFeedback}",
                                                            fontSize = 10.sp,
                                                            color = NousBlue,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.annotateTraceFeedback(trace.query, "Helpful") },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            androidx.compose.material.icons.Icons.Default.ThumbUp,
                                                            contentDescription = "Helpful",
                                                            tint = if (trace.userFeedback == "Helpful") NousBlue else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = { viewModel.annotateTraceFeedback(trace.query, "Not Helpful") },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            androidx.compose.material.icons.Icons.Default.ThumbDown,
                                                            contentDescription = "Not Helpful",
                                                            tint = if (trace.userFeedback == "Not Helpful") Color.Red.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (showAgent) {
                            // Agent View (Module 9)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Agent Stack Control",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NousBlue
                                    )
                                    IconButton(onClick = { showAgent = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                                
                                val agentStatus by viewModel.agentStatus.collectAsState()
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, NousBlue.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("System Probes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NousBlue)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(agentStatus, fontSize = 12.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("UI Automation (Hands)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        val isAccEnabled = com.example.agent.automation.NousAccessibilityService.registry.isServiceEnabled()
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Accessibility Service: ", fontSize = 11.sp)
                                            Text(
                                                if (isAccEnabled) "ACTIVE" else "INACTIVE",
                                                color = if (isAccEnabled) Color.Green else Color.Red,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { /* Open Settings would go here */ },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isAccEnabled) Color.Gray else NousBlue)
                                        ) {
                                            Text(if (isAccEnabled) "Service Configured" else "Enable Accessibility", fontSize = 11.sp)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Banking Guard (Safety)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red.copy(alpha = 0.7f))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Protected Financial Apps:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        viewModel.agentFacade.guard.getBlacklist().take(8).forEach { pkg ->
                                            Text("• ${pkg.substringAfterLast(".")}", fontSize = 10.sp)
                                        }
                                        if (viewModel.agentFacade.guard.getBlacklist().size > 8) {
                                            Text("...and ${viewModel.agentFacade.guard.getBlacklist().size - 8} more", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Response field
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                ) {
                                    if (uiState.isAskingNous) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = NousBlue)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "Synthesizing thoughts...",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    } else {
                                        val responseText = uiState.nousInsight
                                        if (responseText != null) {
                                            Text(
                                                text = responseText,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                lineHeight = 18.sp
                                            )
                                        } else {
                                            Text(
                                                text = "Hello! I am NOUS, your cognitive partner. Type a prompt or select a node and click 'Analyze' to map out insights from your mental workspace.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Prompt input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = aiPrompt,
                                    onValueChange = { aiPrompt = it },
                                    placeholder = { Text("Ask NOUS to link or analyze your thoughts...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_prompt_input"),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NousBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (aiPrompt.isNotBlank()) {
                                            viewModel.askNous(aiPrompt)
                                            aiPrompt = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(NousBlue)
                                        .testTag("send_ai_prompt_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Space-Time Memory Explorer Panel
    AnimatedVisibility(
        visible = showMemoryExplorer,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Dialog(onDismissRequest = { showMemoryExplorer = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                val episodicEvents by viewModel.episodicEvents.collectAsState()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Explorer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = NousBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Space-Time Memory Explorer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { showMemoryExplorer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .padding(4.dp)
                    ) {
                        val tabs = listOf("Temporal" to Icons.Default.History, "Spatial" to Icons.Default.Map)
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = explorerTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) NousBlue else Color.Transparent)
                                    .clickable { explorerTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        tab.second,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        tab.first,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }

                    // Content
                    Box(modifier = Modifier.weight(1f)) {
                        if (explorerTab == 0) {
                            MemoryTimelineView(events = episodicEvents)
                        } else {
                            MemoryMapView(events = episodicEvents)
                        }
                    }
                }
            }
        }
    }

    // Add Thought Node Dialog Modal
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var importance by remember { mutableIntStateOf(3) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("add_thought_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Anchor Concept Node",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Concept Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("thought_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NousBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Details or Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("thought_content_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NousBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Concept Weight (Node Size): $importance",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Slider(
                        value = importance.toFloat(),
                        onValueChange = { importance = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.testTag("thought_importance_slider")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    // Generate a random initial layout coordinates centered with a bit of noise
                                    val rx = (0.35f + Math.random().toFloat() * 0.3f).coerceIn(0.1f, 0.9f)
                                    val ry = (0.35f + Math.random().toFloat() * 0.3f).coerceIn(0.1f, 0.9f)
                                    viewModel.addThought(title, content, importance, rx, ry)
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NousBlue),
                            modifier = Modifier.testTag("submit_thought_button")
                        ) {
                            Text("Form Node", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
