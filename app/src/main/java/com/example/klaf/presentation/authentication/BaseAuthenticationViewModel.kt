package com.example.klaf.presentation.authentication

import androidx.lifecycle.ViewModel
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseAuthenticationViewModel : ViewModel(), EventMessageSource {

    abstract val inputState: StateFlow<AuthenticationTypingState>

    abstract fun updateEmail(value: String)
    abstract fun updatePassword(value: String)
    abstract fun signIn()
    abstract fun signUp()
}