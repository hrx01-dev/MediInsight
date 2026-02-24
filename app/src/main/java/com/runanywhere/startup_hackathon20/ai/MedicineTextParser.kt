package com.runanywhere.startup_hackathon20.ai

import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI-powered medicine text parser
 * Uses RunAnywhere LLM to extract structured medicine information from OCR text
 */
class MedicineTextParser {

    companion object {
        private const val TAG = "MedicineTextParser"
    }

    /**
     * Parsed medicine data
     */
    data class ParsedMedicine(
        val name: String = "",
        val dosage: String = "",
        val frequency: String = "",
        val time: String = "",
        val duration: String = "",
        val quantity: String = "",
        val instructions: String = ""
    )

    /**
     * Parse medicine text using AI
     * Extracts structured information from unstructured OCR text
     */
    suspend fun parseWithAI(scannedText: String): ParsedMedicine = withContext(Dispatchers.IO) {
        try {
            val prompt = buildParsingPrompt(scannedText)
            val response = RunAnywhere.generate(prompt)
            
            Log.d(TAG, "AI Response: $response")
            
            // Parse AI response into structured data
            parseAIResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing with AI: ${e.message}", e)
            // Fallback to rule-based parsing
            parseWithRules(scannedText)
        }
    }

    /**
     * Build prompt for AI to parse medicine information
     */
    private fun buildParsingPrompt(text: String): String {
        return """
Extract medicine information from the following text. Provide the information in this exact format:

NAME: [medicine name]
DOSAGE: [dosage amount and unit, e.g., "100mg" or "5ml"]
FREQUENCY: [how often to take, e.g., "Once daily", "Twice daily", "Every 8 hours"]
TIME: [when to take, e.g., "Morning", "With meals", "Before bed"]
DURATION: [how long, e.g., "7 days", "2 weeks", "As needed"]
QUANTITY: [number of pills/tablets, e.g., "30 tablets", "100ml"]
INSTRUCTIONS: [special instructions, e.g., "Take with food", "Do not crush"]

If any information is not available in the text, write "Not specified".

Text to analyze:
$text

Provide only the extracted information in the format above, nothing else.
""".trimIndent()
    }

    /**
     * Parse AI response into structured data
     */
    private fun parseAIResponse(response: String): ParsedMedicine {
        val lines = response.lines()
        var name = ""
        var dosage = ""
        var frequency = ""
        var time = ""
        var duration = ""
        var quantity = ""
        var instructions = ""

        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("NAME:", ignoreCase = true) -> {
                    name = extractValue(trimmed)
                }
                trimmed.startsWith("DOSAGE:", ignoreCase = true) -> {
                    dosage = extractValue(trimmed)
                }
                trimmed.startsWith("FREQUENCY:", ignoreCase = true) -> {
                    frequency = extractValue(trimmed)
                }
                trimmed.startsWith("TIME:", ignoreCase = true) -> {
                    time = extractValue(trimmed)
                }
                trimmed.startsWith("DURATION:", ignoreCase = true) -> {
                    duration = extractValue(trimmed)
                }
                trimmed.startsWith("QUANTITY:", ignoreCase = true) -> {
                    quantity = extractValue(trimmed)
                }
                trimmed.startsWith("INSTRUCTIONS:", ignoreCase = true) -> {
                    instructions = extractValue(trimmed)
                }
            }
        }

        return ParsedMedicine(
            name = cleanValue(name),
            dosage = cleanValue(dosage),
            frequency = cleanValue(frequency),
            time = cleanValue(time),
            duration = cleanValue(duration),
            quantity = cleanValue(quantity),
            instructions = cleanValue(instructions)
        )
    }

    /**
     * Extract value after the label (e.g., "NAME: Aspirin" -> "Aspirin")
     */
    private fun extractValue(line: String): String {
        val parts = line.split(":", limit = 2)
        return if (parts.size == 2) parts[1].trim() else ""
    }

    /**
     * Clean extracted value
     */
    private fun cleanValue(value: String): String {
        val cleaned = value.trim()
        // Remove "Not specified" or similar
        return if (cleaned.equals("Not specified", ignoreCase = true) ||
                   cleaned.equals("N/A", ignoreCase = true) ||
                   cleaned.equals("None", ignoreCase = true) ||
                   cleaned.isEmpty()) {
            ""
        } else {
            cleaned
        }
    }

    /**
     * Fallback rule-based parsing when AI is not available
     * Uses regex patterns to extract common medicine information
     */
    private fun parseWithRules(text: String): ParsedMedicine {
        Log.d(TAG, "Using rule-based parsing as fallback")
        
        val name = extractName(text)
        val dosage = extractDosage(text)
        val frequency = extractFrequency(text)
        val time = extractTime(text)
        val duration = extractDuration(text)
        val quantity = extractQuantity(text)
        val instructions = extractInstructions(text)

        return ParsedMedicine(
            name = name,
            dosage = dosage,
            frequency = frequency,
            time = time,
            duration = duration,
            quantity = quantity,
            instructions = instructions
        )
    }

    // Rule-based extraction methods

    private fun extractName(text: String): String {
        // Look for medicine name (usually first line or after "Name:")
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isNotEmpty()) {
            val firstLine = lines[0].trim()
            // Remove common prefixes
            return firstLine
                .replace(Regex("^(Name|Medicine|Drug):?\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
        }
        return ""
    }

    private fun extractDosage(text: String): String {
        // Look for patterns like "100mg", "5ml", "250 mg"
        val dosagePattern = Regex("""(\d+\.?\d*)\s*(mg|g|ml|mcg|iu|units?)""", RegexOption.IGNORE_CASE)
        val match = dosagePattern.find(text)
        return match?.value ?: ""
    }

    private fun extractFrequency(text: String): String {
        val frequencyPatterns = listOf(
            Regex("""(once|twice|three times|thrice)\s*(daily|a day|per day)""", RegexOption.IGNORE_CASE),
            Regex("""every\s*(\d+)\s*hours?""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*times?\s*(daily|a day|per day)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in frequencyPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.value
            }
        }
        return ""
    }

    private fun extractTime(text: String): String {
        val timePatterns = listOf(
            Regex("""(morning|afternoon|evening|night|bedtime)""", RegexOption.IGNORE_CASE),
            Regex("""(before|after|with)\s*(meals?|food|breakfast|lunch|dinner)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in timePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.value
            }
        }
        return ""
    }

    private fun extractDuration(text: String): String {
        val durationPattern = Regex("""(\d+)\s*(days?|weeks?|months?|years?)""", RegexOption.IGNORE_CASE)
        val match = durationPattern.find(text)
        return match?.value ?: ""
    }

    private fun extractQuantity(text: String): String {
        val quantityPatterns = listOf(
            Regex("""(\d+)\s*(tablets?|pills?|capsules?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+\.?\d*)\s*ml""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in quantityPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.value
            }
        }
        return ""
    }

    private fun extractInstructions(text: String): String {
        val instructionKeywords = listOf(
            "take with", "do not", "avoid", "store", "keep", 
            "refrigerate", "shake well", "dissolve", "swallow whole"
        )
        
        val lines = text.lines()
        val instructionLines = lines.filter { line ->
            instructionKeywords.any { keyword ->
                line.contains(keyword, ignoreCase = true)
            }
        }
        
        return instructionLines.joinToString(". ").take(200) // Limit length
    }
}
