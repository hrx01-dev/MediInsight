package com.runanywhere.startup_hackathon20.ai

/**
 * Utility to parse prescription text and extract medicine information.
 */
object PrescriptionParser {
    data class Medicine(
        val name: String,
        val dosage: String = "",
        val frequency: String = "",
        val time: String = "",
        val duration: String = "",
        val quantity: String = "",
        val instructions: String = ""
    )

    fun parse(text: String): List<Medicine> {
        val lines = text.lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotBlank() }
        val dosageRegex = Regex("(?i)\\b\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|g|ml|mL|IU|units?)\\b")
        val frequencyRegex = Regex("(?i)\\b(?:once|twice|thrice|\\d+\\s*(?:times|x)\\s*(?:a|per)\\s*day|every\\s*\\d+\\s*hours?|OD|BD|BID|TID|QID|HS|SOS)\\b")
        val durationRegex = Regex("(?i)\\b\\d+\\s*(?:days?|weeks?|months?)\\b")
        val dosageFormRegex = Regex("(?i)\\b(?:tab(?:let)?s?|cap(?:sule)?s?|syrup|suspension|injection|inj|cream|ointment|drops?)\\b")
        val stopWords = Regex("(?i)\\b(?:patient|doctor|dr|date|age|address|phone|hospital|clinic|diagnosis|prescription|rx|signature|registration|reg|follow.?up|allergy|complaint|history|mrn|name)\\b")
        val result = mutableListOf<Medicine>()
        lines.forEach { line ->
            val doseMatch = dosageRegex.find(line)
            val formMatch = dosageFormRegex.find(line)
            val hasMedicineEvidence = doseMatch != null || formMatch != null
            if (!hasMedicineEvidence || stopWords.containsMatchIn(line)) return@forEach

            val boundary = listOfNotNull(doseMatch?.range?.first, formMatch?.range?.first).minOrNull() ?: return@forEach
            val prefix = line.substring(0, boundary).trim().trimEnd(':', '-', '.', ',', ';', ')')
            val name = prefix.replace(Regex("(?i)^(?:\\d+[.)]?\\s*|(?:tab|tablet|cap|capsule)\\s+)"), "").trim()
            if (name.length < 2 || name.split(Regex("\\s+")).size > 5 || stopWords.containsMatchIn(name)) return@forEach

            val dosage = listOfNotNull(doseMatch?.value, formMatch?.value).joinToString(" ").trim()
            val frequency = frequencyRegex.find(line)?.value.orEmpty()
            val duration = durationRegex.find(line)?.value.orEmpty()
            val quantity = Regex("(?i)\\b(?:qty|quantity)\\s*[:.-]?\\s*(\\d+)\\b").find(line)?.groupValues?.get(1)?.let { "$it units" }.orEmpty()
            val instructionBits = buildList {
                Regex("(?i)\\b(?:after|before|with)\\s+(?:food|meals?|breakfast|lunch|dinner)\\b|\\bon empty stomach\\b|\\b(?:morning|afternoon|evening|night|bedtime)\\b")
                    .findAll(line).forEach { add(it.value) }
            }.distinct()
            
            // Try to extract time from instruction bits
            val timeValue = instructionBits.find { it.contains(Regex("morning|afternoon|evening|night|bedtime", RegexOption.IGNORE_CASE)) } ?: ""
            
            result.add(Medicine(name, dosage, frequency, timeValue, duration, quantity, instructionBits.joinToString("; ")))
        }
        return result.distinctBy { it.name.lowercase() }
    }
}
