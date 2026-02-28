package com.kuts.klaf.networking.gemini

object WordMeaningInsightsContract {

    const val LANGUAGE = "en"
    const val MIN_SENSES_COUNT = 4
    const val MAX_SENSES_COUNT = 4
    const val EXAMPLES_PER_SENSE = 3

    val systemInstruction = """
        You are a lexicography assistant for English learners.
        Return only strict JSON that matches the provided JSON schema.
        Do not include markdown, comments, or additional keys.
        Senses must be ordered by real usage frequency in modern English.
        Keep outputs practical for learners and avoid archaic meanings.
        If the input token is not a real English lexical word, mark it invalid.
    """.trimIndent()

    fun buildResponseJsonSchema(word: String): String {
        val escapedWord = word.trim().escapeJsonString()
        return """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["word", "word_valid", "invalid_reason", "language", "senses"],
              "properties": {
                "word": {
                  "type": "string",
                  "minLength": 1,
                  "enum": ["$escapedWord"]
                },
                "word_valid": {
                  "type": "boolean"
                },
                "invalid_reason": {
                  "type": "string"
                },
                "language": {
                  "type": "string",
                  "enum": ["en"]
                },
                "senses": {
                  "type": "array",
                  "minItems": 0,
                  "maxItems": $MAX_SENSES_COUNT,
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["rank", "translation_ru", "cefr", "context", "examples_en"],
                    "properties": {
                      "rank": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": $MAX_SENSES_COUNT
                      },
                      "translation_ru": {
                        "type": "string",
                        "minLength": 1
                      },
                      "cefr": {
                        "type": "string",
                        "enum": ["A1", "A2", "B1", "B2", "C1", "C2"]
                      },
                      "context": {
                        "type": "string",
                        "minLength": 1
                      },
                      "examples_en": {
                        "type": "array",
                        "minItems": $EXAMPLES_PER_SENSE,
                        "maxItems": $EXAMPLES_PER_SENSE,
                        "items": {
                          "type": "string",
                          "minLength": 1
                        },
                        "uniqueItems": true
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }

    fun buildResponseApiSchema(word: String): String {
        val escapedWord = word.trim().escapeJsonString()
        return """
            {
              "type": "OBJECT",
              "required": ["word", "word_valid", "invalid_reason", "language", "senses"],
              "properties": {
                "word": {
                  "type": "STRING",
                  "enum": ["$escapedWord"]
                },
                "word_valid": {
                  "type": "BOOLEAN"
                },
                "invalid_reason": {
                  "type": "STRING"
                },
                "language": {
                  "type": "STRING",
                  "enum": ["en"]
                },
                "senses": {
                  "type": "ARRAY",
                  "minItems": 0,
                  "maxItems": $MAX_SENSES_COUNT,
                  "items": {
                    "type": "OBJECT",
                    "required": ["rank", "translation_ru", "cefr", "context", "examples_en"],
                    "properties": {
                      "rank": {
                        "type": "INTEGER"
                      },
                      "translation_ru": {
                        "type": "STRING"
                      },
                      "cefr": {
                        "type": "STRING",
                        "enum": ["A1", "A2", "B1", "B2", "C1", "C2"]
                      },
                      "context": {
                        "type": "STRING"
                      },
                      "examples_en": {
                        "type": "ARRAY",
                        "minItems": $EXAMPLES_PER_SENSE,
                        "maxItems": $EXAMPLES_PER_SENSE,
                        "items": {
                          "type": "STRING"
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }

    private fun String.escapeJsonString(): String {
        return this
            .replace(oldValue = "\\", newValue = "\\\\")
            .replace(oldValue = "\"", newValue = "\\\"")
    }
}
