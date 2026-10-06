package com.kuts.klaf.authentication

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.managers.AccountSignInResult
import com.kuts.domain.managers.AccountSignUpResult
import com.kuts.domain.managers.AccountEnrollmentKind
import com.kuts.domain.managers.AccountEnrollmentStatus
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.ImageSynchronizationException
import com.kuts.domain.managers.ImageSyncFailure
import com.kuts.domain.managers.IAccountSession
import com.kuts.domain.managers.AccountPasswordPolicy
import com.kuts.domain.repositories.IAuthenticationRepository.IAuthenticationError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningUpError
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsNeutral
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.authentication_warning_invalid_email_format
import com.kuts.klaf.presentation.resources.authentication_warning_type_email
import com.kuts.klaf.presentation.resources.authentication_warning_invalid_password
import com.kuts.klaf.presentation.resources.authentication_warning_invalid_password_confirmation
import com.kuts.klaf.presentation.resources.authentication_approval_awaiting
import com.kuts.klaf.presentation.resources.account_error_connection
import com.kuts.klaf.presentation.resources.account_error_timeout
import com.kuts.klaf.presentation.resources.account_error_not_found
import com.kuts.klaf.presentation.resources.account_error_exists
import com.kuts.klaf.presentation.resources.account_error_device
import com.kuts.klaf.presentation.resources.account_error_invalid_request
import com.kuts.klaf.presentation.resources.account_error_invalid_response
import com.kuts.klaf.presentation.resources.account_error_server
import com.kuts.klaf.presentation.resources.account_error_server_busy
import com.kuts.klaf.presentation.resources.account_error_throttled
import com.kuts.klaf.presentation.resources.account_error_throttled_wait
import com.kuts.klaf.presentation.resources.account_error_pending_signup
import com.kuts.klaf.presentation.resources.account_error_sign_in_required
import com.kuts.klaf.presentation.resources.account_error_invalid_credentials
import com.kuts.klaf.presentation.resources.account_error_approval_pending
import com.kuts.klaf.presentation.resources.account_error_device_proof
import com.kuts.klaf.presentation.resources.account_error_unknown
import com.kuts.klaf.presentation.resources.image_sync_error_id_reused
import com.kuts.klaf.presentation.resources.image_sync_error_invalid_file
import com.kuts.klaf.presentation.resources.authentication_reset_done
import com.kuts.klaf.presentation.resources.authentication_reset_token_required
import com.kuts.klaf.presentation.resources.authentication_reset_invalid
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun accountErrorMessage(failure: Throwable) = when ((failure as? AccountOperationException)?.failure) {
    AccountFailure.CONNECTION -> Res.string.account_error_connection
    AccountFailure.TIMEOUT -> Res.string.account_error_timeout
    AccountFailure.ACCOUNT_NOT_FOUND -> Res.string.account_error_not_found
    AccountFailure.ACCOUNT_EXISTS -> Res.string.account_error_exists
    AccountFailure.DEVICE_REGISTRATION -> Res.string.account_error_device
    AccountFailure.INVALID_REQUEST -> Res.string.account_error_invalid_request
    AccountFailure.INVALID_RESPONSE -> Res.string.account_error_invalid_response
    AccountFailure.SERVER -> Res.string.account_error_server
    AccountFailure.SERVER_BUSY -> Res.string.account_error_server_busy
    AccountFailure.THROTTLED -> Res.string.account_error_throttled
    AccountFailure.PENDING_SIGNUP -> Res.string.account_error_pending_signup
    AccountFailure.SIGN_IN_REQUIRED -> Res.string.account_error_sign_in_required
    AccountFailure.INVALID_CREDENTIALS -> Res.string.account_error_invalid_credentials
    AccountFailure.RESET_TOKEN_INVALID -> Res.string.authentication_reset_invalid
    AccountFailure.APPROVAL_PENDING -> Res.string.account_error_approval_pending
    AccountFailure.DEVICE_PROOF -> Res.string.account_error_device_proof
    AccountFailure.UNKNOWN, null -> Res.string.account_error_unknown
}

internal fun imageSyncErrorMessage(failure: ImageSynchronizationException) = when (failure.failure) {
    ImageSyncFailure.ID_REUSED -> Res.string.image_sync_error_id_reused
    ImageSyncFailure.INVALID_FILE -> Res.string.image_sync_error_invalid_file
}

