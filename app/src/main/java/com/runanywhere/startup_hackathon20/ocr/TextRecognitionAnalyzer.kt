package com.runanywhere.startup_hackathon20.ocr

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * ImageAnalysis analyzer for real-time text recognition using ML Kit
 * Processes camera frames and extracts text from medicine labels/packages
 */
class TextRecognitionAnalyzer(
    private val onTextDetected: (String) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isProcessing = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isProcessing) {
            isProcessing = true
            
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text
                    if (detectedText.isNotBlank()) {
                        Log.d("TextRecognition", "Detected: $detectedText")
                        onTextDetected(detectedText)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("TextRecognition", "Error: ${exception.message}")
                    onError(exception)
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun close() {
        recognizer.close()
    }
}
