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
    private val ignoredTerms = Regex("(?i)\\b(?:oral|intravenous|topical|purpose|infection|immune system|fever|phlegm)\\b")

    suspend fun parse(ocrText: String, forceAi: Boolean = false): List<Medicine> = withContext(Dispatchers.IO) {
        if (ocrText.isBlank()) return@withContext emptyList()
        if (!forceAi) {
            val tableMedicines = parseTabularFallback(ocrText)
            if (tableMedicines.isNotEmpty()) return@withContext tableMedicines
        }
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
            if (json.isBlank()) return@withContext parseTabularFallback(ocrText)
            val result = runCatching { gson.fromJson(json, Result::class.java) }.getOrNull()
            val modelMedicines = result?.medicines.orEmpty().filter { medicine ->
                val name = medicine.name.trim()
                name.length >= 2 && !nonMedicineHeadings.containsMatchIn(name) &&
                    (medicineEvidence.containsMatchIn(name + " " + medicine.dosage) || medicine.frequency.isNotBlank())
            }
            (modelMedicines.ifEmpty { parseTabularFallback(ocrText) }).distinctBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.e("PrescriptionParser", "Could not structure prescription OCR", e)
            parseTabularFallback(ocrText)
        }
    }

    /** Handles clear printed tables when the local model returns malformed or empty JSON. */
    private fun parseTabularFallback(text: String): List<Medicine> {
        val frequencyRegex = Regex("(?i)\\b(?:every\\s+\\d+\\s+hours?|once\\s+(?:a\\s+)?day|twice\\s+(?:a\\s+)?day|\\d+\\s+times?\\s+(?:a|per)\\s+day)\\b")
        val doseRegex = Regex("(?i)\\b(?:\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|g|ml|iu|units?)|\\d+\\s+(?:tablet|tablets|tab|tabs|capsule|capsules))\\b")
        val allLines = text.lines().map { it.replace(Regex("\\s+"), " ").trim().trim('|', '│', '¦', '•', '-', ' ') }.filter { it.isNotBlank() }
        // ML Kit may preserve each table row as one line, or produce a separate line per cell.
        val rowPattern = Regex("(?i)^(.+?)\\s+(?:(?:removes?\\s+)?(?:phlegm|for\\s+fever|bacterial\\s+infection|immune\\s+system)\\s+)?(\\d+\\s+(?:tablet|tablets|tab|tabs|capsule|capsules)|\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|g|ml|iu|units?))\\s+(?:oral|intravenous|topical|im|iv|po)\\s+(every\\s+\\d+\\s+hours?|once\\s+(?:a\\s+)?day|twice\\s+(?:a\\s+)?day|\\d+\\s+times?\\s+(?:a|per)\\s+day)$")
        val rowMatches = allLines.mapNotNull { line ->
            val m = rowPattern.matchEntire(line) ?: return@mapNotNull null
            val name = m.groupValues[1].trim()
            if (name.length < 2 || nonMedicineHeadings.containsMatchIn(name) || ignoredTerms.containsMatchIn(name)) return@mapNotNull null
            Medicine(
                name = name.replace(Regex("(?i)\\s+(?:TIccaICa|bacterial\\s+infection)$"), "").trim(),
                dosage = m.groupValues[2].trim(),
                frequency = m.groupValues[3].trim()
            )
        }
        if (rowMatches.isNotEmpty()) return rowMatches.distinctBy { it.name.lowercase() }

        // OCR on tables frequently groups all cells by column. Start strictly after the table header
        // and use only medication-name, dosage, and frequency columns to avoid patient data.
        val header = allLines.indexOfFirst { line ->
            line.contains("medication name", true) || line.contains("medicine name", true) ||
                (line.contains("purpose", true) && line.contains("dosage", true) && line.contains("frequency", true))
        }
        val rowCandidates = allLines.filter { line ->
            val dose = doseRegex.find(line)
            val frequency = frequencyRegex.find(line)
            if (dose == null || frequency == null) return@filter false
            val name = line.substringBefore(dose.value).trim().trim('|', '│', '¦', '•', '-', ' ')
            name.length >= 2 && name.split(Regex("\\s+")).size <= 4 &&
                !nonMedicineHeadings.containsMatchIn(name) && !ignoredTerms.containsMatchIn(name)
        }
        if (rowCandidates.isNotEmpty()) return rowCandidates.mapNotNull { line ->
            val dose = doseRegex.find(line) ?: return@mapNotNull null
            val frequency = frequencyRegex.find(line) ?: return@mapNotNull null
            val name = line.substringBefore(dose.value).trim().trim('|', '│', '¦', '•', '-', ' ')
            Medicine(name = name, dosage = dose.value, frequency = frequency.value)
        }.distinctBy { it.name.lowercase() }
        if (header < 0) {
            // ML Kit often emits table cells as separate lines rather than preserving columns.
            val cellLines = allLines
            val namesIndex = cellLines.indexOfFirst { it.equals("Medication Name", true) || it.equals("Medicine Name", true) }
            if (namesIndex >= 0) {
                val doseColumn = cellLines.indexOfFirst { it.equals("Dosage", true) || it.equals("Dose", true) }
                val freqColumn = cellLines.indexOfFirst { it.equals("Frequency", true) }
                if (doseColumn > namesIndex && freqColumn > doseColumn) {
                    val names = cellLines.subList(namesIndex + 1, doseColumn).filter { !nonMedicineHeadings.containsMatchIn(it) }
                    val doses = cellLines.subList(doseColumn + 1, freqColumn).mapNotNull { doseRegex.find(it)?.value }
                    val freqStart = freqColumn + 1
                    val frequencies = cellLines.drop(freqStart).mapNotNull { frequencyRegex.find(it)?.value }
                    if (names.isNotEmpty() && doses.isNotEmpty() && frequencies.isNotEmpty()) {
                        return names.indices.mapNotNull { index ->
                            val name = names.getOrNull(index) ?: return@mapNotNull null
                            val dose = doses.getOrNull(index) ?: return@mapNotNull null
                            val frequency = frequencies.getOrNull(index) ?: return@mapNotNull null
                            if (name.split(Regex("\\s+")).size > 4 || ignoredTerms.containsMatchIn(name)) null
                            else Medicine(name = name, dosage = dose, frequency = frequency)
                        }
                    }
                }
            }
            val frequencyIndexes = cellLines.indices.filter { frequencyRegex.containsMatchIn(cellLines[it]) }
            return frequencyIndexes.mapNotNull { frequencyIndex ->
                if (frequencyIndex < 2) return@mapNotNull null
                val doseIndex = (frequencyIndex - 1 downTo maxOf(0, frequencyIndex - 3))
                    .firstOrNull { doseRegex.containsMatchIn(cellLines[it]) } ?: return@mapNotNull null
                val nameIndex = doseIndex - 2
                if (nameIndex < 0) return@mapNotNull null
                val name = cellLines[nameIndex]
                if (name.length < 2 || name.split(Regex("\\s+")).size > 4 || nonMedicineHeadings.containsMatchIn(name)) return@mapNotNull null
                val purpose = cellLines.getOrNull(nameIndex + 1).orEmpty()
                if (ignoredTerms.containsMatchIn(purpose) || purpose.split(Regex("\\s+")).size >= 2) return@mapNotNull null
                Medicine(name = name, dosage = doseRegex.find(cellLines[doseIndex])?.value.orEmpty(), frequency = frequencyRegex.find(cellLines[frequencyIndex])?.value.orEmpty())
            }.distinctBy { it.name.lowercase() }
        }
        return text.lines().drop(header + 1).mapNotNull { raw ->
            val line = raw.replace(Regex("\\s+"), " ").trim().trim('|', '│', '¦', '•', '-', ' ')
            val frequency = frequencyRegex.find(line)?.value ?: return@mapNotNull null
            val dose = doseRegex.find(line)?.value ?: return@mapNotNull null
            val name = line.substringBefore(dose).trim().trim('|', '│', '¦', '•', '-', ' ')
            if (name.length < 2 || name.split(Regex("\\s+")).size > 4 || nonMedicineHeadings.containsMatchIn(name) || ignoredTerms.containsMatchIn(name)) return@mapNotNull null
            Medicine(name = name, dosage = dose, frequency = frequency)
        }
    }
}
