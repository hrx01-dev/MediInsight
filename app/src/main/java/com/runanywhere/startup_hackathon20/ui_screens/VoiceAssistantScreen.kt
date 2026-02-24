package com.runanywhere.startup_hackathon20.ui_screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.runanywhere.startup_hackathon20.ChatMessage
import com.runanywhere.startup_hackathon20.ChatViewModel
import com.runanywhere.startup_hackathon20.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Voice Assistant Screen
 * Combines AI chat with voice input/output capabilities
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    onBack: () -> Unit,
    chatViewModel: ChatViewModel? = viewModel()
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceManager(context) }
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    var userInput by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }
    var voiceMode by remember { mutableStateOf(false) } // Voice input/output enabled
    
    // ViewModel states
    val messages by (chatViewModel?.messages ?: remember { MutableStateFlow(emptyList()) }).collectAsState()
    val isLoading by (chatViewModel?.isLoading ?: remember { MutableStateFlow(false) }).collectAsState()
    val currentModelId by (chatViewModel?.currentModelId ?: remember { MutableStateFlow<String?>(null) }).collectAsState()
    val statusMessage by (chatViewModel?.statusMessage ?: remember { MutableStateFlow("Initializing...") }).collectAsState()
    val isModelVerified by (chatViewModel?.isModelVerified ?: remember { MutableStateFlow(false) }).collectAsState()
    
    // Voice states
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val sttError by voiceManager.sttError.collectAsState()
    val voiceActivityDetected by voiceManager.voiceActivityDetected.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Handle recognized text
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            userInput = recognizedText
            voiceManager.clearRecognizedText()
        }
    }
    
    // Auto-speak AI responses in voice mode
    LaunchedEffect(messages.size, voiceMode) {
        if (voiceMode && messages.isNotEmpty()) {
            val lastMessage = messages.last()
            if (!lastMessage.isUser && lastMessage.text.isNotBlank()) {
                voiceManager.speak(lastMessage.text)
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.cleanup()
        }
    }
    
    // Model dialog
    if (showModelDialog) {
        ModelSelectionDialog(
            chatViewModel = chatViewModel,
            onDismiss = { showModelDialog = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Voice Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when {
                                isListening -> "🎤 Listening..."
                                isSpeaking -> "🔊 Speaking..."
                                voiceActivityDetected -> "🟢 Voice detected"
                                voiceMode -> "Voice mode active"
                                else -> statusMessage
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Voice mode toggle
                    IconButton(
                        onClick = { voiceMode = !voiceMode }
                    ) {
                        Icon(
                            imageVector = if (voiceMode) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle voice mode",
                            tint = if (voiceMode) Color(0xFF10B981) else Color.Gray
                        )
                    }
                    
                    // Model selection
                    IconButton(onClick = { showModelDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Model settings")
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
            // STT Error banner
            sttError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { voiceManager.clearSttError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
            
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyStateVoice(
                            isModelReady = isModelVerified,
                            onShowModelDialog = { showModelDialog = true }
                        )
                    }
                } else {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            onSpeak = {
                                if (!message.isUser) {
                                    voiceManager.speak(message.text)
                                }
                            },
                            isSpeaking = isSpeaking
                        )
                    }
                }
                
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }
            
            // Input section
            InputSection(
                userInput = userInput,
                onInputChange = { userInput = it },
                onSend = {
                    if (userInput.isNotBlank() && isModelVerified) {
                        chatViewModel?.sendMessage(userInput)
                        userInput = ""
                    }
                },
                onVoiceInput = {
                    if (micPermissionState.status.isGranted) {
                        if (isListening) {
                            voiceManager.stopListening()
                        } else {
                            voiceManager.startListening()
                        }
                    } else {
                        micPermissionState.launchPermissionRequest()
                    }
                },
                onStopSpeaking = { voiceManager.stopSpeaking() },
                isLoading = isLoading,
                isModelReady = isModelVerified,
                isListening = isListening,
                isSpeaking = isSpeaking
            )
        }
    }
}

@Composable
private fun EmptyStateVoice(
    isModelReady: Boolean,
    onShowModelDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isModelReady) "Voice Assistant Ready" else "Model Not Loaded",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isModelReady) 
                "Tap the microphone to start speaking or type your question below"
            else 
                "Please load an AI model to start chatting",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isModelReady) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onShowModelDialog) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Model")
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onSpeak: () -> Unit,
    isSpeaking: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isUser) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = if (message.isUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (!message.isUser) {
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "Speak",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            if (message.isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InputSection(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    onStopSpeaking: () -> Unit,
    isLoading: Boolean,
    isModelReady: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Voice input button
            IconButton(
                onClick = onVoiceInput,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = when {
                            isListening -> Color(0xFFEF4444)
                            !isModelReady -> Color.Gray
                            else -> MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Voice input",
                    tint = Color.White
                )
            }
            
            // Text input
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isListening) "Listening..." else "Type or speak...") },
                enabled = !isLoading && isModelReady && !isListening,
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            
            // Send button
            IconButton(
                onClick = onSend,
                enabled = userInput.isNotBlank() && !isLoading && isModelReady,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (userInput.isNotBlank() && isModelReady)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
            
            // Stop speaking button (if speaking)
            if (isSpeaking) {
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFFEF4444),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Stop speaking",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelSelectionDialog(
    chatViewModel: ChatViewModel?,
    onDismiss: () -> Unit
) {
    // Use the existing model selection from ChatScreen
    // This is a placeholder - you can reuse the ModelSelectionDialog from ChatScreen
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Model Settings") },
        text = { Text("Model selection dialog - integrate with ChatViewModel") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
