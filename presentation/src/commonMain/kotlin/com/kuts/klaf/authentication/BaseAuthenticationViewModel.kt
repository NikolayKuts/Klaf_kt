package com.kuts.klaf.authentication

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.repositories.IAuthenticationRepository.IAuthenticationError
import com.kuts.domain.managers.AccountEnrollmentStatus
import com.kuts.klaf.common.IEventMessageSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseAuthenticationViewModel : ViewModel(), IEventMessageSource {

    open val isPasswordless: Boolean = false
    open val pendingDeviceRegistrationEmail: StateFlow<String?> = MutableStateFlow(null)
    open val pendingApprovalRequestId: StateFlow<String?> = MutableStateFlow(null)
    open val pendingApprovalStatus: StateFlow<AccountEnrollmentStatus?> = MutableStateFlow(null)
    open val passwordResetCompleted: StateFlow<Boolean> = MutableStateFlow(false)

    abstract val typingState: StateFlow<AuthenticationTypingState>
    abstract val screenLoadingState: StateFlow<LoadingState<AuthenticationAction, IAuthenticationError>>

    abstract fun updateEmail(value: String)
    abstract fun updatePassword(value: String)
    abstract fun updatePasswordConfirmation(value: String)
    abstract fun signIn()
    abstract fun signUp()

    open fun registerDevice() = Unit
    open fun cancelDeviceRegistration() = Unit
    open fun checkPendingApproval() = Unit
    open fun completePendingApproval() = Unit
    open fun resetPassword(token: String, newPassword: String, confirmation: String) = Unit
}
