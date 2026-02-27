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

class AndroidSpeechViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AndroidSpeechViewModel"
    private val context: Context = application.applicationContext

    private val _speechState = MutableStateFlow(AndroidSpeechState())
    val speechState: StateFlow<AndroidSpeechState> = _speechState

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    init {
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        try {
            val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)

            if (isAvailable) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupRecognizerIntent()
                setupRecognitionListener()

                _speechState.value = _speechState.value.copy(
                    isAvailable = true,
                    statusMessage = "Speech recognition ready"
                )
            } else {
                _speechState.value = _speechState.value.copy(
                    isAvailable = false,
                    statusMessage = "Speech recognition not available",
                    error = "Speech recognition service not available"
                )
            }
        } catch (e: Exception) {
            _speechState.value = _speechState.value.copy(
                isAvailable = false,
                error = "Initialization failed: ${e.message}",
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
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = true,
                        isProcessing = false,
                        statusMessage = "Listening...",
                        error = null
                    )
                }
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val level = ((rmsdB + 10f) / 20f).coerceIn(0f, 1f)
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(audioLevel = level)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        isProcessing = true,
                        statusMessage = "Processing...",
                        audioLevel = 0f
                    )
                }
            }

            override fun onError(error: Int) {
                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        isProcessing = false,
                        statusMessage = "Error occurred",
                        error = "Speech recognition error: $error",
                        audioLevel = 0f
                    )
                }
            }

            // ✅ FIXED VERSION (IMPORTANT PART)
            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcription = matches?.firstOrNull() ?: ""

                viewModelScope.launch {
                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        isProcessing = false,
                        transcribedText = transcription,
                        statusMessage = if (transcription.isNotEmpty()) {
                            "Speech recognized"
                        } else {
                            "No speech recognized"
                        },
                        audioLevel = 0f,
                        error = null
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""

                if (partialText.isNotEmpty()) {
                    viewModelScope.launch {
                        _speechState.value = _speechState.value.copy(
                            statusMessage = "Recognizing: $partialText"
                        )
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        viewModelScope.launch {
            if (!_speechState.value.isAvailable) return@launch
            if (_speechState.value.isListening || _speechState.value.isProcessing) return@launch

            _speechState.value = _speechState.value.copy(
                transcribedText = "",
                error = null,
                statusMessage = "Initializing..."
            )

            recognizerIntent?.let {
                speechRecognizer?.startListening(it)
            }
        }
    }

    fun stopListening() {
        viewModelScope.launch {
            speechRecognizer?.stopListening()
            _speechState.value = _speechState.value.copy(
                isListening = false,
                audioLevel = 0f
            )
        }
    }

    fun clearTranscription() {
        _speechState.value = _speechState.value.copy(
            transcribedText = "",
            statusMessage = "Ready to listen"
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}