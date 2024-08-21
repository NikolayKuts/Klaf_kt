package com.kuts.klaf.data.networking

import com.kuts.domain.entities.WordInfo
import com.kuts.klaf.data.networking.yandexApi.entities.YandexWordInfo

fun YandexWordInfo.toDomainEntity(): WordInfo = WordInfo(
    transcription = def?.firstOrNull()?.ts ?: "",
    translations = def?.flatMap { def -> def.tr.map { tr -> tr.text } } ?: emptyList()
)