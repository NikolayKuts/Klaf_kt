package com.kuts.klaf.navigation.navType

import androidx.navigation.NavType
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T : Any> serializableNavTypeMap(): Map<KType, NavType<*>> = mapOf(
    typeOf<T>() to SerializableNavType(serializer<T>()),
)
