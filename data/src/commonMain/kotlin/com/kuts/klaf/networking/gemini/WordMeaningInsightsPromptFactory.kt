package com.kuts.klaf.networking.gemini

data class WordMeaningInsightsPrompt(
    val systemInstruction: String,
    val userPrompt: String,
    val responseJsonSchema: String,
)

object WordMeaningInsightsPromptFactory {

    fun build(word: String): WordMeaningInsightsPrompt {
        require(value = word.isNotBlank()) { "Word must not be blank." }

        return WordMeaningInsightsPrompt(
            systemInstruction = WordMeaningInsightsContract.systemInstruction,
            userPrompt = buildUserPrompt(word = word.trim()),
            responseJsonSchema = WordMeaningInsightsContract.responseJsonSchema,
        )
    }

    private fun buildUserPrompt(word: String): String = """
        Analyze the English word "$word".
        Build a learner-friendly semantic profile.

        Requirements:
        1. Return from ${WordMeaningInsightsContract.MIN_SENSES_COUNT} to ${WordMeaningInsightsContract.MAX_SENSES_COUNT} senses.
        2. Sort senses by real usage frequency (most typical first).
        3. For each sense include:
           - rank
           - translation_ru
           - cefr (A1/A2/B1/B2/C1/C2)
           - context (short disambiguating label)
           - examples_en (exactly ${WordMeaningInsightsContract.EXAMPLES_PER_SENSE} distinct examples)
        4. Examples must be natural, concise, and reflect the exact sense.
        5. Avoid duplicate senses and avoid rare/archaic meanings unless common.
        6. language must be "${WordMeaningInsightsContract.LANGUAGE}".
        7. Return JSON only.
    """.trimIndent()
}
