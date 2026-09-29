package com.kuts.klaf.server.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val nullableOmittingVocabularyJson = Json { explicitNulls = false }

class VocabularySyncProtocolTest {

    @Test
    fun `a new source with omitted optional analysis date decodes on the server`() {
        val operation: SyncOperation = SyncOperation.UpsertVocabularySource("operation-a",
            SyncVocabularySource("source-a", "Title", "", "", "text", "text", 1, 1, 2, null))
        assertEquals(operation, Json.decodeFromString<SyncOperation>(nullableOmittingVocabularyJson.encodeToString(operation)))
    }

    @Test
    fun `source word with omitted optional card deck and cefr links decodes on the server`() {
        val item = SyncVocabularySourceItem("item-a", "en", "word", "", "meaning", "", "UNKNOWN", null,
            "MEDIUM", "NEW", "PENDING", "", "", "", false, "[]", null, null, 0, false, 1, 2)
        assertEquals(item, Json.decodeFromString<SyncVocabularySourceItem>(nullableOmittingVocabularyJson.encodeToString(item)))
    }
}
