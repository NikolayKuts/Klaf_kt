package com.kuts.klaf.networking.yandexApi.entities

import kotlinx.serialization.Serializable

@Serializable
data class YandexWordInfo(
    val def: List<Def>?,
    val head: Head?
)