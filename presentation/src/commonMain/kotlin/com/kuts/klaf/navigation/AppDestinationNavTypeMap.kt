package com.kuts.klaf.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal class EnumNavType<E : Enum<E>>(
    private val enumValues: Array<E>,
) : NavType<E>(isNullableAllowed = false) {

    override val name: String = "enum"

    override fun put(
        bundle: SavedState,
        key: String,
        value: E
    ) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(
        bundle: SavedState,
        key: String
    ): E? {
        return bundle.read { getString(key) }?.let(::parseValue)
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

internal class NullableEnumNavType<E : Enum<E>>(
    private val enumValues: Array<E>,
) : NavType<E?>(isNullableAllowed = true) {

    override val name: String = "enum?"

    override fun put(bundle: SavedState, key: String, value: E?) {
        bundle.write {
            if (value == null) putNull(key) else putString(key, value.name)
        }
    }

    override fun get(bundle: SavedState, key: String): E? {
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

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified E : Enum<E>> enumNavTypeMap(
    includeNullable: Boolean = false,
): Map<KType, NavType<*>> = buildMap {
    val enumValues = enumValues<E>()
    put(typeOf<E>(), EnumNavType(enumValues))

    if (includeNullable) {
        put(typeOf<E?>(), NullableEnumNavType(enumValues))
    }
}
