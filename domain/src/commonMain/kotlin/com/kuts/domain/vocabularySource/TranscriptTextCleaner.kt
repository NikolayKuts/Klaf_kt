package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.VocabularySource

class TranscriptTextCleaner(
    private val maxTextLength: Int = VocabularySource.MAX_TEXT_LENGTH,
) {

    fun validateRawText(rawText: String): TranscriptTextValidationResult {
        return if (rawText.length > maxTextLength) {
            TranscriptTextValidationResult.TooLong(
                actualLength = rawText.length,
                maxLength = maxTextLength,
            )
        } else {
            TranscriptTextValidationResult.Valid
        }
    }

    fun clean(rawText: String): String {
        val normalizedLines = rawText
            .replace(oldValue = "\r\n", newValue = "\n")
            .replace(oldValue = "\r", newValue = "\n")
            .lines()

        return normalizedLines
            .map { line -> line.trim().stripSimpleMarkup() }
            .filterNot { line -> line.isBlank() }
            .filterNot { line -> line.isSubtitleIndex() }
            .filterNot { line -> line.isTimestampLine() }
            .filterNot { line -> line.isWebVttMetadata() }
            .joinToString(separator = "\n")
            .replace(Regex(pattern = "\n{3,}"), replacement = "\n\n")
            .trim()
    }

    private fun String.isSubtitleIndex(): Boolean {
        return all { char -> char.isDigit() } && length <= MAX_SUBTITLE_INDEX_LENGTH
    }

    private fun String.isTimestampLine(): Boolean {
        return srtTimestampRegex.containsMatchIn(input = this)
            || vttTimestampRegex.containsMatchIn(input = this)
    }

    private fun String.stripSimpleMarkup(): String {
        return replace(regex = htmlTagRegex, replacement = "")
            .replace(oldValue = "&nbsp;", newValue = " ")
            .replace(oldValue = "&amp;", newValue = "&")
            .replace(oldValue = "&lt;", newValue = "<")
            .replace(oldValue = "&gt;", newValue = ">")
            .trim()
    }

    private fun String.isWebVttMetadata(): Boolean {
        return equals(other = "WEBVTT", ignoreCase = true)
            || startsWith(prefix = "NOTE ", ignoreCase = true)
    }

    companion object {

        private const val MAX_SUBTITLE_INDEX_LENGTH = 7

        private val srtTimestampRegex = Regex(
            pattern = """^\d{1,2}:\d{2}:\d{2},\d{3}\s+-->\s+\d{1,2}:\d{2}:\d{2},\d{3}.*$""",
        )
        private val vttTimestampRegex = Regex(
            pattern = """^\d{1,2}:\d{2}:\d{2}\.\d{3}\s+-->\s+\d{1,2}:\d{2}:\d{2}\.\d{3}.*$""",
        )
        private val htmlTagRegex = Regex(pattern = """<[^>]+>""")
    }
}

sealed interface TranscriptTextValidationResult {

    data object Valid : TranscriptTextValidationResult

    data class TooLong(
        val actualLength: Int,
        val maxLength: Int,
    ) : TranscriptTextValidationResult
}
