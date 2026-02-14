package com.kuts.klaf.presentation.authentication

import android.os.Parcelable
import com.kuts.domain.common.AuthenticationAction
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class AuthenticationActionResult(
    val action: AuthenticationAction,
    val isSuccessful: Boolean = false
) : Parcelable
