package com.runanywhere.startup_hackathon20.ai

import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for analyzing medicine information using AI
 * Takes OCR-extracted text and generates comprehensive medicine analysis
 */
class MedicineAnalysisService {

    companion object {
        private const val TAG = "MedicineAnalysisService"
    }

    /**
     * Analyze medicine text using AI to extract comprehensive information
     */
    suspend fun analyzeMedicine(ocrText: String): MedicineAnalysis = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting medicine analysis for OCR text: ${ocrText.take(100)}...")
            
            val prompt = buildAnalysisPrompt(ocrText)
            val response = RunAnywhere.generate(prompt)
            
            Log.d(TAG, "AI Response received: ${response.take(200)}...")
            
            // Parse AI response into structured data
            parseAnalysisResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing medicine: ${e.message}", e)
            throw MedicineAnalysisException("Failed to analyze medicine: ${e.message}", e)
        }
    }

    /**
     * Build comprehensive prompt for AI to analyze medicine
     */
    private fun buildAnalysisPrompt(ocrText: String): String {
        return """
You are a medical information assistant. Analyze the following text extracted from a medicine label/package and provide comprehensive information.

OCR TEXT:
$ocrText

Please provide a detailed analysis in the following EXACT format (use the exact headers shown):

MEDICINE NAME:
[Extract the medicine name]

CATEGORY:
[Medicine category/class, e.g., Antibiotic, Pain Reliever, Antihistamine, etc.]

USE CASES:
- [Use case 1]
- [Use case 2]
- [Use case 3]

HOW IT WORKS:
[Brief explanation of the mechanism of action]

DOSAGE GUIDANCE:
[General dosage information - emphasize this is general guidance and patient should follow doctor's prescription]

PRECAUTIONS:
- [Precaution 1]
- [Precaution 2]
- [Precaution 3]

COMMON SIDE EFFECTS:
- [Side effect 1]
- [Side effect 2]
- [Side effect 3]

SERIOUS WARNINGS:
- [Warning 1]
- [Warning 2]
- [Warning 3]

DRUG INTERACTIONS:
- [Interaction 1]
- [Interaction 2]
- [Interaction 3]

IMPORTANT RULES:
1. If you cannot identify the medicine clearly, state "Unable to identify" for MEDICINE NAME
2. Provide accurate, evidence-based medical information
3. For DOSAGE GUIDANCE, always emphasize following doctor's prescription
4. If information is not available from the text, provide general information for that medicine if you recognize it
5. Be concise but comprehensive
6. Use bullet points (-) for lists

Provide ONLY the analysis in the format above, nothing else.
""".trimIndent()
    }

    /**
     * Parse AI response into structured MedicineAnalysis object
     */
    private fun parseAnalysisResponse(response: String): MedicineAnalysis {
        var medicineName = ""
        var category = ""
        val useCases = mutableListOf<String>()
        var howItWorks = ""
        var dosageGuidance = ""
        val precautions = mutableListOf<String>()
        val commonSideEffects = mutableListOf<String>()
        val seriousWarnings = mutableListOf<String>()
        val drugInteractions = mutableListOf<String>()

        var currentSection = ""
        val lines = response.lines()

        for (line in lines) {
            val trimmed = line.trim()
            
            if (trimmed.isEmpty()) continue

            // Detect section headers
            when {
                trimmed.matches(Regex("MEDICINE NAME:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "MEDICINE_NAME"
                }
                trimmed.matches(Regex("CATEGORY:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "CATEGORY"
                }
                trimmed.matches(Regex("USE CASES:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "USE_CASES"
                }
                trimmed.matches(Regex("HOW IT WORKS:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "HOW_IT_WORKS"
                }
                trimmed.matches(Regex("DOSAGE GUIDANCE:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "DOSAGE"
                }
                trimmed.matches(Regex("PRECAUTIONS:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "PRECAUTIONS"
                }
                trimmed.matches(Regex("COMMON SIDE EFFECTS:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "SIDE_EFFECTS"
                }
                trimmed.matches(Regex("SERIOUS WARNINGS:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "WARNINGS"
                }
                trimmed.matches(Regex("DRUG INTERACTIONS:?", RegexOption.IGNORE_CASE)) -> {
                    currentSection = "INTERACTIONS"
                }
                // Process content based on current section
                else -> {
                    when (currentSection) {
                        "MEDICINE_NAME" -> {
                            if (medicineName.isEmpty() && trimmed.isNotBlank()) {
                                medicineName = trimmed.removePrefix("[").removeSuffix("]")
                            }
                        }
                        "CATEGORY" -> {
                            if (category.isEmpty() && trimmed.isNotBlank()) {
                                category = trimmed.removePrefix("[").removeSuffix("]")
                            }
                        }
                        "USE_CASES" -> {
                            if (trimmed.startsWith("-") || trimmed.startsWith("•")) {
                                val useCase = trimmed.removePrefix("-").removePrefix("•").trim()
                                    .removePrefix("[").removeSuffix("]")
                                if (useCase.isNotBlank()) {
                                    useCases.add(useCase)
                                }
                            }
                        }
                        "HOW_IT_WORKS" -> {
                            if (trimmed.isNotBlank()) {
                                howItWorks += if (howItWorks.isEmpty()) trimmed else " $trimmed"
                            }
                        }
                        "DOSAGE" -> {
                            if (trimmed.isNotBlank()) {
                                dosageGuidance += if (dosageGuidance.isEmpty()) trimmed else " $trimmed"
                            }
                        }
                        "PRECAUTIONS" -> {
                            if (trimmed.startsWith("-") || trimmed.startsWith("•")) {
                                val precaution = trimmed.removePrefix("-").removePrefix("•").trim()
                                    .removePrefix("[").removeSuffix("]")
                                if (precaution.isNotBlank()) {
                                    precautions.add(precaution)
                                }
                            }
                        }
                        "SIDE_EFFECTS" -> {
                            if (trimmed.startsWith("-") || trimmed.startsWith("•")) {
                                val sideEffect = trimmed.removePrefix("-").removePrefix("•").trim()
                                    .removePrefix("[").removeSuffix("]")
                                if (sideEffect.isNotBlank()) {
                                    commonSideEffects.add(sideEffect)
                                }
                            }
                        }
                        "WARNINGS" -> {
                            if (trimmed.startsWith("-") || trimmed.startsWith("•")) {
                                val warning = trimmed.removePrefix("-").removePrefix("•").trim()
                                    .removePrefix("[").removeSuffix("]")
                                if (warning.isNotBlank()) {
                                    seriousWarnings.add(warning)
                                }
                            }
                        }
                        "INTERACTIONS" -> {
                            if (trimmed.startsWith("-") || trimmed.startsWith("•")) {
                                val interaction = trimmed.removePrefix("-").removePrefix("•").trim()
                                    .removePrefix("[").removeSuffix("]")
                                if (interaction.isNotBlank()) {
                                    drugInteractions.add(interaction)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clean up extracted values
        howItWorks = howItWorks.removePrefix("[").removeSuffix("]").trim()
        dosageGuidance = dosageGuidance.removePrefix("[").removeSuffix("]").trim()

        return MedicineAnalysis(
            medicineName = medicineName.ifEmpty { "Unknown Medicine" },
            category = category.ifEmpty { "Not specified" },
            useCases = useCases.ifEmpty { listOf("Information not available") },
            howItWorks = howItWorks.ifEmpty { "Information not available" },
            dosageGuidance = dosageGuidance.ifEmpty { "Please consult your healthcare provider for dosage information" },
            precautions = precautions.ifEmpty { listOf("Consult your healthcare provider") },
            commonSideEffects = commonSideEffects.ifEmpty { listOf("Information not available") },
            seriousWarnings = seriousWarnings.ifEmpty { listOf("Consult healthcare provider immediately if you experience any severe reactions") },
            drugInteractions = drugInteractions.ifEmpty { listOf("Inform your doctor about all medications you are taking") }
        )
    }
}

/**
 * Custom exception for medicine analysis errors
 */
class MedicineAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
