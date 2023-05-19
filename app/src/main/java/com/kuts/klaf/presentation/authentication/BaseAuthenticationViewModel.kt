package com.kuts.klaf.presentation.authentication

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.StateFlow

abstract class BaseAuthenticationViewModel : ViewModel(), EventMessageSource {

    abstract val typingState: StateFlow<AuthenticationTypingState>
    abstract val screenLoadingState: StateFlow<LoadingState<AuthenticationAction>>

    abstract fun updateEmail(value: String)
    abstract fun updatePassword(value: String)
    abstract fun updatePasswordConfirmation(value: String)
    abstract fun signIn()
    abstract fun signUp()
}