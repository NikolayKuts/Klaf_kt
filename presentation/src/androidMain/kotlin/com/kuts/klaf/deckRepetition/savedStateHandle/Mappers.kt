package com.kuts.klaf.deckRepetition.savedStateHandle

import android.os.Bundle
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

inline fun <reified T> List<T>.serialized(key: String): Bundle = Bundle().apply {
    val toPut = map { item -> json.encodeToString(item) }
    putStringArrayList(key, ArrayList(toPut))
}

inline fun <reified T> Bundle.deserializedList(key: String): List<T> {
    return getStringArrayList(key)?.mapNotNull {
        json.decodeFromString<T>(it)
    } ?: emptyList()
}

inline fun <reified T> T?.serialized(key: String): Bundle = Bundle().apply {
    this@serialized ?: return@apply
    putString(key, json.encodeToString(this@serialized))
}

inline fun <reified T> Bundle.deserialized(key: String): T? = getString(key)?.let {
    json.decodeFromString<T>(it)
}