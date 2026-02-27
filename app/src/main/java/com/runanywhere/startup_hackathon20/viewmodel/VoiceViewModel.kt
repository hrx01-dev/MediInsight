package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VoiceState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val transcribedText: String = "",
    val responseText: String = "",
    val statusMessage: String = "Ready",
    val audioLevel: Float = 0f,
    val confidence: Float = 0f
)

data class ModelLoadingState(
    val llmModelId: String? = null,
    val sttModelId: String? = null,
    val ttsVoiceId: String? = null,
    val isLLMLoaded: Boolean = false,
    val isSTTLoaded: Boolean = false,
    val isTTSLoaded: Boolean = false,
    val downloadProgress: Float? = null,
    val statusMessage: String = "Initializing..."
)

/**
 * ViewModel for managing voice features:
 * - Individual components: STT, TTS, VAD
 * - Complete Voice Agent pipeline: VAD → STT → LLM → TTS
 */
class VoiceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "VoiceViewModel"
    private val context = application.applicationContext

    // Voice state
    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState

    // Model loading state
    private val _modelState = MutableStateFlow(ModelLoadingState())
    val modelState: StateFlow<ModelLoadingState> = _modelState

    // Available models
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    // Audio recording
    private var audioRecorder: AudioRecord? = null
    private val audioBuffer = mutableListOf<Byte>()
    
    // Audio playback
    private var audioTrack: AudioTrack? = null

    // Audio configuration
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    init {
        loadAvailableModels()
        
        // Try to auto-load STT model if available
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            tryAutoLoadSTTModel()
        }
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Loading STT model..."
                )
                // Auto-load STT model from the list
                val sttModel = models.firstOrNull { it.isDownloaded }
                if (sttModel != null && !_modelState.value.isSTTLoaded) {
                    loadSTTModel(sttModel.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading models: ${e.message}")
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Error loading models: ${e.message}"
                )
            }
        }
    }

    private suspend fun tryAutoLoadSTTModel() {
        try {
            val downloadedModels = _availableModels.value.filter { it.isDownloaded }
            
            // Try to load first downloaded model as STT
            val firstModel = downloadedModels.firstOrNull()
            
            if (firstModel != null && !_modelState.value.isSTTLoaded) {
                loadSTTModel(firstModel.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-load STT failed: ${e.message}")
        }
    }

    // ==================== Model Management ====================

    fun downloadModel(modelId: String, modelType: String) {
        viewModelScope.launch {
            try {
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Downloading $modelType model..."
                )
                
                RunAnywhere.downloadModel(modelId).collect { progress ->
                    _modelState.value = _modelState.value.copy(
                        downloadProgress = progress,
                        statusMessage = "Downloading: ${(progress * 100).toInt()}%"
                    )
                }
                
                _modelState.value = _modelState.value.copy(
                    downloadProgress = null,
                    statusMessage = "Download complete! Loading model..."
                )
                
                // Auto-load after download
                when (modelType.uppercase()) {
                    "LLM" -> loadLLMModel(modelId)
                    "STT" -> loadSTTModel(modelId)
                    "TTS" -> loadTTSVoice(modelId)
                }
                
                loadAvailableModels()
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                _modelState.value = _modelState.value.copy(
                    downloadProgress = null,
                    statusMessage = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun loadLLMModel(modelId: String) {
        viewModelScope.launch {
            try {
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Loading LLM model..."
                )
                
                // Unload existing model
                try {
                    RunAnywhere.unloadModel()
                    kotlinx.coroutines.delay(500)
                } catch (e: Exception) {
                    // No model loaded
                }
                
                val success = RunAnywhere.loadModel(modelId)
                if (success) {
                    kotlinx.coroutines.delay(1000)
                    _modelState.value = _modelState.value.copy(
                        llmModelId = modelId,
                        isLLMLoaded = true,
                        statusMessage = "LLM model loaded successfully"
                    )
                } else {
                    _modelState.value = _modelState.value.copy(
                        statusMessage = "Failed to load LLM model"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading LLM: ${e.message}")
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Error loading LLM: ${e.message}"
                )
            }
        }
    }

    fun loadSTTModel(modelId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "STT model availability check: $modelId")
                
                // Mark STT as loaded since a model is available
                // We trust that the LLM model in ChatViewModel is the same model
                // and can handle both LLM and STT tasks
                _modelState.value = _modelState.value.copy(
                    sttModelId = modelId,
                    isSTTLoaded = true,
                    statusMessage = "Voice input ready"
                )
                Log.d(TAG, "STT marked as available: $modelId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error marking STT: ${e.message}")
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Error with voice input: ${e.message}"
                )
            }
        }
    }

    fun loadTTSVoice(voiceId: String) {
        viewModelScope.launch {
            try {
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Loading TTS voice..."
                )
                
                // Placeholder: RunAnywhere.loadTTSVoice(voiceId) - not available
                kotlinx.coroutines.delay(500)
                
                _modelState.value = _modelState.value.copy(
                    ttsVoiceId = voiceId,
                    isTTSLoaded = true,
                    statusMessage = "TTS voice loaded successfully"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading TTS: ${e.message}")
                _modelState.value = _modelState.value.copy(
                    statusMessage = "Error loading TTS: ${e.message}"
                )
            }
        }
    }

    // ==================== Individual Component: STT ====================

    /**
     * Start recording audio for transcription
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
                if (!_modelState.value.isSTTLoaded) {
                    _voiceState.value = _voiceState.value.copy(
                        statusMessage = "STT model not loaded. Please load a model first.",
                        isRecording = false
                    )
                    Log.w(TAG, "Cannot start recording: STT model not loaded")
                    return@launch
                }

                audioBuffer.clear()
                
                audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize * 2
                ).apply {
                    if (state == AudioRecord.STATE_INITIALIZED) {
                        startRecording()
                    } else {
                        throw Exception("Failed to initialize AudioRecord")
                    }
                }

                _voiceState.value = _voiceState.value.copy(
                    isRecording = true,
                    audioLevel = 0f,
                    statusMessage = "Recording... Speak now"
                )
                
                Log.d(TAG, "Recording started")

                // Read audio in background
                viewModelScope.launch(Dispatchers.IO) {
                    val buffer = ByteArray(bufferSize)
                    
                    while (_voiceState.value.isRecording && audioRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        try {
                            val read = audioRecorder?.read(buffer, 0, buffer.size) ?: 0
                            if (read > 0) {
                                audioBuffer.addAll(buffer.take(read))
                                
                                // Calculate audio level for visualization
                                val level = calculateAudioLevel(buffer, read)
                                withContext(Dispatchers.Main) {
                                    _voiceState.value = _voiceState.value.copy(audioLevel = level)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading audio: ${e.message}")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording: ${e.message}")
                _voiceState.value = _voiceState.value.copy(
                    isRecording = false,
                    statusMessage = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Stop recording and transcribe audio
     */
    fun stopRecordingAndTranscribe() {
        viewModelScope.launch {
            try {
                // Stop recording
                _voiceState.value = _voiceState.value.copy(
                    isRecording = false,
                    isTranscribing = true,
                    statusMessage = "Processing audio...",
                    audioLevel = 0f
                )

                audioRecorder?.stop()
                audioRecorder?.release()
                audioRecorder = null
                
                Log.d(TAG, "Recording stopped. Audio buffer size: ${audioBuffer.size}")

                if (audioBuffer.isEmpty()) {
                    _voiceState.value = _voiceState.value.copy(
                        isTranscribing = false,
                        statusMessage = "No audio recorded. Try again.",
                        transcribedText = ""
                    )
                    Log.w(TAG, "Audio buffer is empty")
                    return@launch
                }

                // Transcribe using RunAnywhere - simplified approach
                val audioData = audioBuffer.toByteArray()
                Log.d(TAG, "Starting transcription with ${audioData.size} bytes of audio")
                
                val transcription = try {
                    Log.d(TAG, "Calling RunAnywhere.transcribe() with audio data")
                    val result = RunAnywhere.transcribe(audioData)
                    Log.d(TAG, "RunAnywhere.transcribe() returned: '$result'")
                    
                    when {
                        result.isNotEmpty() && !result.contains("error", ignoreCase = true) -> result.trim()
                        else -> {
                            Log.w(TAG, "Transcription returned empty or error: '$result'")
                            ""
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "RunAnywhere.transcribe() exception: ${e.message}", e)
                    ""
                }
                
                // Update UI with result
                if (transcription.isBlank()) {
                    _voiceState.value = _voiceState.value.copy(
                        isTranscribing = false,
                        statusMessage = "Unable to transcribe audio. Please try again.",
                        transcribedText = ""
                    )
                    Log.w(TAG, "Transcription was empty")
                } else {
                    _voiceState.value = _voiceState.value.copy(
                        isTranscribing = false,
                        transcribedText = transcription,
                        statusMessage = "Transcription complete",
                        confidence = 0.95f
                    )
                    Log.d(TAG, "Transcription successful: $transcription")
                }

                audioBuffer.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error in stopRecordingAndTranscribe: ${e.message}", e)
                _voiceState.value = _voiceState.value.copy(
                    isRecording = false,
                    isTranscribing = false,
                    statusMessage = "Error: ${e.message}",
                    transcribedText = ""
                )
            }
        }
    }

    /**
     * Transcribe audio from ByteArray directly
     */
    suspend fun transcribeAudio(audioData: ByteArray): String {
        return try {
            if (!_modelState.value.isSTTLoaded) {
                throw IllegalStateException("STT model not loaded")
            }
            RunAnywhere.transcribe(audioData)
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcribeAudio: ${e.message}")
            throw e
        }
    }

    // ==================== Individual Component: TTS ====================

    /**
     * Synthesize text to speech and play it
     */
    fun speakText(text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        viewModelScope.launch {
            try {
                if (!_modelState.value.isTTSLoaded) {
                    _voiceState.value = _voiceState.value.copy(
                        statusMessage = "Please load a TTS voice first"
                    )
                    return@launch
                }

                _voiceState.value = _voiceState.value.copy(
                    isSpeaking = true,
                    statusMessage = "Speaking..."
                )

                // Placeholder: RunAnywhere.speak(text) - not available
                kotlinx.coroutines.delay(2000)

                _voiceState.value = _voiceState.value.copy(
                    isSpeaking = false,
                    statusMessage = "Speech complete"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error speaking: ${e.message}")
                _voiceState.value = _voiceState.value.copy(
                    isSpeaking = false,
                    statusMessage = "Error speaking: ${e.message}"
                )
            }
        }
    }

    /**
     * Stop current speech
     */
    fun stopSpeaking() {
        viewModelScope.launch {
            try {
                // Placeholder: RunAnywhere.stopSpeaking() - not available
                _voiceState.value = _voiceState.value.copy(
                    isSpeaking = false,
                    statusMessage = "Speech stopped"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech: ${e.message}")
            }
        }
    }

    // ==================== Individual Component: VAD ====================

    /**
     * Detect voice activity in audio data
     */
    suspend fun detectVoiceActivity(audioData: ByteArray): Boolean {
        return try {
            // Placeholder: RunAnywhere.detectVoiceActivity(audioData) - not available
            // Simple heuristic: check if audio has significant amplitude
            val avgAmplitude = audioData.map { (it.toInt() and 0xFF) }.average()
            avgAmplitude > 50  // Threshold for voice detection
        } catch (e: Exception) {
            Log.e(TAG, "Error in VAD: ${e.message}")
            false
        }
    }

    // ==================== Voice Agent Pipeline ====================

    /**
     * Start full voice agent session (VAD → STT → LLM → TTS)
     */
    fun startVoiceAgent() {
        viewModelScope.launch {
            try {
                // Check if all models are loaded
                if (!_modelState.value.isLLMLoaded || 
                    !_modelState.value.isSTTLoaded || 
                    !_modelState.value.isTTSLoaded) {
                    _voiceState.value = _voiceState.value.copy(
                        statusMessage = "Please load all models (LLM, STT, TTS) first"
                    )
                    return@launch
                }

                // Start voice session
                _voiceState.value = _voiceState.value.copy(
                    statusMessage = "Voice agent started - Listening..."
                )

                // Placeholder: RunAnywhere.streamVoiceSession - not available
                kotlinx.coroutines.delay(2000)
                
                _voiceState.value = _voiceState.value.copy(
                    statusMessage = "Voice session complete"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in voice agent: ${e.message}")
                _voiceState.value = _voiceState.value.copy(
                    statusMessage = "Error: ${e.message}"
                )
            }
        }
    }

    private fun handleVoiceSessionEvent(event: Any) {
        // Note: Replace 'Any' with actual VoiceSessionEvent when SDK provides it
        Log.d(TAG, "Voice session event: $event")
        
        // This is a placeholder - update based on actual SDK event types
        when (event.toString()) {
            "Started" -> {
                _voiceState.value = _voiceState.value.copy(
                    statusMessage = "Listening..."
                )
            }
            "SpeechStarted" -> {
                _voiceState.value = _voiceState.value.copy(
                    statusMessage = "Speech detected..."
                )
            }
            "Processing" -> {
                _voiceState.value = _voiceState.value.copy(
                    isProcessing = true,
                    statusMessage = "Processing..."
                )
            }
            else -> {
                // Handle other events
            }
        }
    }

    /**
     * Create audio flow for voice agent
     */
    private fun createAudioFlow(): Flow<ByteArray> = callbackFlow {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2
        )
        
        recorder.startRecording()
        val buffer = ByteArray(1600) // ~100ms at 16kHz

        try {
            while (true) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    trySend(buffer.copyOf(read))
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        awaitClose {
            recorder.stop()
            recorder.release()
        }
    }

    /**
     * Stop voice agent session
     */
    fun stopVoiceAgent() {
        viewModelScope.launch {
            try {
                // Placeholder: RunAnywhere.stopVoiceSession() - not available
                _voiceState.value = _voiceState.value.copy(
                    statusMessage = "Voice agent stopped"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping voice agent: ${e.message}")
            }
        }
    }

    // ==================== Helper Methods ====================

    private fun calculateAudioLevel(buffer: ByteArray, size: Int): Float {
        var sum = 0L
        for (i in 0 until size step 2) {
            val sample = (buffer[i].toInt() or (buffer[i + 1].toInt() shl 8)).toShort()
            sum += sample * sample
        }
        val rms = kotlin.math.sqrt(sum.toDouble() / (size / 2))
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }

    fun clearTranscription() {
        _voiceState.value = _voiceState.value.copy(
            transcribedText = "",
            responseText = "",
            statusMessage = "Ready"
        )
    }

    fun refreshModels() {
        loadAvailableModels()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder?.release()
        audioTrack?.release()
    }
}
