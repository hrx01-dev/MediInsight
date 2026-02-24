package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.startup_hackathon20.ai.MedicineTextParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for passing scanned OCR text from Scanner to AddMedicine screen
 * Handles AI-powered text parsing
 */
class SharedMedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val parser = MedicineTextParser()

    // Scanned text from OCR
    private val _scannedText = MutableStateFlow("")
    val scannedText: StateFlow<String> = _scannedText.asStateFlow()

    // Parsed medicine data
    private val _parsedMedicine = MutableStateFlow<MedicineTextParser.ParsedMedicine?>(null)
    val parsedMedicine: StateFlow<MedicineTextParser.ParsedMedicine?> = _parsedMedicine.asStateFlow()

    // Parsing status
    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parseError = MutableStateFlow<String?>(null)
    val parseError: StateFlow<String?> = _parseError.asStateFlow()

    /**
     * Set scanned text and trigger AI parsing
     */
    fun setScannedText(text: String) {
        _scannedText.value = text
        if (text.isNotBlank()) {
            parseTextWithAI(text)
        }
    }

    /**
     * Parse scanned text using AI
     */
    private fun parseTextWithAI(text: String) {
        viewModelScope.launch {
            try {
                _isParsing.value = true
                _parseError.value = null
                
                val parsed = parser.parseWithAI(text)
                _parsedMedicine.value = parsed
                
                _isParsing.value = false
            } catch (e: Exception) {
                _parseError.value = "Failed to parse text: ${e.message}"
                _isParsing.value = false
                
                // Still provide some parsed data even on error
                try {
                    val fallback = MedicineTextParser.ParsedMedicine(
                        instructions = text // Put full text in instructions as fallback
                    )
                    _parsedMedicine.value = fallback
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Clear scanned data
     */
    fun clearScannedData() {
        _scannedText.value = ""
        _parsedMedicine.value = null
        _parseError.value = null
    }

    /**
     * Manual parse (if user wants to re-parse)
     */
    fun reParse() {
        if (_scannedText.value.isNotBlank()) {
            parseTextWithAI(_scannedText.value)
        }
    }
}
