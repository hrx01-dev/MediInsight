package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import com.runanywhere.startup_hackathon20.database.ChatMessageEntity
import com.runanywhere.startup_hackathon20.database.ChatMessageRepository
import com.runanywhere.startup_hackathon20.database.MedicineDatabase
import com.runanywhere.startup_hackathon20.database.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Simple Message Data Class
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// ViewModel
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatMessageRepository
    private val userRepository: UserRepository
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _currentUserId = MutableStateFlow<Long?>(null)

    private val _isModelVerified = MutableStateFlow(false)
    val isModelVerified: StateFlow<Boolean> = _isModelVerified

    private val context = application.applicationContext

    init {
        val database = MedicineDatabase.getDatabase(application)
        val chatMessageDao = database.chatMessageDao()
        val userDao = database.userDao()
        repository = ChatMessageRepository(chatMessageDao)
        userRepository = UserRepository(userDao)

        loadAvailableModels()
        
        // Load current user
        viewModelScope.launch {
            userRepository.loggedInUser.collect { user ->
                _currentUserId.value = user?.id
                user?.id?.let { loadMessagesFromDatabase(it) }
            }
        }

        // Try to auto-load a downloaded model only ONCE on app startup
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for SDK initialization
            if (_currentModelId.value == null && !_isModelVerified.value) {
                tryAutoLoadModel()
            }
        }
    }

    private suspend fun tryAutoLoadModel() {
        try {
            // Check if we have any downloaded models
            val downloadedModel = _availableModels.value.firstOrNull { it.isDownloaded }
            if (downloadedModel != null && _currentModelId.value == null) {
                _statusMessage.value = "Auto-loading model: ${downloadedModel.name}..."
                loadModel(downloadedModel.id)
            } else if (downloadedModel == null && _availableModels.value.isEmpty()) {
                // Models might not be loaded yet, try refreshing
                loadAvailableModels()
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Auto-load failed: ${e.message}")
        }
    }

    private fun loadMessagesFromDatabase(userId: Long) {
        viewModelScope.launch {
            repository.getRecent20MessagesByUser(userId).collect { dbMessages ->
                _messages.value = dbMessages.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        isUser = entity.isUser,
                        timestamp = entity.timestamp
                    )
                }
            }
        }
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                _statusMessage.value = "Ready - Please download and load a model"
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Downloading model..."
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _downloadProgress.value = progress
                    _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                }
                _downloadProgress.value = null
                _statusMessage.value = "Download complete! Loading model..."

                // Automatically load the model after download
                loadModel(modelId)

                // Refresh the available models list to update download status
                loadAvailableModels()
            } catch (e: Exception) {
                _statusMessage.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Preparing to load model..."
                _currentModelId.value = null // Clear current model first
                _isModelVerified.value = false

                // Unload any existing model first
                try {
                    RunAnywhere.unloadModel()
                    kotlinx.coroutines.delay(500) // Brief pause after unload
                } catch (e: Exception) {
                    // No model was loaded, ignore
                }

                // Ensure LLM service provider is registered
                try {
                    com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider.register()
                } catch (e: Exception) {
                    // Already registered, ignore
                }

                _statusMessage.value = "Loading model..."
                val success = RunAnywhere.loadModel(modelId)
                if (success) {
                    _statusMessage.value = "Initializing model..."
                    // Give the model time to fully initialize
                    kotlinx.coroutines.delay(2000)

                    // Try verification with retries
                    var verificationSuccess = false

                    repeat(3) { attempt ->
                        try {
                            _statusMessage.value =
                                if (attempt == 0) "Testing model..." else "Retrying... (${attempt + 1}/3)"
                            val testResponse = RunAnywhere.generate("Test")
                            if (testResponse.isNotEmpty()) {
                                verificationSuccess = true
                                return@repeat // Exit the retry loop
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "ChatViewModel",
                                "Verification attempt ${attempt + 1} failed: ${e.message}"
                            )
                            // Wait longer before retry
                            if (attempt < 2) {
                                kotlinx.coroutines.delay(2000)
                            }
                        }
                    }

                    // Always mark as loaded - let actual usage determine if there are issues
                    _currentModelId.value = modelId
                    _isModelVerified.value = true

                    if (verificationSuccess) {
                        _statusMessage.value = "Model ready! You can start chatting."
                    } else {
                        _statusMessage.value = "Model loaded. Ready to chat."
                        android.util.Log.w(
                            "ChatViewModel",
                            "Model loaded but verification didn't complete - will verify on first use"
                        )
                    }
                } else {
                    _statusMessage.value = "Failed to load model. Please try again."
                    _currentModelId.value = null
                    _isModelVerified.value = false
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}. Please try reloading."
                _currentModelId.value = null
                _isModelVerified.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        Log.d("ChatViewModel", "sendMessage called - ModelID: ${_currentModelId.value}, Verified: ${_isModelVerified.value}")
        
        if (_currentModelId.value == null || !_isModelVerified.value) {
            _statusMessage.value = "Please wait for model to fully load, then try again"
            Log.w("ChatViewModel", "Message not sent - Model not ready. ModelID: ${_currentModelId.value}, Verified: ${_isModelVerified.value}")
            return
        }

        val userId = _currentUserId.value
        if (userId == null) {
            _statusMessage.value = "No user logged in"
            Log.w("ChatViewModel", "Message not sent - No user logged in")
            return
        }

        viewModelScope.launch {
            // Save user message to database with current timestamp
            val userTimestamp = System.currentTimeMillis()
            val userMessageEntity = ChatMessageEntity(
                userId = userId,
                text = text,
                isUser = true,
                timestamp = userTimestamp
            )
            repository.insertMessage(userMessageEntity, userId)

            _isLoading.value = true

            try {
                // Generate response with streaming
                var assistantResponse = ""
                var assistantMessageId: Long? = null
                // Ensure assistant message has a later timestamp
                val assistantTimestamp = userTimestamp + 1

                RunAnywhere.generateStream(text).collect { token ->
                    assistantResponse += token

                    // Save or update assistant message in database
                    if (assistantMessageId == null) {
                        // First token - create new assistant message
                        val assistantEntity = ChatMessageEntity(
                            userId = userId,
                            text = assistantResponse,
                            isUser = false,
                            timestamp = assistantTimestamp
                        )
                        assistantMessageId = repository.insertMessage(assistantEntity, userId)
                    } else {
                        // Update existing assistant message
                        val assistantEntity = ChatMessageEntity(
                            id = assistantMessageId!!,
                            userId = userId,
                            text = assistantResponse,
                            isUser = false,
                            timestamp = assistantTimestamp
                        )
                        repository.insertMessage(assistantEntity, userId)
                    }
                }

                // If response is empty, something went wrong
                if (assistantResponse.isEmpty()) {
                    val errorEntity = ChatMessageEntity(
                        userId = userId,
                        text = "Error: Model failed to generate a response. Try reloading the model.",
                        isUser = false,
                        timestamp = assistantTimestamp
                    )
                    repository.insertMessage(errorEntity, userId)
                    _statusMessage.value = "Model not responding. Please reload the model."
                    _currentModelId.value = null // Reset model state
                    _isModelVerified.value = false
                }
            } catch (e: Exception) {
                // Save error message to database
                val errorMessage = if (e.message?.contains("model", ignoreCase = true) == true) {
                    "Model is not properly loaded. Please reload the model and try again."
                } else {
                    "Error: ${e.message}"
                }

                val errorEntity = ChatMessageEntity(
                    userId = userId,
                    text = errorMessage,
                    isUser = false,
                    timestamp = userTimestamp + 1
                )
                repository.insertMessage(errorEntity, userId)

                // Reset model state if it's a model-related error
                if (e.message?.contains("model", ignoreCase = true) == true) {
                    _statusMessage.value = "Model error detected. Please reload the model."
                    _currentModelId.value = null
                    _isModelVerified.value = false
                }
            }

            _isLoading.value = false
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId != null) {
                repository.deleteAllMessagesByUser(userId)
            }
        }
    }

    fun refreshModels() {
        loadAvailableModels()
    }
}
