package com.runanywhere.startup_hackathon20.ui_screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
import com.runanywhere.startup_hackathon20.viewmodel.AndroidSpeechViewModel
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
    androidSpeechViewModel: AndroidSpeechViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var showModelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Android Speech Recognition state
    val speechState by androidSpeechViewModel.speechState.collectAsState()

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("ChatScreen", "Audio permission result: $isGranted")
        if (isGranted) {
            // Permission granted, start listening
            android.util.Log.d("ChatScreen", "Permission granted, starting listening...")
            androidSpeechViewModel.startListening()
        } else {
            android.util.Log.w("ChatScreen", "Audio permission denied")
            // Permission denied - show a message to user
            // You could also show a dialog explaining why the permission is needed
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
    
    // Track if we already sent the preset message
    var presetMessageSent by remember { mutableStateOf(false) }
    
    LaunchedEffect(presetMessage, isModelVerified, currentModelId) {
        android.util.Log.d("ChatScreen", "Preset check - Message: ${!presetMessage.isNullOrBlank()}, Verified: $isModelVerified, ModelID: $currentModelId, Sent: $presetMessageSent")
        
        // Only send if ALL conditions are met
        if (!presetMessage.isNullOrBlank() && isModelVerified && currentModelId != null && !presetMessageSent) {
            // Wait to ensure model is fully ready
            android.util.Log.d("ChatScreen", "Model verified, waiting 2 seconds before sending preset...")
            kotlinx.coroutines.delay(2000)
            android.util.Log.d("ChatScreen", "Sending preset message: ${presetMessage.take(50)}...")
            viewModel.sendMessage(presetMessage)
            presetMessageSent = true
        } else if (!presetMessage.isNullOrBlank() && !presetMessageSent) {
            android.util.Log.d("ChatScreen", "Waiting for model - Verified: $isModelVerified, ModelID: $currentModelId, Status: $statusMessage")
        }
    }
    // SIMPLE TEST: Just set text whenever transcribedText changes (no conditions)
    LaunchedEffect(speechState.transcribedText) {
        android.util.Log.d("ChatScreen", "Simple LaunchedEffect - transcribedText changed to: '${speechState.transcribedText}'")
        if (speechState.transcribedText.isNotEmpty()) {
            android.util.Log.d("ChatScreen", "SIMPLE TEST - Setting input text to: '${speechState.transcribedText}'")
            android.util.Log.d("ChatScreen", "Input text before: '${inputText.text}'")
            inputText = TextFieldValue(speechState.transcribedText)
            android.util.Log.d("ChatScreen", "Input text after: '${inputText.text}'")
        }
    }
    
    // Debug: Log all speech state changes
    LaunchedEffect(speechState) {
        android.util.Log.d("ChatScreen", "SpeechState changed: isListening=${speechState.isListening}, " +
                "isProcessing=${speechState.isProcessing}, transcribedText='${speechState.transcribedText}', " +
                "error=${speechState.error}, statusMessage='${speechState.statusMessage}'")
    }
    
    // REMOVED COMPLEX CONDITIONS FOR TESTING - will re-add after basic version works
    /*
    // Update input text when Android speech transcription is complete
    LaunchedEffect(speechState.transcribedText, speechState.isListening, speechState.isProcessing) {
        android.util.Log.d("ChatScreen", "LaunchedEffect1 triggered - transcribedText='${speechState.transcribedText}', " +
                "isListening=${speechState.isListening}, isProcessing=${speechState.isProcessing}, error=${speechState.error}")
        
        // Only update input text when we have transcription AND we're done processing
        if (speechState.transcribedText.isNotEmpty() && 
            !speechState.isListening && 
            !speechState.isProcessing &&
            speechState.error == null) {
            
            android.util.Log.d("ChatScreen", "CONDITIONS MET - Setting transcribed text: '${speechState.transcribedText}'")
            android.util.Log.d("ChatScreen", "Current inputText before: '${inputText.text}'")
            inputText = TextFieldValue(speechState.transcribedText)
            android.util.Log.d("ChatScreen", "Current inputText after: '${inputText.text}'")
            
            // Delay before clearing to ensure text is set properly
            kotlinx.coroutines.delay(500) // Increased delay to ensure UI update
            android.util.Log.d("ChatScreen", "About to clear transcription...")
            androidSpeechViewModel.clearTranscription()
        } else {
            android.util.Log.d("ChatScreen", "CONDITIONS NOT MET - transcribedText.isEmpty()=${speechState.transcribedText.isEmpty()}, " +
                    "isListening=${speechState.isListening}, isProcessing=${speechState.isProcessing}, error=${speechState.error}")
        }
    }
    
    // Alternative approach: Monitor when processing completes with text
    LaunchedEffect(speechState.isProcessing) {
        android.util.Log.d("ChatScreen", "LaunchedEffect2 triggered - isProcessing=${speechState.isProcessing}")
        
        // When processing changes from true to false, check if we have text
        if (!speechState.isProcessing && speechState.transcribedText.isNotEmpty() && speechState.error == null) {
            kotlinx.coroutines.delay(100) // Small delay to ensure state is stable
            if (inputText.text != speechState.transcribedText) { // Only update if different
                android.util.Log.d("ChatScreen", "FALLBACK TRIGGERED - Setting transcribed text: '${speechState.transcribedText}'")
                android.util.Log.d("ChatScreen", "Fallback: Current inputText before: '${inputText.text}'")
                inputText = TextFieldValue(speechState.transcribedText)
                android.util.Log.d("ChatScreen", "Fallback: Current inputText after: '${inputText.text}'")
            } else {
                android.util.Log.d("ChatScreen", "Fallback: Text already matches, no update needed")
            }
        }
    }
    */

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

        // 🔥 MODEL SETUP BANNER (shown only when model is NOT verified and not loading)
        if (!isModelVerified && (statusMessage.contains("Setup", ignoreCase = true) || statusMessage.contains("Download", ignoreCase = true) || statusMessage.contains("failed", ignoreCase = true))) {
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
        
        // 🔥 MODEL LOADING BANNER (shown when model is being loaded/verified)
        if ((statusMessage.contains("Loading", ignoreCase = true) || statusMessage.contains("Initializing", ignoreCase = true) || statusMessage.contains("Verifying", ignoreCase = true)) && !isModelVerified) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF2196F3)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Preparing AI Model",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1565C0)
                            )
                        }
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
            // Android Speech Recognition status indicator
            if (speechState.isListening || speechState.isProcessing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (speechState.isListening)
                            Color(0xFF10B981).copy(alpha = 0.1f)  // Green for listening
                        else
                            MaterialTheme.colorScheme.primaryContainer  // Blue for processing
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (speechState.isListening) {
                            // Listening animation
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "🎤 Listening... Speak now or tap to stop",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF059669)
                            )
                        } else if (speechState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                speechState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Audio level indicator (if listening)
                        if (speechState.isListening && speechState.audioLevel > 0) {
                            Spacer(Modifier.weight(1f))
                            LinearProgressIndicator(
                                progress = { speechState.audioLevel },
                                modifier = Modifier.width(60.dp),
                                color = Color(0xFF10B981),
                            )
                        }
                     }
                 }
             }
             
             // Speech recognition availability warning
             if (!speechState.isListening && !speechState.isProcessing && !speechState.isAvailable) {
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
                             speechState.statusMessage,
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

                // Voice Input Button - Android Speech Recognition
                IconButton(
                    onClick = {
                        android.util.Log.d("ChatScreen", "Microphone button clicked - isListening: ${speechState.isListening}")
                        if (speechState.isListening) {
                            // Stop listening
                            android.util.Log.d("ChatScreen", "Stopping listening...")
                            androidSpeechViewModel.stopListening()
                        } else {
                            // Check if speech recognition is available
                            android.util.Log.d("ChatScreen", "Speech recognition available: ${speechState.isAvailable}")
                            if (!speechState.isAvailable) {
                                android.util.Log.w("ChatScreen", "Speech recognition not available")
                                return@IconButton
                            }
                            // Start listening (don't clear transcription here to avoid interference)
                            // Check if permission is already granted
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, 
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            android.util.Log.d("ChatScreen", "Has audio permission: $hasPermission")
                            
                            if (hasPermission) {
                                // Permission already granted, start listening directly
                                android.util.Log.d("ChatScreen", "Starting listening directly (permission already granted)...")
                                androidSpeechViewModel.startListening()
                            } else {
                                // Request audio permission and start listening
                                android.util.Log.d("ChatScreen", "Requesting audio permission...")
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    enabled = if (speechState.isListening) true else speechState.isAvailable && !isLoading,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                speechState.isListening -> {
                                    // Listening - green gradient
                                    Brush.linearGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF34D399))
                                    )
                                }
                                speechState.isAvailable && !isLoading -> {
                                    // Ready - blue/teal gradient
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
                    if (speechState.isProcessing) {
                        // Show loading indicator while processing
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (speechState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (speechState.isListening) "Stop Listening" else "Voice Input",
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
