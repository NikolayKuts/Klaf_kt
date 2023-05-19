package com.kuts.klaf.presentation.authentication

import android.os.Parcelable
import com.kuts.domain.common.AuthenticationAction
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationActionResult(
    val action: AuthenticationAction,
    val isSuccessful: Boolean = false
) : Parcelable
