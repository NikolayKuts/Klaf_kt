package com.kuts.klaf.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DataStorePreference<T>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val defaultValue: T,
) {
    val flow: Flow<T> = dataStore.data.map { preferences ->
        preferences[key] ?: defaultValue
    }

    suspend fun get(): T = flow.first()

    suspend fun set(value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}

fun DataStore<Preferences>.booleanPreference(
    defaultValue: Boolean = false,
    keyName: String? = null,
): ReadOnlyProperty<Any?, DataStorePreference<Boolean>> = preference(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::booleanPreferencesKey,
)

fun DataStore<Preferences>.intPreference(
    defaultValue: Int = 0,
    keyName: String? = null,
): ReadOnlyProperty<Any?, DataStorePreference<Int>> = preference(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::intPreferencesKey,
)

fun DataStore<Preferences>.longPreference(
    defaultValue: Long = 0L,
    keyName: String? = null,
): ReadOnlyProperty<Any?, DataStorePreference<Long>> = preference(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::longPreferencesKey,
)

fun DataStore<Preferences>.floatPreference(
    defaultValue: Float = 0f,
    keyName: String? = null,
): ReadOnlyProperty<Any?, DataStorePreference<Float>> = preference(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::floatPreferencesKey,
)

fun DataStore<Preferences>.stringPreference(
    defaultValue: String = "",
    keyName: String? = null,
): ReadOnlyProperty<Any?, DataStorePreference<String>> = preference(
    defaultValue = defaultValue,
    keyName = keyName,
    keyFactory = ::stringPreferencesKey,
)

private fun <T> DataStore<Preferences>.preference(
    defaultValue: T,
    keyName: String?,
    keyFactory: (String) -> Preferences.Key<T>,
): ReadOnlyProperty<Any?, DataStorePreference<T>> = object : ReadOnlyProperty<Any?, DataStorePreference<T>> {
    private var cached: DataStorePreference<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): DataStorePreference<T> {
        cached?.let { return it }

        val resolvedKeyName = keyName ?: property.name
        return DataStorePreference(
            dataStore = this@preference,
            key = keyFactory(resolvedKeyName),
            defaultValue = defaultValue,
        ).also { cached = it }
    }
}
