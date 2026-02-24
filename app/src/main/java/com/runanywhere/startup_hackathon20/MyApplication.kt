package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SDK asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeSDK()
        }
    }

    private suspend fun initializeSDK() {
        try {
            Log.i("MyApp", "Starting SDK initialization...")

            // Step 1: Initialize SDK
            RunAnywhere.initialize(
                context = this@MyApplication,
                apiKey = "dev",  // Any string works in dev mode
                environment = SDKEnvironment.DEVELOPMENT
            )
            Log.i("MyApp", "SDK core initialized")

            // Step 2: Register Service Providers
            // Register LLM Service Provider (LlamaCpp)
            LlamaCppServiceProvider.register()
            Log.i("MyApp", "LLM Service Provider registered")
            
            // Note: STT/TTS providers will be auto-registered when models are loaded
            // The RunAnywhere SDK handles STT/TTS backend registration automatically

            // Step 3: Register Models
            registerModels()
            Log.i("MyApp", "Models registered")

            // Step 4: Scan for previously downloaded models
            RunAnywhere.scanForDownloadedModels()
            Log.i("MyApp", "Model scan completed")

            Log.i("MyApp", "SDK initialized successfully")

        } catch (e: Exception) {
            Log.e("MyApp", "SDK initialization failed: ${e.message}", e)
        }
    }

    private suspend fun registerModels() {
        try {
            // LLM Model - Medium-sized model for better quality (374 MB)
            addModelFromURL(
                url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
                name = "Qwen 2.5 0.5B Instruct Q6_K",
                type = "LLM"
            )
            
            // Add STT and TTS model registration here when you have the model URLs
            // Example STT model registration:
            // addModelFromURL(
            //     url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.en.tar.gz",
            //     name = "Whisper Tiny EN",
            //     type = "STT"
            // )
            
            // Example TTS model registration:
            // addModelFromURL(
            //     url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx",
            //     name = "Piper TTS EN US",
            //     type = "TTS"
            // )
            
            Log.i("MyApp", "LLM models registered. Add STT/TTS model URLs to enable voice features.")
            
        } catch (e: Exception) {
            Log.e("MyApp", "Model registration failed: ${e.message}", e)
        }
    }
}
