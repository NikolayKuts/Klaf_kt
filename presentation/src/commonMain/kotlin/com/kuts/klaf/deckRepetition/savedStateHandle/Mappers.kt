package com.kuts.klaf.deckRepetition.savedStateHandle

import com.kuts.domain.common.UnitSurrogate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val json = Json {
    serializersModule = SerializersModule {
        polymorphic(Any::class) {
            subclass(UnitSurrogate::class, UnitSurrogate.serializer())
        }
    }
}

inline fun <reified T> T.serialized(): String = json.encodeToString(this)

inline fun <reified T> String.deserialized(): T = json.decodeFromString(this)
