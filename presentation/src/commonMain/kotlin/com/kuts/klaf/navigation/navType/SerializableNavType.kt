package com.kuts.klaf.navigation.navType

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal class SerializableNavType<T : Any>(
    private val serializer: KSerializer<T>,
    private val json: Json = Json,
) : NavType<T>(isNullableAllowed = false) {

    override val name: String = "serializable"

    override fun put(
        bundle: SavedState,
        key: String,
        value: T,
    ) {
        bundle.write { putString(key, json.encodeToString(serializer, value)) }
    }

    override fun get(
        bundle: SavedState,
        key: String,
    ): T? {
        return bundle.read { getString(key) }.let { serializedValue ->
            json.decodeFromString(serializer, serializedValue)
        }
    }

    override fun parseValue(value: String): T {
        return json.decodeFromString(serializer, value.decodeNavValue())
    }

    override fun serializeAsValue(value: T): String {
        return json.encodeToString(serializer, value).encodeNavValue()
    }
}
