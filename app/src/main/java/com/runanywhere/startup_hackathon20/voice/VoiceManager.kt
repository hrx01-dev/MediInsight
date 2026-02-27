package com.runanywhere.startup_hackathon20.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages voice input (STT) and output (TTS) using Android's native Speech APIs
 * 
 * Features:
 * - Speech-to-Text using Android SpeechRecognizer
 * - Text-to-Speech using Android TextToSpeech
 * - Voice Activity Detection using AudioRecord
 * 
 * Note: This uses Android's native APIs. When RunAnywhere SDK modules become available,
 * migrate to:
 * - RunAnywhere.transcribe() for STT
 * - RunAnywhere.speak() for TTS
 * - RunAnywhere.detectVoiceActivity() for VAD
 */
class VoiceManager(private val context: Context) {

    // Text-to-Speech
    private var tts: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Speech-to-Text
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    var onSpeechResult: ((String) -> Unit)? = null

    private val _sttError = MutableStateFlow<String?>(null)
    val sttError: StateFlow<String?> = _sttError.asStateFlow()

    // Voice Activity Detection
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val _voiceActivityDetected = MutableStateFlow(false)
    val voiceActivityDetected: StateFlow<Boolean> = _voiceActivityDetected.asStateFlow()

    companion object {
        private const val TAG = "VoiceManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val VAD_THRESHOLD = 1000 // Adjust based on environment
    }

    init {
        initializeTTS()
        initializeSpeechRecognizer()
    }

    // ======================== TEXT-TO-SPEECH ========================

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language not supported")
                } else {
                    _isTtsReady.value = true
                    Log.d(TAG, "TTS initialized successfully")
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                Log.e(TAG, "TTS error for utterance: $utteranceId")
            }
        })
    }

    /**
     * Speak text aloud using TextToSpeech
     * @param text The text to speak
     * @param rate Speech rate (0.5 = slow, 1.0 = normal, 2.0 = fast)
     * @param pitch Voice pitch (0.5 = low, 1.0 = normal, 2.0 = high)
     */
    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!_isTtsReady.value) {
            Log.w(TAG, "TTS not ready")
            return
        }

        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId_${System.currentTimeMillis()}")

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageId_${System.currentTimeMillis()}")
        Log.d(TAG, "Speaking: $text")
    }

    /**
     * Stop current speech playback
     */
    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    // ======================== SPEECH-TO-TEXT ========================

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _isListening.value = true
                _sttError.value = null
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed - could use for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Partial audio data - not used
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                _isListening.value = false
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error: $error"
                }
                Log.e(TAG, "Recognition error: $errorMessage")
                _sttError.value = errorMessage
                _isListening.value = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    _recognizedText.value = recognizedText
                    Log.d(TAG, "Recognized: $recognizedText")
                }
                _isListening.value = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        })
    }

    /**
     * Start listening for speech input
     */
    fun startListening() {
        if (_isListening.value) {
            Log.w(TAG, "Already listening")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _sttError.value = "Failed to start listening: ${e.message}"
            _isListening.value = false
        }
    }

    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Cancel speech recognition
     */
    fun cancelListening() {
        speechRecognizer?.cancel()
        _isListening.value = false
        Log.d(TAG, "Cancelled listening")
    }

    /**
     * Clear recognized text
     */
    fun clearRecognizedText() {
        _recognizedText.value = ""
    }

    /**
     * Clear STT error
     */
    fun clearSttError() {
        _sttError.value = null
    }

    // ======================== VOICE ACTIVITY DETECTION ========================

    /**
     * Start voice activity detection
     * Simple implementation using audio amplitude
     */
    fun startVAD() {
        if (isRecording) {
            Log.w(TAG, "Already recording for VAD")
            return
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            // Process audio in a separate thread
            Thread {
                val buffer = ShortArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        // Calculate RMS amplitude
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = kotlin.math.sqrt(sum / read)

                        // Detect voice activity based on threshold
                        val voiceDetected = rms > VAD_THRESHOLD
                        if (voiceDetected != _voiceActivityDetected.value) {
                            _voiceActivityDetected.value = voiceDetected
                            Log.d(TAG, "Voice activity: $voiceDetected (RMS: $rms)")
                        }
                    }
                }
            }.start()

            Log.d(TAG, "VAD started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VAD", e)
            isRecording = false
        }
    }

    /**
     * Stop voice activity detection
     */
    fun stopVAD() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _voiceActivityDetected.value = false
        Log.d(TAG, "VAD stopped")
    }

    // ======================== CLEANUP ========================

    /**
     * Release all resources
     */
    fun cleanup() {
        stopSpeaking()
        stopListening()
        stopVAD()

        tts?.shutdown()
        tts = null

        speechRecognizer?.destroy()
        speechRecognizer = null

        Log.d(TAG, "VoiceManager cleaned up")
    }
}
