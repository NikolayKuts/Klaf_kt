package com.kuts.klaf.authentication

import com.kuts.domain.common.AuthenticationAction
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationActionResult(
    val action: AuthenticationAction,
    val isSuccessful: Boolean = false
)
