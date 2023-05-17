package com.example.klaf.presentation.authentication

import android.os.Parcelable
import com.example.domain.common.AuthenticationAction
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationActionResult(
    val action: AuthenticationAction,
    val isSuccessful: Boolean = false
) : Parcelable
