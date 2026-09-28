package com.kuts.klaf.room.converters

import androidx.room.TypeConverter
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.common.WordMeaningInsightsPayload
import com.kuts.klaf.common.toDomainEntity
import com.kuts.klaf.common.toPayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomDateConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
    private val importJson = Json {
        ignoreUnknownKeys = false
        isLenient = true
        explicitNulls = false
    }

    @TypeConverter
    fun fromDateListToString(lastRepetitionDates: List<Long>): String {
        return json.encodeToString(lastRepetitionDates)
    }

    @TypeConverter
    fun fromStringToDateList(lastRepetitionDatesAsString: String): List<Long> {
        return json.decodeFromString(lastRepetitionDatesAsString)
    }

    @TypeConverter
    fun fromWordMeaningInsightsToString(insights: WordMeaningInsights): String {
        return json.encodeToString(insights.toPayload())
    }

    @TypeConverter
    fun fromStringToWordMeaningInsights(insightsAsJson: String): WordMeaningInsights {
        if (insightsAsJson.isBlank()) return WordMeaningInsights.EMPTY

        decodeInsightsOrNull(payload = insightsAsJson)?.let { return it }

        val unwrappedPayload = runCatching {
            json.decodeFromString<String>(insightsAsJson)
        }.getOrNull()

        if (unwrappedPayload != null) {
            decodeInsightsOrNull(payload = unwrappedPayload)?.let { return it }
        }

        return WordMeaningInsights.EMPTY
    }

    fun fromStringToWordMeaningInsightsStrict(insightsAsJson: String): WordMeaningInsights =
        json.decodeFromString<WordMeaningInsightsPayload>(insightsAsJson).toDomainEntity()

    /** Migration must fail rather than silently replace unrecognized legacy details with EMPTY. */
    fun fromStringToWordMeaningInsightsForImport(insightsAsJson: String): WordMeaningInsights {
        if (insightsAsJson.isBlank()) return WordMeaningInsights.EMPTY
        val direct = runCatching { importJson.decodeFromString<WordMeaningInsightsPayload>(insightsAsJson) }.getOrNull()
        val payload = direct ?: runCatching {
            val unwrapped = importJson.decodeFromString<String>(insightsAsJson)
            importJson.decodeFromString<WordMeaningInsightsPayload>(unwrapped)
        }.getOrElse { throw IllegalArgumentException("Unrecognized word meaning insights", it) }
        require(payload.isWordValid && payload.invalidReason.isBlank()) {
            "Legacy word validity details cannot be represented without loss"
        }
        return payload.toDomainEntity()
    }

    private fun decodeInsightsOrNull(payload: String): WordMeaningInsights? {
        return runCatching {
            json.decodeFromString<WordMeaningInsightsPayload>(payload).toDomainEntity()
        }.getOrNull()
    }
}
