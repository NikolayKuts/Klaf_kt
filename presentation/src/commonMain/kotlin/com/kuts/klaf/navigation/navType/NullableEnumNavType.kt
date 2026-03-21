package com.kuts.klaf.navigation.navType

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

internal class NullableEnumNavType<E : Enum<E>>(
    private val enumValues: Array<E>,
) : NavType<E?>(isNullableAllowed = true) {

    override val name: String = "enum?"

    override fun put(
        bundle: SavedState,
        key: String,
        value: E?,
    ) {
        bundle.write {
            if (value == null) putNull(key) else putString(key, value.name)
        }
    }

    override fun get(
        bundle: SavedState,
        key: String,
    ): E? {
        val rawValue = bundle.read {
            if (!contains(key) || isNull(key)) null else getString(key)
        }
        return rawValue?.let(::parseEnum)
    }

    override fun parseValue(value: String): E? {
        return if (value == "null") null else parseEnum(value)
    }

    override fun serializeAsValue(value: E?): String {
        return value?.name ?: "null"
    }

    private fun parseEnum(value: String): E {
        return enumValues.firstOrNull { constant ->
            constant.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException("Enum value $value not found for type $name.")
    }
}
