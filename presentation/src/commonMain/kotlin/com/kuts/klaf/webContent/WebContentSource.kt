package com.kuts.klaf.webContent

import kotlinx.serialization.Serializable

@Serializable
sealed interface WebContentSource {
    val url: String?

    @Serializable
    data class YouGlish(
        val word: String,
    ) : WebContentSource {
        override val url: String?
            get() {
                val trimmedWord = word.trim()
                if (trimmedWord.isEmpty()) {
                    return null
                }

                return "$BASE_URL/${trimmedWord.encodeUrlPathSegment()}/english?"
            }

        private companion object {
            const val BASE_URL = "https://youglish.com/pronounce"
        }
    }
}

private fun String.encodeUrlPathSegment(): String {
    val unreservedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val bytes = encodeToByteArray()

    return buildString(capacity = bytes.size) {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xFF
            val character = value.toChar()

            if (character in unreservedCharacters) {
                append(character)
            } else {
                append('%')
                append("0123456789ABCDEF"[value ushr 4])
                append("0123456789ABCDEF"[value and 0x0F])
            }
        }
    }
}
