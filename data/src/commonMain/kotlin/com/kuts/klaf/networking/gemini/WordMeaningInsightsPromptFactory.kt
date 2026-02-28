package com.kuts.klaf.networking.gemini

data class WordMeaningInsightsPrompt(
    val systemInstruction: String,
    val userPrompt: String,
    val responseJsonSchema: String,
    val responseApiSchema: String,
)

object WordMeaningInsightsPromptFactory {

    fun build(word: String): WordMeaningInsightsPrompt {
        require(value = word.isNotBlank()) { "Word must not be blank." }
        val requestedWord = word.trim()

        return WordMeaningInsightsPrompt(
            systemInstruction = WordMeaningInsightsContract.systemInstruction,
            userPrompt = buildUserPrompt(word = requestedWord),
            responseJsonSchema = WordMeaningInsightsContract.buildResponseJsonSchema(word = requestedWord),
            responseApiSchema = WordMeaningInsightsContract.buildResponseApiSchema(word = requestedWord),
        )
    }

    private fun buildUserPrompt(word: String): String = """
        Analyze the English word "$word".
        Build a learner-friendly semantic profile.

        Requirements:
        1. "word" must be exactly "$word".
        2. Set "word_valid":
           - true, if the input is a real English lexical word.
           - false, if the input token is nonsense / not a real English word.
        3. Set "invalid_reason":
           - empty string when word_valid=true.
           - short reason when word_valid=false.
        4. If word_valid=false:
           - return "senses": [].
           - do not invent meanings or examples.
        5. If word_valid=true:
           - return from ${WordMeaningInsightsContract.MIN_SENSES_COUNT} to ${WordMeaningInsightsContract.MAX_SENSES_COUNT} senses.
           - sort senses by real usage frequency (most typical first).
        6. For each sense include:
           - rank
           - translation_ru
           - cefr (A1/A2/B1/B2/C1/C2)
           - context (short disambiguating label)
           - examples_en (exactly ${WordMeaningInsightsContract.EXAMPLES_PER_SENSE} distinct examples)
        7. Examples must be natural, concise, and reflect the exact sense.
        8. Avoid duplicate senses and avoid rare/archaic meanings unless common.
        9. language must be "${WordMeaningInsightsContract.LANGUAGE}".
        10. Return JSON only.
    """.trimIndent()
}
