package com.kuts.domain.common

import kotlinx.serialization.Serializable

@Serializable
enum class AuthenticationAction { SIGN_IN, SIGN_UP }
