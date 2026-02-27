package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class AndroidSpeechState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val transcribedText: String = "",
    val statusMessage: String = "Ready to listen",
    val audioLevel: Float = 0f,
    val error: String? = null,
    val isAvailable: Boolean = false
)

/**
 * ViewModel for Android's built-in Speech Recognition
 * Uses SpeechRecognizer API instead of custom STT models
 */
class AndroidSpeechViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "AndroidSpeechViewModel"
    private val context: Context = application.applicationContext
    
    // Speech state
    private val _speechState = MutableStateFlow(AndroidSpeechState())
    val speechState: StateFlow<AndroidSpeechState> = _speechState
    
    // Speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    
    init {
        initializeSpeechRecognizer()
    }
    
    private fun initializeSpeechRecognizer() {
        try {
            // Check if speech recognition is available
            val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
            
            if (isAvailable) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupRecognizerIntent()
                setupRecognitionListener()
                
                _speechState.value = _speechState.value.copy(
                    isAvailable = true,
                    statusMessage = "Speech recognition ready"
                )
                Log.d(TAG, "Speech recognizer initialized successfully")
            } else {
                _speechState.value = _speechState.value.copy(
                    isAvailable = false,
                    statusMessage = "Speech recognition not available on this device",
                    error = "Speech recognition service not available"
                )
                Log.w(TAG, "Speech recognition not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer: ${e.message}", e)
            _speechState.value = _speechState.value.copy(
                isAvailable = false,
                error = "Failed to initialize: ${e.message}",
                statusMessage = "Speech recognition initialization failed"
            )
        }
    }
    
    private fun setupRecognizerIntent() {
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }
    }
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = true,
                        isProcessing = false,
                        statusMessage = "Listening... Speak now",
                        error = null
                    )
                }
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        statusMessage = "Speech detected...",
                        audioLevel = 0.5f
                    )
                }
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Convert RMS to a 0-1 range for audio level indicator
                val level = (rmsdB + 10f) / 20f // Normalize roughly
                val clampedLevel = level.coerceIn(0f, 1f)
                
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(audioLevel = clampedLevel)
                }
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Optional: Handle raw audio buffer if needed
                Log.d(TAG, "Buffer received: ${buffer?.size ?: 0} bytes")
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        isProcessing = true,
                        statusMessage = "Processing speech...",
                        audioLevel = 0f
                    )
                }
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error" 
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                    else -> "Unknown error: $error"
                }
                
                Log.e(TAG, "Speech recognition error: $errorMessage")
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        isProcessing = false,
                        error = errorMessage,
                        statusMessage = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            "No speech detected. Try again."
                        } else {
                            "Error: $errorMessage"
                        },
                        audioLevel = 0f
                    )
                }
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcription = matches?.firstOrNull() ?: ""
                
                Log.d(TAG, "Speech recognition results: '$transcription' (matches: ${matches?.size ?: 0})")
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        isProcessing = false,
                        transcribedText = transcription,
                        statusMessage = if (transcription.isNotEmpty()) {
                            "Speech recognized: $transcription"
                        } else {
                            "No speech recognized"
                        },
                        audioLevel = 0f,
                        error = null
                    )
                    Log.d(TAG, "Updated speech state - transcribedText: '$transcription'")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                
                if (partialText.isNotEmpty()) {
                    Log.d(TAG, "Partial results: $partialText")
                    viewModelScope.launch {
                        _speechState.value = _speechState.value.copy(
                            statusMessage = "Recognizing: $partialText"
                        )
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Speech event: $eventType")
            }
        })
    }
    
    /**
     * Start listening for speech input
     */
    fun startListening() {
        viewModelScope.launch {
            try {
                if (!_speechState.value.isAvailable) {
                    _speechState.value = _speechState.value.copy(
                        error = "Speech recognition not available",
                        statusMessage = "Speech recognition service not available"
                    )
                    return@launch
                }
                
                if (_speechState.value.isListening || _speechState.value.isProcessing) {
                    Log.w(TAG, "Already listening or processing")
                    return@launch
                }
                
                // Clear previous results
                _speechState.value = _speechState.value.copy(
                    transcribedText = "", // Clear old transcription
                    error = null,
                    statusMessage = "Initializing...",
                    audioLevel = 0f
                )
                
                recognizerIntent?.let { intent ->
                    speechRecognizer?.startListening(intent)
                    Log.d(TAG, "Started listening for speech")
                } ?: run {
                    _speechState.value = _speechState.value.copy(
                        error = "Recognition intent not configured",
                        statusMessage = "Failed to start listening"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition: ${e.message}", e)
                _speechState.value = _speechState.value.copy(
                    isListening = false,
                    isProcessing = false,
                    error = "Failed to start: ${e.message}",
                    statusMessage = "Error starting speech recognition"
                )
            }
        }
    }
    
    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        viewModelScope.launch {
            try {
                speechRecognizer?.stopListening()
                Log.d(TAG, "Stopped listening for speech")
                
                _speechState.value = _speechState.value.copy(
                    isListening = false,
                    statusMessage = "Stopped listening",
                    audioLevel = 0f
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition: ${e.message}", e)
                _speechState.value = _speechState.value.copy(
                    isListening = false,
                    isProcessing = false,
                    error = "Error stopping: ${e.message}",
                    audioLevel = 0f
                )
            }
        }
    }
    
    /**
     * Cancel current speech recognition
     */
    fun cancelListening() {
        viewModelScope.launch {
            try {
                speechRecognizer?.cancel()
                Log.d(TAG, "Cancelled speech recognition")
                
                _speechState.value = _speechState.value.copy(
                    isListening = false,
                    isProcessing = false,
                    statusMessage = "Cancelled",
                    audioLevel = 0f,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling speech recognition: ${e.message}", e)
                _speechState.value = _speechState.value.copy(
                    isListening = false,
                    isProcessing = false,
                    audioLevel = 0f
                )
            }
        }
    }
    
    /**
     * Clear the transcribed text
     */
    fun clearTranscription() {
        Log.d(TAG, "Clearing transcription")
        _speechState.value = _speechState.value.copy(
            transcribedText = "",
            error = null,
            statusMessage = if (_speechState.value.isAvailable) "Ready to listen" else "Speech recognition not available"
        )
    }
    
    /**
     * Check if speech recognition is available
     */
    fun isRecognitionAvailable(): Boolean {
        return _speechState.value.isAvailable
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "Speech recognizer destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer: ${e.message}", e)
        }
    }
}