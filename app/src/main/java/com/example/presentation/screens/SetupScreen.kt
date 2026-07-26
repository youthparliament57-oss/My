package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brain.download.ModelDownloader
import com.example.ui.theme.NousBlue

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    downloader: ModelDownloader,
    onSetupComplete: () -> Unit
) {
    val progressMap by downloader.downloadProgress.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Define the models required
    val llmModelName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
    val wakeWordModelName = "openwakeword_nous.tflite"

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: android.net.Uri? ->
            uri?.let {
                scope.launch {
                    val success = downloader.importModelFromUri(it, wakeWordModelName)
                    if (success) {
                        snackbarHostState.showSnackbar("✅ Model Uploaded Successfully!")
                    } else {
                        snackbarHostState.showSnackbar("❌ Upload Failed! Check file.")
                    }
                }
            }
        }
    )
    
    val llmUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
    val wakeWordUrl = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/lite-model_yamnet_classification_tflite_1.tflite" // YAMNet Audio model as placeholder for Wake Word
    
    val isLlmDownloaded = downloader.isModelDownloaded(llmModelName) || progressMap[llmModelName]?.isComplete == true
    val isWakeWordDownloaded = downloader.isModelDownloaded(wakeWordModelName) || progressMap[wakeWordModelName]?.isComplete == true

    if (isLlmDownloaded && isWakeWordDownloaded) {
        LaunchedEffect(Unit) {
            onSetupComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = "NOUS Initial Setup",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Downloading core neural models to device storage for offline execution.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // LLM Download UI
        DownloadItem(
            title = "Local LLM Engine (TinyLlama Q4)",
            modelName = llmModelName,
            url = llmUrl,
            downloader = downloader,
            state = progressMap[llmModelName],
            isCompleteInitially = isLlmDownloaded
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Wake Word Download UI
        DownloadItem(
            title = "Wake Word TFLite Model",
            modelName = wakeWordModelName,
            url = wakeWordUrl,
            downloader = downloader,
            state = progressMap[wakeWordModelName],
            isCompleteInitially = isWakeWordDownloaded
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Import ka section (Sirf tab dikhao jab model download nahi hua ho)
        if (!isWakeWordDownloaded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔧 Dev Mode: Upload Trained Model",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // TFLite files ke liye MIME type set karo
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE)
                        )
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select .tflite from Storage")
                    }
                    Text(
                        text = "Browse and select your trained 'openwakeword_nous.tflite' file",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Skip for now button just for dev testing if they don't want to wait
        TextButton(onClick = { onSetupComplete() }) {
            Text("Skip Download (Dev Mode)", color = Color.Gray)
        }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun DownloadItem(
    title: String,
    modelName: String,
    url: String,
    downloader: ModelDownloader,
    state: ModelDownloader.DownloadState?,
    isCompleteInitially: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCompleteInitially || state?.isComplete == true) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                contentDescription = null,
                tint = if (isCompleteInitially || state?.isComplete == true) Color.Green else NousBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                if (isCompleteInitially || state?.isComplete == true) {
                    Text("Downloaded & Ready", fontSize = 12.sp, color = Color.Green)
                } else {
                    val progress = state?.progress ?: 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = NousBlue,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (!isCompleteInitially && state?.isComplete != true && state?.isDownloading != true) {
                Button(onClick = { downloader.startDownload(modelName, url) }) {
                    Text(if (state?.hasFailed == true) "Retry" else "Start")
                }
            }
        }
    }
}
