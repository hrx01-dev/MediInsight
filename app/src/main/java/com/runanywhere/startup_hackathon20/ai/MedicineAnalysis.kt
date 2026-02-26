package com.runanywhere.startup_hackathon20.ai

/**
 * Comprehensive medicine analysis data model
 * Contains all information extracted and analyzed from OCR text using AI
 */
data class MedicineAnalysis(
    val medicineName: String = "",
    val category: String = "",
    val useCases: List<String> = emptyList(),
    val howItWorks: String = "",
    val dosageGuidance: String = "",
    val precautions: List<String> = emptyList(),
    val commonSideEffects: List<String> = emptyList(),
    val seriousWarnings: List<String> = emptyList(),
    val drugInteractions: List<String> = emptyList(),
    val medicalDisclaimer: String = DEFAULT_DISCLAIMER
) {
    companion object {
        const val DEFAULT_DISCLAIMER = 
            "⚠️ MEDICAL DISCLAIMER: This information is for educational purposes only and should not replace professional medical advice. " +
            "Always consult your healthcare provider before starting, stopping, or changing any medication. " +
            "In case of medical emergency, contact emergency services immediately."
    }
}

/**
 * State for medicine analysis flow
 */
sealed class MedicineAnalysisState {
    object Idle : MedicineAnalysisState()
    object ScanningImage : MedicineAnalysisState()
    data class OcrCompleted(val extractedText: String) : MedicineAnalysisState()
    object AnalyzingWithAI : MedicineAnalysisState()
    data class AnalysisCompleted(val analysis: MedicineAnalysis) : MedicineAnalysisState()
    data class Error(val message: String) : MedicineAnalysisState()
}
