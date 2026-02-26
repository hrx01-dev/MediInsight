package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.runanywhere.startup_hackathon20.ai.MedicineAnalysis
import com.runanywhere.startup_hackathon20.ai.MedicineAnalysisService
import com.runanywhere.startup_hackathon20.ai.MedicineAnalysisState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for OCR to AI Medicine Analysis Pipeline
 * Orchestrates the flow: Image → OCR → AI Analysis → Results
 */
class MedicineAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MedicineAnalysisVM"
    }

    private val analysisService = MedicineAnalysisService()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // State management
    private val _state = MutableStateFlow<MedicineAnalysisState>(MedicineAnalysisState.Idle)
    val state: StateFlow<MedicineAnalysisState> = _state.asStateFlow()

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()

    private val _analysis = MutableStateFlow<MedicineAnalysis?>(null)
    val analysis: StateFlow<MedicineAnalysis?> = _analysis.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()

    /**
     * Complete pipeline: Process image → OCR → AI Analysis
     */
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _state.value = MedicineAnalysisState.ScanningImage
                _progress.value = "Scanning image..."

                // Step 1: OCR - Extract text from image
                val ocrText = performOCR(bitmap)
                
                if (ocrText.isBlank()) {
                    _error.value = "No text detected in image. Please ensure the image is clear and contains readable text."
                    _state.value = MedicineAnalysisState.Error("No text detected")
                    _isLoading.value = false
                    _progress.value = ""
                    return@launch
                }

                Log.d(TAG, "OCR completed. Extracted text length: ${ocrText.length}")
                _extractedText.value = ocrText
                _state.value = MedicineAnalysisState.OcrCompleted(ocrText)
                _progress.value = "Text extracted. Analyzing with AI..."

                // Step 2: AI Analysis - Analyze extracted text
                val medicineAnalysis = analysisService.analyzeMedicine(ocrText)
                
                Log.d(TAG, "AI analysis completed: ${medicineAnalysis.medicineName}")
                _analysis.value = medicineAnalysis
                _state.value = MedicineAnalysisState.AnalysisCompleted(medicineAnalysis)
                _progress.value = "Analysis complete!"

            } catch (e: Exception) {
                Log.e(TAG, "Error in processing pipeline: ${e.message}", e)
                _error.value = "Analysis failed: ${e.message}"
                _state.value = MedicineAnalysisState.Error(e.message ?: "Unknown error")
                _progress.value = ""
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Perform OCR on the provided bitmap
     */
    private suspend fun performOCR(bitmap: Bitmap): String {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        Log.d(TAG, "OCR Success: ${text.take(100)}...")
                        continuation.resume(text) {}
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "OCR Failed: ${exception.message}", exception)
                        continuation.resume("") {}
                    }
            } catch (e: Exception) {
                Log.e(TAG, "OCR Exception: ${e.message}", e)
                continuation.resume("") {}
            }
        }
    }

    /**
     * Process text directly (skip OCR if text is already available)
     */
    fun processText(text: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _extractedText.value = text
                _state.value = MedicineAnalysisState.OcrCompleted(text)
                _progress.value = "Analyzing with AI..."

                val medicineAnalysis = analysisService.analyzeMedicine(text)
                
                _analysis.value = medicineAnalysis
                _state.value = MedicineAnalysisState.AnalysisCompleted(medicineAnalysis)
                _progress.value = "Analysis complete!"

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing text: ${e.message}", e)
                _error.value = "Analysis failed: ${e.message}"
                _state.value = MedicineAnalysisState.Error(e.message ?: "Unknown error")
                _progress.value = ""
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset to initial state
     */
    fun reset() {
        _state.value = MedicineAnalysisState.Idle
        _extractedText.value = ""
        _analysis.value = null
        _isLoading.value = false
        _error.value = null
        _progress.value = ""
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}
