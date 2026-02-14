package com.kuts.klaf.data.networking.yandexApi.entities

import kotlinx.serialization.Serializable

@Serializable
data class YandexWordInfo(
    val def: List<Def>?,
    val head: Head?
)