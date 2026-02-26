package com.runanywhere.startup_hackathon20.ui_screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.runanywhere.startup_hackathon20.viewmodel.VoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    navController: NavHostController,
    voiceViewModel: VoiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val voiceState by voiceViewModel.voiceState.collectAsState()
    val modelState by voiceViewModel.modelState.collectAsState()
    val availableModels by voiceViewModel.availableModels.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("STT", "TTS", "Voice Agent", "Models")

    // Permission handling
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Assistant") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Card
            StatusCard(modelState, voiceState)

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> STTTab(voiceViewModel, voiceState, hasAudioPermission)
                1 -> TTSTab(voiceViewModel, voiceState)
                2 -> VoiceAgentTab(voiceViewModel, voiceState, modelState, hasAudioPermission)
                3 -> ModelsTab(voiceViewModel, availableModels, modelState)
            }
        }
    }
}

@Composable
fun StatusCard(modelState: com.runanywhere.startup_hackathon20.viewmodel.ModelLoadingState, voiceState: com.runanywhere.startup_hackathon20.viewmodel.VoiceState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip("LLM", modelState.isLLMLoaded)
                StatusChip("STT", modelState.isSTTLoaded)
                StatusChip("TTS", modelState.isTTSLoaded)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = voiceState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun StatusChip(label: String, isLoaded: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isLoaded) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isLoaded) Color.White else Color.Gray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isLoaded) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun STTTab(
    viewModel: VoiceViewModel,
    voiceState: com.runanywhere.startup_hackathon20.viewmodel.VoiceState,
    hasPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Speech-to-Text",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Audio level visualization
        if (voiceState.isRecording) {
            AudioLevelIndicator(voiceState.audioLevel)
        }

        // Transcribed text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Transcription:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = voiceState.transcribedText.ifEmpty { "Start recording to see transcription..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (voiceState.transcribedText.isEmpty()) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (voiceState.confidence > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Confidence: ${(voiceState.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Recording button
        if (hasPermission) {
            if (voiceState.isRecording) {
                Button(
                    onClick = { viewModel.stopRecordingAndTranscribe() },
                    modifier = Modifier
                        .size(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier.size(80.dp),
                    enabled = !voiceState.isTranscribing,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Record",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (voiceState.isTranscribing) {
                CircularProgressIndicator()
            }

            if (voiceState.transcribedText.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.clearTranscription() }
                ) {
                    Icon(Icons.Default.Clear, "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
            }
        } else {
            Text(
                text = "Microphone permission required",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TTSTab(
    viewModel: VoiceViewModel,
    voiceState: com.runanywhere.startup_hackathon20.viewmodel.VoiceState
) {
    var textToSpeak by remember { mutableStateOf("Hello! This is a test of the text-to-speech system.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Text-to-Speech",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = textToSpeak,
            onValueChange = { textToSpeak = it },
            label = { Text("Text to speak") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 8,
            enabled = !voiceState.isSpeaking
        )

        if (voiceState.isSpeaking) {
            SpeakingAnimation()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.speakText(textToSpeak) },
                modifier = Modifier.weight(1f),
                enabled = !voiceState.isSpeaking && textToSpeak.isNotBlank()
            ) {
                Icon(Icons.Default.VolumeUp, "Speak")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Speak")
            }

            if (voiceState.isSpeaking) {
                OutlinedButton(
                    onClick = { viewModel.stopSpeaking() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, "Stop")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        }

        // Quick test phrases
        Text(
            text = "Quick Test Phrases:",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start)
        )

        val testPhrases = listOf(
            "Hello, how are you today?",
            "The quick brown fox jumps over the lazy dog.",
            "Testing text-to-speech functionality.",
            "Welcome to the voice assistant demo."
        )

        testPhrases.forEach { phrase ->
            OutlinedButton(
                onClick = { textToSpeak = phrase },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(phrase, maxLines = 1)
            }
        }
    }
}

@Composable
fun VoiceAgentTab(
    viewModel: VoiceViewModel,
    voiceState: com.runanywhere.startup_hackathon20.viewmodel.VoiceState,
    modelState: com.runanywhere.startup_hackathon20.viewmodel.ModelLoadingState,
    hasPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Full Voice Agent",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Complete pipeline: VAD → STT → LLM → TTS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // Pipeline status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pipeline Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                PipelineStep("VAD", "Voice Detection", true)
                PipelineStep("STT", "Speech Recognition", modelState.isSTTLoaded)
                PipelineStep("LLM", "AI Processing", modelState.isLLMLoaded)
                PipelineStep("TTS", "Speech Synthesis", modelState.isTTSLoaded)
            }
        }

        // Conversation display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Conversation:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (voiceState.transcribedText.isNotEmpty()) {
                    Text(
                        text = "You: ${voiceState.transcribedText}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                if (voiceState.responseText.isNotEmpty()) {
                    Text(
                        text = "AI: ${voiceState.responseText}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (voiceState.transcribedText.isEmpty() && voiceState.responseText.isEmpty()) {
                    Text(
                        text = "Start the voice agent to begin conversation...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        val allModelsLoaded = modelState.isLLMLoaded && 
                             modelState.isSTTLoaded && 
                             modelState.isTTSLoaded

        if (!allModelsLoaded) {
            Text(
                text = "Please load all models (LLM, STT, TTS) in the Models tab first",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Control buttons
        if (hasPermission) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.startVoiceAgent() },
                    modifier = Modifier.weight(1f),
                    enabled = allModelsLoaded && !voiceState.isProcessing
                ) {
                    Icon(Icons.Default.PlayArrow, "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Agent")
                }

                OutlinedButton(
                    onClick = { viewModel.stopVoiceAgent() },
                    modifier = Modifier.weight(1f),
                    enabled = voiceState.isProcessing
                ) {
                    Icon(Icons.Default.Stop, "Stop")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        } else {
            Text(
                text = "Microphone permission required",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ModelsTab(
    viewModel: VoiceViewModel,
    availableModels: List<com.runanywhere.sdk.models.ModelInfo>,
    modelState: com.runanywhere.startup_hackathon20.viewmodel.ModelLoadingState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Model Management",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (modelState.downloadProgress != null) {
            LinearProgressIndicator(
                progress = { modelState.downloadProgress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Downloading: ${(modelState.downloadProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Button(
            onClick = { viewModel.refreshModels() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, "Refresh")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Models")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableModels) { model ->
                ModelCard(
                    model = model,
                    viewModel = viewModel,
                    modelState = modelState
                )
            }
        }
    }
}

@Composable
fun ModelCard(
    model: com.runanywhere.sdk.models.ModelInfo,
    viewModel: VoiceViewModel,
    modelState: com.runanywhere.startup_hackathon20.viewmodel.ModelLoadingState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Model: ${model.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (model.isDownloaded) {
                    Button(
                        onClick = {
                            // Placeholder: Load model functionality
                        },
                        modifier = Modifier.weight(1f),
                        enabled = true
                    ) {
                        Icon(
                            Icons.Default.Check,
                            "Load"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Loaded")
                    }
                } else {
                    Button(
                        onClick = { /* Placeholder: Download functionality */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, "Download")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

@Composable
fun PipelineStep(icon: String, label: String, isReady: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (icon) {
                "VAD" -> Icons.Default.Mic
                "STT" -> Icons.Default.Mic
                "LLM" -> Icons.Default.Psychology
                "TTS" -> Icons.Default.VolumeUp
                else -> Icons.Default.Circle
            },
            contentDescription = icon,
            tint = if (isReady) Color(0xFF4CAF50) else Color.Gray
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isReady) MaterialTheme.colorScheme.onTertiaryContainer
                   else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isReady) {
            Icon(
                Icons.Default.CheckCircle,
                "Ready",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AudioLevelIndicator(level: Float) {
    val scale = 1f + (level * 0.5f)
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE53935).copy(alpha = 0.8f),
                        Color(0xFFE53935).copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Mic,
            "Recording",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun SpeakingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.VolumeUp,
            "Speaking",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}
