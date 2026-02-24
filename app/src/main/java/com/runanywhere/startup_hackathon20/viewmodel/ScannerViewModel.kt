package com.runanywhere.startup_hackathon20.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing OCR scanner state
 * Handles detected text, scanning status, and error states
 */
class ScannerViewModel : ViewModel() {

    private val _detectedText = MutableStateFlow("")
    val detectedText: StateFlow<String> = _detectedText.asStateFlow()

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _scannedHistory = MutableStateFlow<List<String>>(emptyList())
    val scannedHistory: StateFlow<List<String>> = _scannedHistory.asStateFlow()

    fun updateDetectedText(text: String) {
        _detectedText.value = text
        // Add to history if not empty and not duplicate of last entry
        if (text.isNotBlank() && text != _scannedHistory.value.lastOrNull()) {
            _scannedHistory.value = _scannedHistory.value + text
        }
    }

    fun toggleScanning() {
        _isScanning.value = !_isScanning.value
    }

    fun pauseScanning() {
        _isScanning.value = false
    }

    fun resumeScanning() {
        _isScanning.value = true
        _detectedText.value = ""
    }

    fun setError(errorMessage: String) {
        _error.value = errorMessage
    }

    fun clearError() {
        _error.value = null
    }

    fun clearHistory() {
        _scannedHistory.value = emptyList()
        _detectedText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        _detectedText.value = ""
        _scannedHistory.value = emptyList()
    }
}
