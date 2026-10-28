package com.runanywhere.startup_hackathon20.ai

import android.util.Log
import com.google.gson.Gson
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Structures OCR text into medicine entries using MediInsight's configured on-device model. */
object PrescriptionParser {
    data class Medicine(
        val name: String = "",
        val dosage: String = "",
        val frequency: String = "",
        val time: String = "",
        val duration: String = "",
        val quantity: String = "",
        val instructions: String = ""
    )

    private data class Result(val medicines: List<Medicine> = emptyList())
    private val gson = Gson()
    private val medicineEvidence = Regex("(?i)\\b(?:mg|mcg|µg|g|ml|iu|units?|tablet|tablets|tab|tabs|capsule|capsules|cap|caps|syrup|injection|inj|drops?)\\b|\\b\\d+\\s*(?:tablet|tablets|tab|tabs|capsule|capsules|cap|caps)\\b")
    private val nonMedicineHeadings = Regex("(?i)\\b(?:prescription|template|prescription no|prescription date|patient information|patient|name|age|phone|email|address|date of birth|gender|allergies|health condition|list of prescribed medications|medication name|purpose|dosage|dose|route|frequency|physician|signature|registration|diagnosis)\\b")

    suspend fun parse(ocrText: String): List<Medicine> = withContext(Dispatchers.IO) {
        if (ocrText.isBlank()) return@withContext emptyList()
        val prompt = """You extract medication orders from OCR of a prescription. The OCR may be laid out as a table where each medicine row contains separate cells for name, purpose, dose, route, and frequency. Reconstruct row relationships by reading order and nearby lines. Return ONLY valid JSON in this schema:
{"medicines":[{"name":"","dosage":"","frequency":"","time":"","duration":"","quantity":"","instructions":""}]}

Rules:
- Include each medicine name from the prescribed medications table, even when its name has no numeric strength or dosage form. Include only rows from that table; never use patient/doctor/hospital details, headings, purpose, or route as medicine names.
- Keep every medicine as its own object. Copy only supported details from the text; use empty strings for missing fields.
- dosage is strength/dose (example: 500 mg or 1 tablet); frequency is how often (example: twice daily); time is time of day or relation to meals; duration is course length; quantity is total units only when explicitly stated; instructions are other explicit medicine-specific directions.
- Do not put the same value in multiple fields. Do not treat OCR headings, names, addresses, dates, diagnoses, or signatures as medicines.

OCR text:
${ocrText.take(6000)}""".trimIndent()
        try {
        val response = RunAnywhere.generate(prompt)
            val json = response.substringAfter('{', "").let { if (it.isNotEmpty()) "{$it" else "" }
            if (json.isBlank()) return@withContext emptyList()
            val result = gson.fromJson(json, Result::class.java)
            val modelMedicines = result.medicines.orEmpty().filter { medicine ->
                val name = medicine.name.trim()
                name.length >= 2 && !nonMedicineHeadings.containsMatchIn(name) &&
                    (medicineEvidence.containsMatchIn(name + " " + medicine.dosage) || medicine.frequency.isNotBlank())
            }
            (modelMedicines.ifEmpty { parseTabularFallback(ocrText) }).distinctBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.e("PrescriptionParser", "Could not structure prescription OCR", e)
            emptyList()
        }
    }

    /** Handles clear printed tables when the local model returns malformed or empty JSON. */
    private fun parseTabularFallback(text: String): List<Medicine> {
        val frequencyRegex = Regex("(?i)\\b(?:every\\s+\\d+\\s+hours?|once\\s+(?:a\\s+)?day|twice\\s+(?:a\\s+)?day|\\d+\\s+times?\\s+(?:a|per)\\s+day)\\b")
        val doseRegex = Regex("(?i)\\b(?:\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|g|ml|iu|units?)|\\d+\\s+(?:tablet|tablets|tab|tabs|capsule|capsules))\\b")
        val header = text.lines().indexOfFirst { line ->
            line.contains("medication name", true) && line.contains("dosage", true) && line.contains("frequency", true)
        }
        if (header < 0) return emptyList()
        val ignored = Regex("(?i)\\b(?:oral|intravenous|topical|purpose|infection|immune system|fever|phlegm)\\b")
        return text.lines().drop(header + 1).mapNotNull { raw ->
            val line = raw.replace(Regex("\\s+"), " ").trim().trim('|', '│', '¦', '•', '-', ' ')
            val frequency = frequencyRegex.find(line)?.value ?: return@mapNotNull null
            val dose = doseRegex.find(line)?.value ?: return@mapNotNull null
            val name = line.substringBefore(dose).trim().trim('|', '│', '¦', '•', '-', ' ')
            if (name.length < 2 || name.split(Regex("\\s+")).size > 4 || nonMedicineHeadings.containsMatchIn(name) || ignored.containsMatchIn(name)) return@mapNotNull null
            Medicine(name = name, dosage = dose, frequency = frequency)
        }
    }
}