/** Account UI validation; authenticated transport is implemented by [IAccountSession]. */
class AccountAuthenticationViewModel(
    private val accountSession: IAccountSession,
    private val coroutineContextProvider: ICoroutineContextProvider,
) : BaseAuthenticationViewModel() {

    override val isPasswordless = false
    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    override val typingState = MutableStateFlow(
        AuthenticationTypingState(
            emailHolder = TypingStateHolder(),
            passwordHolder = TypingStateHolder(),
            passwordConfirmationHolder = TypingStateHolder(),
        ),
    )
    override val screenLoadingState = MutableStateFlow<LoadingState<AuthenticationAction, IAuthenticationError>>(
        LoadingState.Non,
    )
    override val pendingDeviceRegistrationEmail = MutableStateFlow<String?>(null)
    override val pendingApprovalRequestId = MutableStateFlow<String?>(null)
    override val pendingApprovalStatus = MutableStateFlow<AccountEnrollmentStatus?>(null)
    override val passwordResetCompleted = MutableStateFlow(false)

    init {
        viewModelScope.launch(coroutineContextProvider.io) {
            accountSession.pendingEnrollment.collect { pending ->
                pendingApprovalRequestId.value = pending?.requestId
            }
        }
    }

    override fun updateEmail(value: String) {
        pendingDeviceRegistrationEmail.value = null
        typingState.update { state ->
            state.copy(emailHolder = state.emailHolder.copy(text = value.trim(), isError = false))
        }
    }

    override fun updatePassword(value: String) {
        typingState.update { state ->
            state.copy(passwordHolder = state.passwordHolder.copy(text = value, isError = false))
        }
    }

    override fun updatePasswordConfirmation(value: String) {
        typingState.update { state ->
            state.copy(passwordConfirmationHolder = state.passwordConfirmationHolder?.copy(text = value, isError = false))
        }
    }

    override fun signIn() {
        val email = validatedEmail() ?: return
        val password = validatedPassword() ?: return
        if (screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                when (val result = accountSession.signIn(email, password)) {
                    AccountSignInResult.SignedIn -> {
                        screenLoadingState.value = LoadingState.Success(AuthenticationAction.SIGN_IN)
                    }

                    is AccountSignInResult.DeviceRegistrationRequired -> {
                        pendingDeviceRegistrationEmail.value = result.email
                        screenLoadingState.value = LoadingState.Non
                    }

                    is AccountSignInResult.PendingApproval -> markPendingApproval(result.requestId)
                }
            } catch (failure: CancellationException) {
                screenLoadingState.value = LoadingState.Non
                throw failure
            } catch (failure: Exception) {
                reportError(ISigningInError.CommonError, failure)
            }
        }
    }

    override fun signUp() {
        val email = validatedEmail() ?: return
        val password = validatedPassword() ?: return
        if (typingState.value.passwordConfirmationHolder?.text != password) {
            typingState.update { state ->
                state.copy(passwordConfirmationHolder = state.passwordConfirmationHolder?.copy(isError = true))
            }
            eventMessage.tryEmitAsNegative(Res.string.authentication_warning_invalid_password_confirmation)
            return
        }
        if (screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                when (val result = accountSession.signUp(email, password)) {
                    is AccountSignUpResult.PendingApproval -> markPendingApproval(result.requestId)
                }
            } catch (failure: CancellationException) {
                screenLoadingState.value = LoadingState.Non
                throw failure
            } catch (failure: Exception) {
                reportError(ISigningUpError.CommonError, failure)
            }
        }
    }

    override fun registerDevice() {
        val email = pendingDeviceRegistrationEmail.value ?: return
        if (screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                accountSession.registerDevice(email)
                pendingDeviceRegistrationEmail.value = null
                screenLoadingState.value = LoadingState.Success(AuthenticationAction.SIGN_IN)
            } catch (failure: CancellationException) {
                screenLoadingState.value = LoadingState.Non
                throw failure
            } catch (failure: Exception) {
                reportError(ISigningInError.CommonError, failure)
            }
        }
    }

    override fun cancelDeviceRegistration() {
        pendingDeviceRegistrationEmail.value = null
    }

    override fun resetPassword(token: String, newPassword: String, confirmation: String) {
        val email = validatedEmail() ?: return
        if (token.isBlank()) {
            eventMessage.tryEmitAsNegative(Res.string.authentication_reset_token_required)
            return
        }
        if (!AccountPasswordPolicy.isValid(newPassword)) {
            eventMessage.tryEmitAsNegative(Res.string.authentication_warning_invalid_password)
            return
        }
        if (newPassword != confirmation) {
            eventMessage.tryEmitAsNegative(Res.string.authentication_warning_invalid_password_confirmation)
            return
        }
        if (screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                accountSession.resetPassword(email, token.trim(), newPassword)
                typingState.update { state -> state.copy(passwordHolder = state.passwordHolder.copy(text = "")) }
                passwordResetCompleted.value = true
                screenLoadingState.value = LoadingState.Non
                eventMessage.tryEmitAsNeutral(Res.string.authentication_reset_done)
            } catch (failure: CancellationException) {
                screenLoadingState.value = LoadingState.Non
                throw failure
            } catch (failure: Exception) {
                reportError(ISigningInError.CommonError, failure)
            }
        }
    }

    override fun checkPendingApproval() {
        if (pendingApprovalRequestId.value == null || screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                pendingApprovalStatus.value = accountSession.checkPendingEnrollment()
                screenLoadingState.value = LoadingState.Non
            } catch (failure: CancellationException) {
                screenLoadingState.value = LoadingState.Non
                throw failure
            } catch (failure: Exception) {
                reportError(ISigningInError.CommonError, failure)
            }
        }
    }

    override fun completePendingApproval() {
        if (pendingApprovalStatus.value != AccountEnrollmentStatus.APPROVED ||
            screenLoadingState.value is LoadingState.Loading) return
        val password = validatedPassword() ?: return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                val kind = accountSession.completePendingEnrollment(password)
                typingState.update { state -> state.copy(passwordHolder = state.passwordHolder.copy(text = "")) }
                pendingApprovalRequestId.value = null
                pendingApprovalStatus.value = null
                screenLoadingState.value = LoadingState.Success(when (kind) {
                    AccountEnrollmentKind.ACCOUNT -> AuthenticationAction.SIGN_UP
                    AccountEnrollmentKind.DEVICE -> AuthenticationAction.SIGN_IN
                })
            } catch (failure: CancellationException) {
                screenLoadingState.value = LoadingState.Non
                throw failure
            } catch (failure: Exception) {
                reportError(ISigningInError.CommonError, failure)
            }
        }
    }

    private fun markPendingApproval(requestId: String) {
        pendingApprovalRequestId.value = requestId
        pendingApprovalStatus.value = null
        typingState.update { state ->
            state.copy(
                passwordHolder = state.passwordHolder.copy(text = "", isError = false),
                passwordConfirmationHolder = state.passwordConfirmationHolder?.copy(text = "", isError = false),
            )
        }
        screenLoadingState.value = LoadingState.Non
        eventMessage.tryEmitAsNeutral(Res.string.authentication_approval_awaiting)
    }

    private fun validatedEmail(): String? {
        val email = typingState.value.emailHolder.text.trim()
        val warning = when (EmailValidator().validate(email)) {
            EmailValidator.IEmailValidationResult.Empty -> Res.string.authentication_warning_type_email
            EmailValidator.IEmailValidationResult.WrongFormat -> Res.string.authentication_warning_invalid_email_format
            EmailValidator.IEmailValidationResult.Valid -> return email
        }
        typingState.update { state -> state.copy(emailHolder = state.emailHolder.copy(isError = true)) }
        eventMessage.tryEmitAsNegative(warning)
        return null
    }

    private fun validatedPassword(): String? {
        val password = typingState.value.passwordHolder.text
        if (AccountPasswordPolicy.isValid(password)) return password
        typingState.update { state -> state.copy(passwordHolder = state.passwordHolder.copy(isError = true)) }
        eventMessage.tryEmitAsNegative(Res.string.authentication_warning_invalid_password)
        return null
    }

    private fun reportError(error: IAuthenticationError, failure: Exception) {
        screenLoadingState.value = LoadingState.Error(error)
        logE("Account operation failed: type=${failure::class.simpleName}, " +
            "reason=${(failure as? AccountOperationException)?.failure}, cause=${failure.cause?.let { it::class.simpleName }}")
        val operation = failure as? AccountOperationException
        val retryAfter = operation?.retryAfterSeconds
        if (operation?.failure == AccountFailure.THROTTLED && retryAfter != null) {
            eventMessage.tryEmitAsNegative(Res.string.account_error_throttled_wait, retryAfter)
        } else {
            eventMessage.tryEmitAsNegative(accountErrorMessage(failure))
        }
    }
}
