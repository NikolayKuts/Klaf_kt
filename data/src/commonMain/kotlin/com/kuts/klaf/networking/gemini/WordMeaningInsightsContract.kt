package com.kuts.klaf.networking.gemini

object WordMeaningInsightsContract {

    const val LANGUAGE = "en"
    const val MIN_SENSES_COUNT = 3
    const val MAX_SENSES_COUNT = 5
    const val EXAMPLES_PER_SENSE = 3

    val systemInstruction = """
        You are a lexicography assistant for English learners.
        Return only strict JSON that matches the provided JSON schema.
        Do not include markdown, comments, or additional keys.
        Senses must be ordered by real usage frequency in modern English.
        Keep outputs practical for learners and avoid archaic meanings.
    """.trimIndent()

    val responseJsonSchema = """
        {
          "type": "object",
          "additionalProperties": false,
          "required": ["word", "language", "senses"],
          "properties": {
            "word": {
              "type": "string",
              "minLength": 1
            },
            "language": {
              "type": "string",
              "enum": ["en"]
            },
            "senses": {
              "type": "array",
              "minItems": 3,
              "maxItems": 5,
              "items": {
                "type": "object",
                "additionalProperties": false,
                "required": ["rank", "translation_ru", "cefr", "context", "examples_en"],
                "properties": {
                  "rank": {
                    "type": "integer",
                    "minimum": 1,
                    "maximum": 5
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
                    "minItems": 3,
                    "maxItems": 3,
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

    val responseApiSchema = """
        {
          "type": "OBJECT",
          "required": ["word", "language", "senses"],
          "properties": {
            "word": {
              "type": "STRING"
            },
            "language": {
              "type": "STRING",
              "enum": ["en"]
            },
            "senses": {
              "type": "ARRAY",
              "minItems": 3,
              "maxItems": 5,
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
                    "minItems": 3,
                    "maxItems": 3,
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
