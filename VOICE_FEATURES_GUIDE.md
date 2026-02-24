# RunAnywhere Voice Features Implementation Guide

This guide demonstrates how to use the RunAnywhere SDK's on-device voice features (STT, TTS, VAD, and Voice Agent) in your Kotlin Android application.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Setup & Permissions](#setup--permissions)
4. [Individual Components](#individual-components)
   - [Speech-to-Text (STT)](#speech-to-text-stt)
   - [Text-to-Speech (TTS)](#text-to-speech-tts)
   - [Voice Activity Detection (VAD)](#voice-activity-detection-vad)
5. [Voice Agent Pipeline](#voice-agent-pipeline)
6. [Code Examples](#code-examples)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Overview

The RunAnywhere SDK provides four main voice capabilities:

| Feature | Description | Use Case |
|---------|-------------|----------|
| **STT** | Speech-to-Text | Convert audio to text |
| **TTS** | Text-to-Speech | Convert text to audio |
| **VAD** | Voice Activity Detection | Detect when user is speaking |
| **Voice Agent** | Complete pipeline (VAD → STT → LLM → TTS) | Hands-free voice conversations |

**Key Benefits:**
- 🔒 **100% On-Device** - All processing happens locally
- ⚡ **Low Latency** - No network round-trips
- 📱 **Offline Capable** - Works without internet
- 🔐 **Privacy First** - Audio never leaves the device

---

## Architecture

### Voice Agent Pipeline

```
User Speech → VAD → STT → LLM → TTS → Audio Output
```

### Component Architecture

```
VoiceAssistantScreen (UI)
        ↓
VoiceViewModel (Business Logic)
        ↓
RunAnywhere SDK
        ↓
On-Device Models (LLM, STT, TTS)
```

---

## Setup & Permissions

### 1. AndroidManifest.xml

Add audio permissions:

```xml
<!-- Required for STT/VAD -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Optional - for better audio quality control -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### 2. Request Runtime Permissions

The VoiceAssistantScreen automatically handles permission requests:

```kotlin
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
```

### 3. Initialize SDK

In `MyApplication.kt`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalScope.launch(Dispatchers.IO) {
            RunAnywhere.initialize(
                context = this@MyApplication,
                apiKey = "dev",
                environment = SDKEnvironment.DEVELOPMENT
            )
            
            // Register LLM Service Provider
            LlamaCppServiceProvider.register()
            
            // Register models
            registerModels()
            
            // Scan for downloaded models
            RunAnywhere.scanForDownloadedModels()
        }
    }
}
```

---

## Individual Components

### Speech-to-Text (STT)

Convert spoken audio to text using on-device Whisper models.

#### Loading STT Model

```kotlin
// In VoiceViewModel
suspend fun loadSTTModel(modelId: String) {
    try {
        RunAnywhere.loadSTTModel(modelId)
        _modelState.value = _modelState.value.copy(
            sttModelId = modelId,
            isSTTLoaded = true
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error loading STT: ${e.message}")
    }
}
```

#### Recording Audio

```kotlin
// Start recording
fun startRecording() {
    audioRecorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        16000, // 16kHz sample rate (required)
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )
    audioRecorder?.startRecording()
    
    // Read audio in background
    viewModelScope.launch(Dispatchers.IO) {
        val buffer = ByteArray(bufferSize)
        while (isRecording) {
            val read = audioRecorder?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                audioBuffer.addAll(buffer.take(read))
            }
        }
    }
}
```

#### Transcribing Audio

```kotlin
// Stop recording and transcribe
suspend fun stopRecordingAndTranscribe() {
    audioRecorder?.stop()
    audioRecorder?.release()
    
    val audioData = audioBuffer.toByteArray()
    val transcription = RunAnywhere.transcribe(audioData)
    
    _voiceState.value = _voiceState.value.copy(
        transcribedText = transcription
    )
}
```

#### Advanced Transcription with Options

```kotlin
val output = RunAnywhere.transcribeWithOptions(
    audioData = audioBytes,
    options = STTOptions(
        language = "en",
        enableTimestamps = true,
        enablePunctuation = true
    )
)

println("Text: ${output.text}")
println("Confidence: ${output.confidence}")

output.wordTimestamps?.forEach { word ->
    println("[${word.startTime}s - ${word.endTime}s]: ${word.word}")
}
```

#### UI Integration

```kotlin
@Composable
fun STTTab(viewModel: VoiceViewModel, voiceState: VoiceState) {
    Column {
        // Audio level visualization during recording
        if (voiceState.isRecording) {
            AudioLevelIndicator(voiceState.audioLevel)
        }
        
        // Transcribed text display
        Text(voiceState.transcribedText)
        
        // Recording button
        Button(
            onClick = { 
                if (voiceState.isRecording) {
                    viewModel.stopRecordingAndTranscribe()
                } else {
                    viewModel.startRecording()
                }
            }
        ) {
            Icon(if (voiceState.isRecording) Icons.Default.Stop else Icons.Default.Mic)
        }
    }
}
```

---

### Text-to-Speech (TTS)

Convert text to natural-sounding speech using neural TTS models.

#### Loading TTS Voice

```kotlin
suspend fun loadTTSVoice(voiceId: String) {
    try {
        RunAnywhere.loadTTSVoice(voiceId)
        _modelState.value = _modelState.value.copy(
            ttsVoiceId = voiceId,
            isTTSLoaded = true
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error loading TTS: ${e.message}")
    }
}
```

#### Simple Speech (Recommended)

The `speak()` API handles synthesis and playback automatically:

```kotlin
fun speakText(text: String) {
    viewModelScope.launch {
        try {
            _voiceState.value = _voiceState.value.copy(isSpeaking = true)
            
            // Speaks text aloud with default settings
            RunAnywhere.speak(text)
            
            _voiceState.value = _voiceState.value.copy(isSpeaking = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking: ${e.message}")
        }
    }
}
```

#### Advanced Synthesis with Options

```kotlin
val output = RunAnywhere.synthesize(
    text = "Hello, welcome to RunAnywhere!",
    options = TTSOptions(
        rate = 0.9f,    // Slower speech (0.0 - 2.0)
        pitch = 1.1f,   // Slightly higher pitch (0.0 - 2.0)
        volume = 0.8f   // 80% volume (0.0 - 1.0)
    )
)

// Play the audio manually
playAudioTrack(output.audioData, output.format)
```

#### Playing Audio with AudioTrack

```kotlin
fun playAudioTrack(audioData: ByteArray, sampleRate: Int = 22050) {
    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setAudioFormat(
            android.media.AudioFormat.Builder()
                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(audioData.size)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    audioTrack.write(audioData, 0, audioData.size)
    audioTrack.play()
}
```

#### Stopping Speech

```kotlin
fun stopSpeaking() {
    viewModelScope.launch {
        RunAnywhere.stopSpeaking()
        _voiceState.value = _voiceState.value.copy(isSpeaking = false)
    }
}
```

#### UI Integration

```kotlin
@Composable
fun TTSTab(viewModel: VoiceViewModel, voiceState: VoiceState) {
    var textToSpeak by remember { 
        mutableStateOf("Hello! This is a test of text-to-speech.") 
    }
    
    Column {
        // Text input
        OutlinedTextField(
            value = textToSpeak,
            onValueChange = { textToSpeak = it },
            label = { Text("Text to speak") },
            enabled = !voiceState.isSpeaking
        )
        
        // Speaking animation
        if (voiceState.isSpeaking) {
            SpeakingAnimation()
        }
        
        // Control buttons
        Row {
            Button(
                onClick = { viewModel.speakText(textToSpeak) },
                enabled = !voiceState.isSpeaking
            ) {
                Icon(Icons.Default.VolumeUp, "Speak")
                Text("Speak")
            }
            
            if (voiceState.isSpeaking) {
                OutlinedButton(
                    onClick = { viewModel.stopSpeaking() }
                ) {
                    Icon(Icons.Default.Stop, "Stop")
                    Text("Stop")
                }
            }
        }
    }
}
```

---

### Voice Activity Detection (VAD)

Detect when the user starts and stops speaking for responsive voice interfaces.

#### Basic VAD Detection

```kotlin
suspend fun detectVoiceActivity(audioData: ByteArray): Boolean {
    val result = RunAnywhere.detectVoiceActivity(audioData)
    
    _voiceState.value = _voiceState.value.copy(
        confidence = result.confidence
    )
    
    return result.hasSpeech
}
```

#### VAD Configuration

```kotlin
RunAnywhere.configureVAD(VADConfiguration(
    threshold = 0.5f,              // Detection sensitivity (0.0-1.0)
    minSpeechDurationMs = 250,     // Minimum speech duration
    minSilenceDurationMs = 300,    // Silence before speech end
    sampleRate = 16000,            // Audio sample rate
    frameSizeMs = 30               // Frame size for processing
))
```

#### Streaming VAD

Process continuous audio with real-time detection:

```kotlin
fun startVADStream() {
    viewModelScope.launch {
        val audioFlow = createAudioFlow()
        
        RunAnywhere.streamVAD(audioFlow)
            .collect { result ->
                if (result.hasSpeech) {
                    // User is speaking
                    _voiceState.value = _voiceState.value.copy(
                        statusMessage = "Speaking..."
                    )
                } else {
                    // User stopped speaking
                    _voiceState.value = _voiceState.value.copy(
                        statusMessage = "Listening..."
                    )
                }
            }
    }
}
```

#### Calibrate with Ambient Noise

Improve accuracy in noisy environments:

```kotlin
// Record 2 seconds of ambient noise
val ambientAudio = recordAmbientNoise(durationMs = 2000)

// Calibrate VAD
RunAnywhere.calibrateVAD(ambientAudio)
```

---

## Voice Agent Pipeline

The Voice Agent orchestrates the complete conversational pipeline: **VAD → STT → LLM → TTS**.

### Prerequisites

All three models must be loaded:

```kotlin
// Check if ready
val allModelsLoaded = modelState.isLLMLoaded && 
                      modelState.isSTTLoaded && 
                      modelState.isTTSLoaded
```

### Starting Voice Agent

```kotlin
fun startVoiceAgent() {
    viewModelScope.launch {
        if (!allModelsLoaded) {
            _voiceState.value = _voiceState.value.copy(
                statusMessage = "Please load all models first"
            )
            return@launch
        }
        
        // Create audio flow
        val audioFlow = createAudioFlow()
        
        // Start voice session with configuration
        val config = VoiceSessionConfig(
            silenceDuration = 1.5,       // Seconds of silence before processing
            speechThreshold = 0.1f,      // Audio level threshold
            autoPlayTTS = false,         // SDK handles TTS playback
            continuousMode = true        // Auto-resume after each turn
        )
        
        RunAnywhere.streamVoiceSession(audioFlow, config)
            .collect { event ->
                handleVoiceSessionEvent(event)
            }
    }
}
```

### Creating Audio Flow

```kotlin
private fun createAudioFlow(): Flow<ByteArray> = callbackFlow {
    val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        16000, // 16kHz
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
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
```

### Handling Voice Session Events

```kotlin
private fun handleVoiceSessionEvent(event: VoiceSessionEvent) {
    when (event) {
        is VoiceSessionEvent.Started -> {
            _voiceState.value = _voiceState.value.copy(
                statusMessage = "Session started - Listening..."
            )
        }
        
        is VoiceSessionEvent.Listening -> {
            _voiceState.value = _voiceState.value.copy(
                audioLevel = event.audioLevel,
                statusMessage = "Listening..."
            )
        }
        
        is VoiceSessionEvent.SpeechStarted -> {
            _voiceState.value = _voiceState.value.copy(
                statusMessage = "Speech detected..."
            )
        }
        
        is VoiceSessionEvent.Processing -> {
            _voiceState.value = _voiceState.value.copy(
                isProcessing = true,
                statusMessage = "Processing..."
            )
        }
        
        is VoiceSessionEvent.Transcribed -> {
            _voiceState.value = _voiceState.value.copy(
                transcribedText = event.text,
                statusMessage = "You: ${event.text}"
            )
        }
        
        is VoiceSessionEvent.Responded -> {
            _voiceState.value = _voiceState.value.copy(
                responseText = event.text,
                statusMessage = "AI: ${event.text}"
            )
        }
        
        is VoiceSessionEvent.Speaking -> {
            _voiceState.value = _voiceState.value.copy(
                isSpeaking = true,
                statusMessage = "Speaking..."
            )
        }
        
        is VoiceSessionEvent.TurnCompleted -> {
            // Conversation turn complete
            event.audio?.let { playAudio(it) }
            _voiceState.value = _voiceState.value.copy(
                isProcessing = false,
                isSpeaking = false
            )
        }
        
        is VoiceSessionEvent.Error -> {
            _voiceState.value = _voiceState.value.copy(
                statusMessage = "Error: ${event.message}",
                isProcessing = false
            )
        }
        
        is VoiceSessionEvent.Stopped -> {
            _voiceState.value = _voiceState.value.copy(
                statusMessage = "Session ended",
                isProcessing = false
            )
        }
    }
}
```

### Stopping Voice Agent

```kotlin
fun stopVoiceAgent() {
    viewModelScope.launch {
        RunAnywhere.stopVoiceSession()
        _voiceState.value = _voiceState.value.copy(
            statusMessage = "Voice agent stopped"
        )
    }
}
```

---

## Code Examples

### Complete Voice Recording Example

```kotlin
class VoiceRecorderViewModel : AndroidViewModel(application) {
    private var audioRecorder: AudioRecord? = null
    private val audioBuffer = mutableListOf<Byte>()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()
    
    private val _transcription = MutableStateFlow("")
    val transcription = _transcription.asStateFlow()
    
    fun startRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            val bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            audioRecorder?.startRecording()
            _isRecording.value = true
            audioBuffer.clear()
            
            val buffer = ByteArray(bufferSize)
            while (_isRecording.value) {
                val read = audioRecorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    audioBuffer.addAll(buffer.take(read))
                }
            }
        }
    }
    
    fun stopAndTranscribe() {
        viewModelScope.launch {
            _isRecording.value = false
            audioRecorder?.stop()
            audioRecorder?.release()
            audioRecorder = null
            
            val audioData = audioBuffer.toByteArray()
            val text = RunAnywhere.transcribe(audioData)
            _transcription.value = text
        }
    }
}
```

### TTS with Custom Voice Settings

```kotlin
suspend fun speakWithCustomVoice(text: String) {
    val output = RunAnywhere.synthesize(
        text = text,
        options = TTSOptions(
            rate = 0.85f,     // Slower, clearer speech
            pitch = 1.0f,     // Normal pitch
            volume = 1.0f,    // Maximum volume
            audioFormat = AudioFormat.PCM,
            sampleRate = 22050
        )
    )
    
    // Get metadata
    println("Duration: ${output.duration}s")
    println("Voice: ${output.metadata.voice}")
    println("Processing time: ${output.metadata.processingTime}s")
    
    // Play audio
    playAudioTrack(output.audioData, output.format.sampleRate)
}
```

### VAD-Triggered Recording

```kotlin
fun startVADRecording() {
    viewModelScope.launch {
        val audioFlow = createAudioFlow()
        var isRecording = false
        val recordedChunks = mutableListOf<ByteArray>()
        
        RunAnywhere.streamVAD(audioFlow)
            .collect { result ->
                if (result.hasSpeech && !isRecording) {
                    // Speech started
                    isRecording = true
                    recordedChunks.clear()
                    println("Recording started...")
                } else if (!result.hasSpeech && isRecording) {
                    // Speech ended
                    isRecording = false
                    
                    // Transcribe collected audio
                    val audioData = recordedChunks.flatten().toByteArray()
                    val transcription = RunAnywhere.transcribe(audioData)
                    println("Transcription: $transcription")
                }
                
                if (isRecording) {
                    // Collect audio during speech
                    // (Audio chunks would come from the flow)
                }
            }
    }
}
```

---

## Best Practices

### Audio Quality

1. **Use 16kHz sample rate** for STT/VAD (required by Whisper models)
2. **Use mono channel** to reduce processing overhead
3. **Use PCM 16-bit encoding** for best compatibility

```kotlin
val sampleRate = 16000
val channelConfig = AudioFormat.CHANNEL_IN_MONO
val audioFormat = AudioFormat.ENCODING_PCM_16BIT
```

### Model Management

1. **Load models on app startup** for faster first use
2. **Keep models loaded** if used frequently
3. **Unload models** when switching between features

```kotlin
// Efficient model management
init {
    viewModelScope.launch {
        delay(2000) // Wait for SDK init
        tryAutoLoadModels()
    }
}
```

### Memory Management

1. **Clear audio buffers** after use
2. **Release AudioRecord/AudioTrack** when done
3. **Cancel coroutines** in onCleared()

```kotlin
override fun onCleared() {
    super.onCleared()
    audioRecorder?.release()
    audioTrack?.release()
    audioBuffer.clear()
}
```

### Error Handling

1. **Always wrap SDK calls** in try-catch
2. **Provide user feedback** on errors
3. **Log errors** for debugging

```kotlin
try {
    val text = RunAnywhere.transcribe(audioData)
    _voiceState.value = _voiceState.value.copy(transcribedText = text)
} catch (e: Exception) {
    Log.e(TAG, "Transcription failed: ${e.message}")
    _voiceState.value = _voiceState.value.copy(
        statusMessage = "Error: ${e.message}"
    )
}
```

### Performance Optimization

1. **Use streaming APIs** for responsive UX
2. **Process audio in background thread** (Dispatchers.IO)
3. **Minimize UI updates** during streaming

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    RunAnywhere.generateStream(text).collect { token ->
        withContext(Dispatchers.Main) {
            updateUI(token)
        }
    }
}
```

---

## Troubleshooting

### Common Issues

#### "Model not loaded" Error

**Problem:** Trying to use STT/TTS before model is loaded

**Solution:**
```kotlin
if (!modelState.isSTTLoaded) {
    _voiceState.value = _voiceState.value.copy(
        statusMessage = "Please load an STT model first"
    )
    return
}
```

#### No Audio Recorded

**Problem:** AudioRecord not capturing audio

**Solution:** Check permissions and buffer size
```kotlin
// Check permission
if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
    != PackageManager.PERMISSION_GRANTED) {
    requestPermission()
}

// Use proper buffer size
val bufferSize = AudioRecord.getMinBufferSize(
    16000,
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT
) * 2 // Double for safety
```

#### Poor Transcription Quality

**Problem:** Low accuracy or missing words

**Solutions:**
1. Use higher sample rate (16kHz minimum)
2. Reduce background noise
3. Speak clearly and at normal pace
4. Use a larger STT model

#### TTS Not Playing

**Problem:** Synthesize succeeds but no audio output

**Solution:** Use `speak()` instead of `synthesize()` for automatic playback
```kotlin
// Easier approach
RunAnywhere.speak(text)

// Manual playback approach
val output = RunAnywhere.synthesize(text, options)
playAudioTrack(output.audioData, output.format.sampleRate)
```

### Debug Logging

Enable detailed logging:

```kotlin
// In MyApplication
RunAnywhere.initialize(
    context = this,
    apiKey = "dev",
    environment = SDKEnvironment.DEVELOPMENT // Enables debug logs
)
```

Check logs:
```bash
adb logcat | grep -E "RunAnywhere|VoiceViewModel"
```

---

## Additional Resources

- [RunAnywhere SDK Documentation](https://docs.runanywhere.ai/)
- [Kotlin SDK Reference](https://docs.runanywhere.ai/kotlin/introduction)
- [STT API Reference](https://docs.runanywhere.ai/kotlin/stt/transcribe)
- [TTS API Reference](https://docs.runanywhere.ai/kotlin/tts/synthesize)
- [VAD API Reference](https://docs.runanywhere.ai/kotlin/vad)
- [Voice Agent Reference](https://docs.runanywhere.ai/kotlin/voice-agent)

---

## Summary

This implementation provides:

✅ **Complete Voice Features**
- Individual STT, TTS, VAD components
- Full Voice Agent pipeline
- Model management UI
- Permission handling

✅ **Production-Ready Code**
- Proper error handling
- Memory management
- State management with Flows
- Clean MVVM architecture

✅ **Great User Experience**
- Visual feedback (animations, progress)
- Tab-based navigation
- Real-time status updates
- Responsive UI

**Next Steps:**
1. Add STT/TTS model URLs in `MyApplication.kt`
2. Download models via the Models tab
3. Test individual components first
4. Try the full Voice Agent pipeline

Happy coding! 🚀
