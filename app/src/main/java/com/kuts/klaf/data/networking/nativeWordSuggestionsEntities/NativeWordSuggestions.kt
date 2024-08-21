package com.kuts.klaf.data.networking.nativeWordSuggestionsEntities

import kotlinx.serialization.Serializable

@Serializable
data class NativeWordSuggestions(
    val def: List<Def>?,
    val head: Head?
)