package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.brain.download.ModelDownloader
import com.example.presentation.MainViewModel
import com.example.presentation.screens.SetupScreen
import com.example.presentation.screens.WorkspaceScreen
import com.example.ui.theme.NousTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var modelDownloader: ModelDownloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            var isSetupComplete by remember { mutableStateOf(false) }

            NousTheme(darkTheme = isDarkTheme) {
                if (!isSetupComplete && (!modelDownloader.isModelDownloaded("tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf") || !modelDownloader.isModelDownloaded("openwakeword_nous.tflite"))) {
                    SetupScreen(
                        downloader = modelDownloader,
                        onSetupComplete = { isSetupComplete = true }
                    )
                } else {
                    WorkspaceScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = it }
                    )
                }
            }
        }
    }
}
