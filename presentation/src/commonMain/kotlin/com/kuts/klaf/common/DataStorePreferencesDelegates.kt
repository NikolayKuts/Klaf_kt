package com.kuts.klaf.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun DataStore<Preferences>.booleanValue(
    defaultValue: Boolean = false,
    keyName: String? = null,
): ReadWriteProperty<Any?, Boolean> = valueDelegate(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::booleanPreferencesKey,
)

fun DataStore<Preferences>.intValue(
    defaultValue: Int = 0,
    keyName: String? = null,
): ReadWriteProperty<Any?, Int> = valueDelegate(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::intPreferencesKey,
)

fun DataStore<Preferences>.longValue(
    defaultValue: Long = 0L,
    keyName: String? = null,
): ReadWriteProperty<Any?, Long> = valueDelegate(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::longPreferencesKey,
)

fun DataStore<Preferences>.floatValue(
    defaultValue: Float = 0f,
    keyName: String? = null,
): ReadWriteProperty<Any?, Float> = valueDelegate(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::floatPreferencesKey,
)

fun DataStore<Preferences>.stringValue(
    defaultValue: String = "",
    keyName: String? = null,
): ReadWriteProperty<Any?, String> = valueDelegate(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::stringPreferencesKey,
)

private fun <T> DataStore<Preferences>.valueDelegate(
    defaultValue: T,
    keyName: String?,
    keyFactory: (String) -> Preferences.Key<T>,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    private var cachedKey: Preferences.Key<T>? = null

    private fun resolveKey(property: KProperty<*>): Preferences.Key<T> {
        return cachedKey ?: keyFactory(keyName ?: property.name).also { key ->
            cachedKey = key
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val key = resolveKey(property = property)
        return runBlocking {
            data.first()[key] ?: defaultValue
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val key = resolveKey(property = property)
        runBlocking {
            edit { preferences -> preferences[key] = value }
        }
    }
}
