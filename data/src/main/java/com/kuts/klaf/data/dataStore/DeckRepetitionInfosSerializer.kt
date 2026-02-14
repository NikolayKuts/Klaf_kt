package com.kuts.klaf.data.dataStore

import androidx.datastore.core.Serializer
import com.kuts.domain.entities.DeckRepetitionInfos
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object DeckRepetitionInfosSerializer : Serializer<DeckRepetitionInfos> {

    override val defaultValue: DeckRepetitionInfos = DeckRepetitionInfos()

    override suspend fun readFrom(input: InputStream): DeckRepetitionInfos {
        return Json.decodeFromString(
            deserializer = DeckRepetitionInfos.serializer(),
            string = input.readBytes().decodeToString()
        )
    }

    override suspend fun writeTo(t: DeckRepetitionInfos, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = DeckRepetitionInfos.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}