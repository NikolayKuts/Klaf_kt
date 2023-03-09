package com.example.domain.ipa

import kotlinx.serialization.Serializable

@Serializable
data class IpaHolder(
    val letter: String,
    val ipa: String,
    val letterIndex: Int,
)