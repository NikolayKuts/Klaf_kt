package com.kuts.klaf.navigation.navType

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

internal class EnumNavType<E : Enum<E>>(
    private val enumValues: Array<E>,
) : NavType<E>(isNullableAllowed = false) {

    override val name: String = "enum"

    override fun put(
        bundle: SavedState,
        key: String,
        value: E,
    ) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(
        bundle: SavedState,
        key: String,
    ): E? {
        return bundle.read { getString(key) }.let(::parseValue)
    }

    override fun parseValue(value: String): E {
        return enumValues.firstOrNull { constant ->
            constant.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException("Enum value $value not found for type $name.")
    }

    override fun serializeAsValue(value: E): String {
        return value.name
    }
}
