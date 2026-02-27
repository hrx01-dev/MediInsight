package com.runanywhere.startup_hackathon20.ui_screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ChatViewModel
import com.runanywhere.startup_hackathon20.ChatMessage
import com.runanywhere.startup_hackathon20.R
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import com.runanywhere.startup_hackathon20.viewmodel.VoiceViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class Message(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen(
    presetMessage: String? = null,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
    voiceViewModel: VoiceViewModel= viewModel()
) {
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var showModelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Voice state from VoiceViewModel
    val voiceState by (voiceViewModel.voiceState
        ?: MutableStateFlow(com.runanywhere.startup_hackathon20.viewmodel.VoiceState())).collectAsState()
    val modelState by (voiceViewModel.modelState
        ?: MutableStateFlow(com.runanywhere.startup_hackathon20.viewmodel.ModelLoadingState())).collectAsState()

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start recording
            voiceViewModel?.startRecording()
        }
    }
    
    // Collect messages from ViewModel FIRST
    val messages by (viewModel.messages.collectAsState())
    val isLoading by (viewModel.isLoading.collectAsState())
    val currentModelId by (viewModel.currentModelId
        ).collectAsState()
    val statusMessage by (viewModel.statusMessage
       ).collectAsState()
    val availableModels by (viewModel.availableModels
         ).collectAsState()
    val downloadProgress by (viewModel.downloadProgress
         ).collectAsState()
    val isModelVerified by (viewModel.isModelVerified ).collectAsState()
    
    LaunchedEffect(presetMessage, isModelVerified) {
        if (!presetMessage.isNullOrBlank() && isModelVerified && messages.isEmpty()) {
            viewModel.sendMessage(presetMessage)
        }
    }
    // Update input text when transcription is complete
    LaunchedEffect(voiceState.transcribedText) {
        if (voiceState.transcribedText.isNotEmpty() && !voiceState.isTranscribing) {
            inputText = TextFieldValue(voiceState.transcribedText)
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val suggestions = listOf(
        "Give me a tip",
        "Explain this",
        "Best practices",
        "How to improve"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // 🔥 TOP BAR WITH GRADIENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // BACK BUTTON
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Expert Chat",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                id = if (isModelVerified) R.drawable.ic_wifi_off else R.drawable.ic_wifi_off
                            ),
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                isModelVerified -> "Ready"
                                currentModelId != null -> "Loading..."
                                else -> "No Model"
                            },
                            color = MaterialTheme.colorScheme.onPrimary.copy(0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Model Selection Button - Always visible
                IconButton(
                    onClick = { showModelDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lightbulb),
                        contentDescription = "Select Model",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Clear Chat Button
                if (messages.isNotEmpty()) {
                    var showDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Clear Chat History") },
                            text = { Text("Are you sure you want to delete all chat messages? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel?.clearAllMessages()
                                        showDialog = false
                                    }
                                ) {
                                    Text("Clear")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }

        // 🔥 MODEL SETUP BANNER (shown when no model is loaded)
        if (currentModelId == null && !isModelVerified) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lightbulb),
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Setup Required",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                "Download AI model to start chatting",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showModelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Setup AI Model", color = Color.White)
                    }

                    downloadProgress?.let { progress ->
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF59E0B),
                        )
                        Text(
                            "Downloading: ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // 🔥 MODEL SETUP DIALOG
        if (showModelDialog ) {
            AlertDialog(
                onDismissRequest = { showModelDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lightbulb),
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI Model Setup", modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.refreshModels() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_send),
                                contentDescription = "Refresh",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                text = {
                    Column {
                        if (availableModels.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            Spacer(Modifier.height(8.dp))
                            Text("Loading available models...")
                        } else {
                            // Show currently loaded model
                            if (currentModelId != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE0F2F1)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_wifi_off),
                                            contentDescription = null,
                                            tint = Color(0xFF00796B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))

                                        Text(    // Clear Chat Button
                                            "Currently Loaded: ${availableModels.find { it.id == currentModelId }?.name ?: currentModelId}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00796B)
                                        )
                                    }
                                }
                            }

                            Text(
                                if (currentModelId == null)
                                    "Choose an AI model to download and enable chat:"
                                else
                                    "Available models (download or switch):",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))

                            availableModels.forEach { model ->
                                val isCurrentModel = model.id == currentModelId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentModel) Color(0xFFE8F5E9) else Color(
                                            0xFFF0FDF4
                                        )
                                    ),
                                    border = if (isCurrentModel) androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        Color(0xFF4CAF50)
                                    ) else null
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                model.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isCurrentModel) {
                                                Text(
                                                    "Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .background(
                                                            Color(0xFF4CAF50),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "Size: ~374 MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        if (model.isDownloaded) {
                                            Text(
                                                "✓ Downloaded",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!model.isDownloaded) {
                                                Button(
                                                    onClick = {
                                                        viewModel.downloadModel(model.id)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF4CAF50)
                                                    ),
                                                    enabled = downloadProgress == null
                                                ) {
                                                    Text("Download", color = Color.White)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        viewModel.loadModel(model.id)
                                                        showModelDialog = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isCurrentModel) Color(
                                                            0xFF2196F3
                                                        ) else Color(0xFF2ECC71)
                                                    ),
                                                    enabled = !isCurrentModel
                                                ) {
                                                    Text(
                                                        if (isCurrentModel) "Loaded" else "Load Model",
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            downloadProgress?.let { progress ->
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF4CAF50),
                                )
                                Text(
                                    "Downloading: ${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModelDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // 🔥 MESSAGES LIST
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Show welcome message if no messages
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Hello! I'm your offline expert assistant.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {

                    Box(
                        modifier = Modifier
                            .then(
                                if (message.isUser) {
                                    Modifier.background(
                                        brush = Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                } else {
                                    Modifier.background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            )
                            .padding(14.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(
                            message.text,
                            color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 🔥 SUGGESTIONS when no messages
            if (messages.isEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Quick suggestions:",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.chunked(2).forEach { rowItems ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { suggestion ->
                                    Button(
                                        onClick = {
                                            viewModel?.sendMessage(suggestion)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        enabled = isModelVerified && !isLoading
                                    ) {
                                        Text(
                                            suggestion,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Show loading indicator
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        // 🔥 INPUT FIELD + VOICE STATUS + SEND BUTTON
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            // Voice recording status indicator
            if (voiceState.isRecording || voiceState.isTranscribing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (voiceState.isRecording)
                            Color(0xFFEF4444).copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (voiceState.isRecording) {
                            // Recording animation
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "🎤 Recording... Tap stop when done",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444)
                            )
                        } else if (voiceState.isTranscribing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Processing speech...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Audio level indicator (if recording)
                        if (voiceState.isRecording && voiceState.audioLevel > 0) {
                            Spacer(Modifier.weight(1f))
                            LinearProgressIndicator(
                                progress = { voiceState.audioLevel },
                                modifier = Modifier.width(60.dp),
                                color = Color(0xFFEF4444),
                            )
                        }
                     }
                 }
             }
             
             // STT model status warning
             if (!voiceState.isRecording && !voiceState.isTranscribing && !modelState.isSTTLoaded) {
                 Card(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(horizontal = 10.dp, vertical = 4.dp),
                     colors = CardDefaults.cardColors(
                         containerColor = Color(0xFFFEF3C7)
                     ),
                     shape = RoundedCornerShape(8.dp)
                 ) {
                     Row(
                         modifier = Modifier
                             .fillMaxWidth()
                             .padding(8.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Icon(
                             Icons.Default.Warning,
                             contentDescription = null,
                             tint = Color(0xFFF59E0B),
                             modifier = Modifier.size(16.dp)
                         )
                         Spacer(Modifier.width(8.dp))
                         Text(
                             "Load an STT model to use voice input",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color(0xFF92400E)
                         )
                     }
                 }
             }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type your question...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.width(8.dp))

                // Voice Input Button - RunAnywhere STT
                IconButton(
                    onClick = {
                        if (voiceState.isRecording) {
                            // Stop recording and transcribe
                            voiceViewModel?.stopRecordingAndTranscribe()
                        } else {
                            // Check if STT model is loaded
                            if (!modelState.isSTTLoaded) {
                                // Show message to load STT model first
                                return@IconButton
                            }
                            // Request audio permission and start recording
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    enabled = if (voiceState.isRecording) true else modelState.isSTTLoaded && !isLoading,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                voiceState.isRecording -> {
                                    // Recording - red gradient
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFF87171))
                                    )
                                }
                                modelState.isSTTLoaded && !isLoading -> {
                                    // Ready - green/teal gradient
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                }
                                else -> {
                                    // Disabled - gray
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            }
                        )
                ) {
                    if (voiceState.isTranscribing) {
                        // Show loading indicator while transcribing
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (voiceState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (voiceState.isRecording) "Stop Recording" else "Voice Input",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.text.isNotBlank()) {
                            viewModel?.sendMessage(inputText.text)
                            inputText = TextFieldValue("")
                        }
                    },
                    enabled = isModelVerified && !isLoading && inputText.text.isNotBlank(),
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isModelVerified && !isLoading && inputText.text.isNotBlank()) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun ChatScreenPreview() {
        Startup_hackathon20Theme {
            ChatScreen(onBack = {})
        }

    }
