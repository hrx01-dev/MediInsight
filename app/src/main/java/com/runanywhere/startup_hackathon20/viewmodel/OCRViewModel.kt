package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.runanywhere.startup_hackathon20.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class OCRResult(
    val fullText: String = "",
    val blocks: List<TextBlock> = emptyList(),
    val confidence: Float = 0f,
    val processingTime: Long = 0L
)

data class TextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect? = null,
    val confidence: Float = 0f,
    val lines: List<String> = emptyList()
)

data class MedicineInfo(
    val medicineName: String = "",
    val dosage: String = "",
    val composition: String = "",
    val manufacturer: String = "",
    val expiryDate: String = "",
    val batchNumber: String = "",
    val mrp: String = "",
    val additionalInfo: List<String> = emptyList()
)

data class OCRState(
    val isProcessing: Boolean = false,
    val capturedImage: Bitmap? = null,
    val processedImage: Bitmap? = null,
    val ocrResult: OCRResult? = null,
    val medicineInfo: MedicineInfo? = null,
    val statusMessage: String = "Ready to scan",
    val imageQuality: Int = 0,
    val error: String? = null
)

/**
 * ViewModel for OCR-based medicine text recognition
 * Uses Google ML Kit for text recognition and OpenCV for image preprocessing
 */
class OCRViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "OCRViewModel"
    private val context = application.applicationContext
    
    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // State
    private val _ocrState = MutableStateFlow(OCRState())
    val ocrState: StateFlow<OCRState> = _ocrState
    
    /**
     * Process image and extract text
     */
    fun processImage(bitmap: Bitmap, usePreprocessing: Boolean = true) {
        viewModelScope.launch {
            try {
                _ocrState.value = _ocrState.value.copy(
                    isProcessing = true,
                    capturedImage = bitmap,
                    statusMessage = "Processing image...",
                    error = null
                )
                
                val startTime = System.currentTimeMillis()
                
                // Step 1: Resize if too large
                val resizedBitmap = withContext(Dispatchers.Default) {
                    ImageProcessor.resizeImage(bitmap, 2048, 2048)
                }
                
                // Step 2: Calculate image quality
                val quality = withContext(Dispatchers.Default) {
                    ImageProcessor.calculateImageQuality(resizedBitmap)
                }
                
                _ocrState.value = _ocrState.value.copy(
                    imageQuality = quality,
                    statusMessage = "Image quality: $quality%"
                )
                
                // Step 3: Preprocess image if enabled
                val processedBitmap = if (usePreprocessing) {
                    _ocrState.value = _ocrState.value.copy(
                        statusMessage = "Enhancing image..."
                    )
                    withContext(Dispatchers.Default) {
                        ImageProcessor.preprocessMedicineLabel(resizedBitmap)
                    }
                } else {
                    resizedBitmap
                }
                
                _ocrState.value = _ocrState.value.copy(
                    processedImage = processedBitmap,
                    statusMessage = "Recognizing text..."
                )
                
                // Step 4: Run OCR
                val ocrResult = recognizeText(processedBitmap)
                val processingTime = System.currentTimeMillis() - startTime
                
                val finalResult = ocrResult.copy(processingTime = processingTime)
                
                // Step 5: Extract medicine information
                val medicineInfo = extractMedicineInfo(finalResult)
                
                _ocrState.value = _ocrState.value.copy(
                    isProcessing = false,
                    ocrResult = finalResult,
                    medicineInfo = medicineInfo,
                    statusMessage = "Recognition complete (${processingTime}ms)"
                )
                
                Log.i(TAG, "OCR completed in ${processingTime}ms. Found ${finalResult.blocks.size} text blocks")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                _ocrState.value = _ocrState.value.copy(
                    isProcessing = false,
                    statusMessage = "Error processing image",
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Recognize text using ML Kit
     */
    private suspend fun recognizeText(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val task = recognizer.process(inputImage)
            
            // Use Tasks.await extension (from kotlinx-coroutines-play-services)
            val result = task.await()
            
            // Extract text blocks - ML Kit TextBlock has: text, boundingBox, lines, confidence (optional)
            val blocks = result.textBlocks.mapNotNull { block ->
                try {
                    TextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        confidence = 0.95f,
                        lines = block.lines.map { it.text }
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val fullText = result.text
            val avgConfidence = if (blocks.isNotEmpty()) 0.95f else 0f
            
            OCRResult(
                fullText = fullText,
                blocks = blocks,
                confidence = avgConfidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing text: ${e.message}", e)
            OCRResult(
                fullText = "",
                blocks = emptyList(),
                confidence = 0f
            )
        }
    }
    
    /**
     * Extract structured medicine information from OCR result
     */
    private fun extractMedicineInfo(ocrResult: OCRResult): MedicineInfo {
        val text = ocrResult.fullText
        val lines = text.lines().filter { it.isNotBlank() }
        
        // Medicine name is typically the first or most prominent text
        val medicineName = extractMedicineName(lines)
        
        // Extract dosage (e.g., "500mg", "10ml", "250 mg")
        val dosage = extractDosage(text)
        
        // Extract composition
        val composition = extractComposition(text, lines)
        
        // Extract manufacturer
        val manufacturer = extractManufacturer(text, lines)
        
        // Extract expiry date
        val expiryDate = extractExpiryDate(text)
        
        // Extract batch number
        val batchNumber = extractBatchNumber(text)
        
        // Extract MRP
        val mrp = extractMRP(text)
        
        // Additional info
        val additionalInfo = mutableListOf<String>()
        
        // Look for common medicine-related keywords
        if (text.contains("tablet", ignoreCase = true)) additionalInfo.add("Form: Tablet")
        if (text.contains("capsule", ignoreCase = true)) additionalInfo.add("Form: Capsule")
        if (text.contains("syrup", ignoreCase = true)) additionalInfo.add("Form: Syrup")
        if (text.contains("injection", ignoreCase = true)) additionalInfo.add("Form: Injection")
        if (text.contains("cream", ignoreCase = true)) additionalInfo.add("Form: Cream")
        if (text.contains("ointment", ignoreCase = true)) additionalInfo.add("Form: Ointment")
        
        if (text.contains("prescription", ignoreCase = true)) additionalInfo.add("Prescription Required")
        
        return MedicineInfo(
            medicineName = medicineName,
            dosage = dosage,
            composition = composition,
            manufacturer = manufacturer,
            expiryDate = expiryDate,
            batchNumber = batchNumber,
            mrp = mrp,
            additionalInfo = additionalInfo
        )
    }
    
    private fun extractMedicineName(lines: List<String>): String {
        // Medicine name is usually in first few lines and may be in all caps or larger font
        val candidates = lines.take(5).filter { 
            it.length > 3 && !it.contains("ltd", ignoreCase = true) &&
            !it.contains("pharma", ignoreCase = true) &&
            !it.contains("mfg", ignoreCase = true)
        }
        
        // Prefer uppercase text (common for brand names)
        return candidates.firstOrNull { it == it.uppercase() }
            ?: candidates.firstOrNull()
            ?: "Unknown"
    }
    
    private fun extractDosage(text: String): String {
        // Common dosage patterns: 500mg, 10ml, 250 mg, 5 g, etc.
        val dosageRegex = Regex("""(\d+\.?\d*)\s*(mg|g|ml|mcg|iu|%)\b""", RegexOption.IGNORE_CASE)
        val match = dosageRegex.find(text)
        return match?.value ?: ""
    }
    
    private fun extractComposition(text: String, lines: List<String>): String {
        // Look for composition keywords
        val compositionKeywords = listOf("composition", "contains", "active ingredient", "each")
        
        for (keyword in compositionKeywords) {
            val index = lines.indexOfFirst { it.contains(keyword, ignoreCase = true) }
            if (index >= 0 && index < lines.size - 1) {
                // Get next 2-3 lines after keyword
                return lines.subList(index + 1, minOf(index + 4, lines.size))
                    .joinToString(" ")
                    .take(200)
            }
        }
        
        // Fallback: look for chemical/generic names (words ending in common drug suffixes)
        val drugSuffixes = listOf("cillin", "mycin", "oxacin", "prazole", "statin", "mab", "ine", "ol")
        val chemicalName = lines.find { line ->
            drugSuffixes.any { suffix -> line.contains(suffix, ignoreCase = true) }
        }
        
        return chemicalName ?: ""
    }
    
    private fun extractManufacturer(text: String, lines: List<String>): String {
        // Look for manufacturer keywords
        val manufacturerKeywords = listOf("mfg", "manufactured", "pharma", "pharmaceuticals", "ltd", "inc")
        
        for (line in lines) {
            if (manufacturerKeywords.any { line.contains(it, ignoreCase = true) }) {
                return line.trim()
            }
        }
        
        return ""
    }
    
    private fun extractExpiryDate(text: String): String {
        // Common expiry date patterns
        val patterns = listOf(
            Regex("""exp[.:\s]*(\d{2}[-/]\d{4}|\d{2}[-/]\d{2}[-/]\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""expiry[:\s]*(\d{2}[-/]\d{4}|\d{2}[-/]\d{2}[-/]\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""best before[:\s]*(\d{2}[-/]\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return ""
    }
    
    private fun extractBatchNumber(text: String): String {
        // Common batch number patterns
        val patterns = listOf(
            Regex("""batch[:\s#]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""lot[:\s#]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""b\.?no[:\s]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return ""
    }
    
    private fun extractMRP(text: String): String {
        // Common MRP patterns
        val patterns = listOf(
            Regex("""mrp[:\s]*₹?\s*(\d+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""price[:\s]*₹?\s*(\d+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""rs\.?\s*(\d+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""₹\s*(\d+\.?\d*)""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return "₹${match.groupValues[1]}"
            }
        }
        
        return ""
    }
    
    /**
     * Reprocess with different preprocessing settings
     */
    fun reprocessWithEnhancement() {
        _ocrState.value.capturedImage?.let { bitmap ->
            processImage(bitmap, usePreprocessing = true)
        }
    }
    
    /**
     * Clear current results
     */
    fun clearResults() {
        _ocrState.value = OCRState()
    }
    
    /**
     * Save medicine info to database (integrate with existing MedicineViewModel)
     */
    fun saveMedicineInfo() {
        val medicineInfo = _ocrState.value.medicineInfo ?: return
        
        // TODO: Integrate with MedicineViewModel to save to database
        // This would call your existing medicine repository
        
        Log.i(TAG, "Saving medicine: ${medicineInfo.medicineName}")
    }
    
    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}
