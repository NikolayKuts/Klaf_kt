package com.kuts.domain.vocabularySource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TranscriptTextCleanerTest {

    private val cleaner = TranscriptTextCleaner(maxTextLength = 20)

    @Test
    fun `clean removes srt indexes and timestamps`() {
        val rawText = """
            1
            00:00:01,000 --> 00:00:03,000
            Hello there.

            2
            00:00:04,000 --> 00:00:05,500
            General Kenobi.
        """.trimIndent()

        val result = cleaner.clean(rawText = rawText)

        assertEquals(
            expected = "Hello there.\nGeneral Kenobi.",
            actual = result,
        )
    }

    @Test
    fun `clean removes vtt timestamps and simple markup`() {
        val rawText = """
            WEBVTT

            00:00:01.000 --> 00:00:03.000 align:start
            <i>I need to charge my phone.</i>
        """.trimIndent()

        val result = cleaner.clean(rawText = rawText)

        assertEquals(
            expected = "I need to charge my phone.",
            actual = result,
        )
    }

    @Test
    fun `validate accepts text within limit`() {
        val result = cleaner.validateRawText(rawText = "short text")

        assertEquals(
            expected = TranscriptTextValidationResult.Valid,
            actual = result,
        )
    }

    @Test
    fun `validate rejects text over limit`() {
        val result = cleaner.validateRawText(rawText = "x".repeat(n = 21))

        assertIs<TranscriptTextValidationResult.TooLong>(value = result)
        assertEquals(expected = 21, actual = result.actualLength)
        assertEquals(expected = 20, actual = result.maxLength)
    }
}
