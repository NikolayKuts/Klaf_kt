package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.SpeechToTextSegment
import com.kuts.domain.entities.VocabularySource
import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptTimestampFormatterTest {

    private val cleaner = TranscriptTextCleaner()

    @Test
    fun `formats segments into standard srt format`() {
        val segments = listOf(
            SpeechToTextSegment(
                startMillis = 0L,
                endMillis = 2_500L,
                text = "Welcome to Klaf.",
            ),
            SpeechToTextSegment(
                startMillis = 3_120L,
                endMillis = 5_005L,
                text = "Learn new words easily.",
            ),
        )

        val result = TranscriptTimestampFormatter.format(
            transcript = "Fallback text",
            segments = segments,
        )

        val expected = """
            1
            00:00:00,000 --> 00:00:02,500
            Welcome to Klaf.

            2
            00:00:03,120 --> 00:00:05,005
            Learn new words easily.
        """.trimIndent()

        assertEquals(expected = expected, actual = result)
    }

    @Test
    fun `formats hours, minutes, seconds and milliseconds with proper padding`() {
        // 1h = 3600000ms, 2m = 120000ms, 3s = 3000ms, 7ms = 7ms -> 3723007ms
        val timestamp = TranscriptTimestampFormatter.formatTimestamp(millis = 3_723_007L)
        assertEquals(expected = "01:02:03,007", actual = timestamp)
    }

    @Test
    fun `formats zero and negative millis safely`() {
        assertEquals(expected = "00:00:00,000", actual = TranscriptTimestampFormatter.formatTimestamp(0L))
        assertEquals(expected = "00:00:00,000", actual = TranscriptTimestampFormatter.formatTimestamp(-500L))
    }

    @Test
    fun `falls back to transcript when segments list is empty`() {
        val result = TranscriptTimestampFormatter.format(
            transcript = "  Raw transcript text without timestamps.  ",
            segments = emptyList(),
        )

        assertEquals(expected = "Raw transcript text without timestamps.", actual = result)
    }

    @Test
    fun `falls back to transcript when all segments have blank text`() {
        val result = TranscriptTimestampFormatter.format(
            transcript = "Fallback transcript",
            segments = listOf(
                SpeechToTextSegment(startMillis = 0L, endMillis = 1000L, text = "   "),
            ),
        )

        assertEquals(expected = "Fallback transcript", actual = result)
    }

    @Test
    fun `formatted srt is cleanly processed by TranscriptTextCleaner`() {
        val segments = listOf(
            SpeechToTextSegment(startMillis = 1000L, endMillis = 3000L, text = "First segment."),
            SpeechToTextSegment(startMillis = 4000L, endMillis = 6000L, text = "Second segment."),
        )

        val srt = TranscriptTimestampFormatter.format(transcript = "", segments = segments)
        val cleaned = cleaner.clean(rawText = srt)

        assertEquals(expected = "First segment.\nSecond segment.", actual = cleaned)
    }

    @Test
    fun `vocabulary source defaults url to empty string`() {
        val source = VocabularySource(
            id = 1,
            title = "Test",
            description = "Desc",
            rawText = "Raw",
            cleanText = "Clean",
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        assertEquals(expected = "", actual = source.url)
    }
}
