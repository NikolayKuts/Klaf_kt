package com.kuts.klaf.server.contract

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class KlafServerProtocolTest {

    @kotlin.test.Test
    fun binaryFrameRejectsEmptyRequestId() {
        kotlin.test.assertNull(AudioUploadFrameCodec.decode(byteArrayOf(0, 0, 1)))
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AudioUploadFrameCodec.encode("", byteArrayOf(1))
        }
    }

    @kotlin.test.Test
    fun binaryFrameRejectsMalformedUtf8RequestId() {
        kotlin.test.assertNull(AudioUploadFrameCodec.decode(byteArrayOf(0, 1, 0xFF.toByte(), 1)))
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    @Test
    fun `protocol version is incremented to 7`() {
        assertEquals(expected = 7, actual = KLAF_SERVER_PROTOCOL_VERSION)
    }

    @Test
    fun `audio upload frame codec encodes and decodes round-trip`() {
        val requestId = "req-test-12345"
        val audioData = byteArrayOf(0, 1, 2, 3, 4, 5, -1, -128, 127)

        val encoded = AudioUploadFrameCodec.encode(requestId = requestId, audioChunk = audioData)
        val decoded = AudioUploadFrameCodec.decode(frameBytes = encoded)

        assertEquals(expected = requestId, actual = decoded?.requestId)
        assertContentEquals(expected = audioData, actual = decoded?.audioChunk)
    }

    @Test
    fun `audio upload frame codec handles empty audio chunk`() {
        val requestId = "empty-chunk-req"
        val encoded = AudioUploadFrameCodec.encode(requestId = requestId, audioChunk = byteArrayOf())
        val decoded = AudioUploadFrameCodec.decode(frameBytes = encoded)

        assertEquals(expected = requestId, actual = decoded?.requestId)
        assertEquals(expected = 0, actual = decoded?.audioChunk?.size)
    }

    @Test
    fun `audio upload frame codec returns null on invalid or truncated frames`() {
        assertNull(AudioUploadFrameCodec.decode(byteArrayOf()))
        assertNull(AudioUploadFrameCodec.decode(byteArrayOf(0)))
        assertNull(AudioUploadFrameCodec.decode(byteArrayOf(0, 10, 65, 66))) // declared length 10, but only 2 bytes payload
    }

    @Test
    fun `vocabularySource transcribe start request serializes and deserializes polymorphically`() {
        val request: KlafServerClientMessage = VocabularySourceTranscribeStartRequest(
            requestId = "tx-1",
            sourceId = 42,
            fileName = "speech.mp3",
            audioFormat = "audio/mpeg",
            declaredByteSize = 1048576L,
        )

        val text = json.encodeToString(request)
        val decoded = json.decodeFromString<KlafServerClientMessage>(text)

        assertIs<VocabularySourceTranscribeStartRequest>(decoded)
        assertEquals("tx-1", decoded.requestId)
        assertEquals(42, decoded.sourceId)
        assertEquals("speech.mp3", decoded.fileName)
        assertEquals("audio/mpeg", decoded.audioFormat)
        assertEquals(1048576L, decoded.declaredByteSize)
    }

    @Test
    fun `vocabularySource transcribe complete request serializes and deserializes polymorphically`() {
        val request: KlafServerClientMessage = VocabularySourceTranscribeCompleteRequest(
            requestId = "tx-2",
        )

        val text = json.encodeToString(request)
        val decoded = json.decodeFromString<KlafServerClientMessage>(text)

        assertIs<VocabularySourceTranscribeCompleteRequest>(decoded)
        assertEquals("tx-2", decoded.requestId)
    }

    @Test
    fun `vocabularySource transcribe progress messages serialize and deserialize polymorphically`() {
        val uploadProgress: KlafServerMessage = VocabularySourceTranscribeUploadProgressMessage(
            requestId = "tx-3",
            uploadedBytes = 5000L,
            totalBytes = 10000L,
        )
        val uploadText = json.encodeToString(uploadProgress)
        val decodedUpload = json.decodeFromString<KlafServerMessage>(uploadText)
        assertIs<VocabularySourceTranscribeUploadProgressMessage>(decodedUpload)
        assertEquals(5000L, decodedUpload.uploadedBytes)
        assertEquals(10000L, decodedUpload.totalBytes)

        val recogProgress: KlafServerMessage = VocabularySourceTranscribeRecognitionProgressMessage(
            requestId = "tx-3",
            completedChunks = 2,
            totalChunks = 5,
        )
        val recogText = json.encodeToString(recogProgress)
        val decodedRecog = json.decodeFromString<KlafServerMessage>(recogText)
        assertIs<VocabularySourceTranscribeRecognitionProgressMessage>(decodedRecog)
        assertEquals(2, decodedRecog.completedChunks)
        assertEquals(5, decodedRecog.totalChunks)
    }

    @Test
    fun `vocabularySource transcribed message serializes and deserializes polymorphically`() {
        val transcribed: KlafServerMessage = VocabularySourceTranscribedMessage(
            requestId = "tx-4",
            transcript = "Full transcript text",
            segments = listOf(
                SpeechToTextSegmentDto(startMillis = 0L, endMillis = 1500L, text = "Full transcript"),
                SpeechToTextSegmentDto(startMillis = 1500L, endMillis = 3000L, text = "text"),
            ),
            serverNotificationSent = true,
        )

        val text = json.encodeToString(transcribed)
        val decoded = json.decodeFromString<KlafServerMessage>(text)

        assertIs<VocabularySourceTranscribedMessage>(decoded)
        assertEquals("tx-4", decoded.requestId)
        assertEquals("Full transcript text", decoded.transcript)
        assertEquals(2, decoded.segments.size)
        assertEquals(0L, decoded.segments[0].startMillis)
        assertEquals(1500L, decoded.segments[0].endMillis)
        assertEquals("Full transcript", decoded.segments[0].text)
        assertEquals(true, decoded.serverNotificationSent)
    }
}
