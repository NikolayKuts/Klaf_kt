package com.kuts.klaf.data.room.converters

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomDateConverter {

    @TypeConverter
    fun fromDateListToString(lastRepetitionDates: List<Long>): String {
        return Json.encodeToString(lastRepetitionDates)
    }

    @TypeConverter
    fun fromStringToDateList(lastRepetitionDatesAsString: String): List<Long> {
        return Json.decodeFromString(lastRepetitionDatesAsString)
    }

}