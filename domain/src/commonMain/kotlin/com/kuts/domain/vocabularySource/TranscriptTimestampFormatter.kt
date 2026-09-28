package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.SpeechToTextSegment

object TranscriptTimestampFormatter {

    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_HOUR = 3_600_000L

    fun format(
        transcript: String,
        segments: List<SpeechToTextSegment>,
    ): String {
        val validSegments = segments.filter { segment -> segment.text.isNotBlank() }
        if (validSegments.isEmpty()) {
            return transcript.trim()
        }

        return validSegments.mapIndexed { index, segment ->
            val indexNumber = index + 1
            val startTime = formatTimestamp(millis = segment.startMillis)
            val endTime = formatTimestamp(millis = segment.endMillis)
            val text = segment.text.trim()
            "$indexNumber\n$startTime --> $endTime\n$text"
        }.joinToString(separator = "\n\n")
    }

    fun formatTimestamp(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val hours = safeMillis / MILLIS_PER_HOUR
        val remainingAfterHours = safeMillis % MILLIS_PER_HOUR
        val minutes = remainingAfterHours / MILLIS_PER_MINUTE
        val remainingAfterMinutes = remainingAfterHours % MILLIS_PER_MINUTE
        val seconds = remainingAfterMinutes / MILLIS_PER_SECOND
        val ms = remainingAfterMinutes % MILLIS_PER_SECOND

        val hoursStr = hours.toString().padStart(2, '0')
        val minutesStr = minutes.toString().padStart(2, '0')
        val secondsStr = seconds.toString().padStart(2, '0')
        val msStr = ms.toString().padStart(3, '0')

        return "$hoursStr:$minutesStr:$secondsStr,$msStr"
    }
}
