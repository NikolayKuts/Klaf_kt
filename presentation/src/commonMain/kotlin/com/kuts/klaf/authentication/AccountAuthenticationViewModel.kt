package com.kuts.klaf.authentication

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.managers.AccountSignInResult
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.ImageSynchronizationException
import com.kuts.domain.managers.ImageSyncFailure
import com.kuts.domain.managers.IAccountSession
import com.kuts.domain.repositories.IAuthenticationRepository.IAuthenticationError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningUpError
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.authentication_warning_invalid_email_format
import com.kuts.klaf.presentation.resources.authentication_warning_type_email
import com.kuts.klaf.presentation.resources.account_error_connection
import com.kuts.klaf.presentation.resources.account_error_timeout
import com.kuts.klaf.presentation.resources.account_error_not_found
import com.kuts.klaf.presentation.resources.account_error_exists
import com.kuts.klaf.presentation.resources.account_error_device
import com.kuts.klaf.presentation.resources.account_error_invalid_request
import com.kuts.klaf.presentation.resources.account_error_invalid_response
import com.kuts.klaf.presentation.resources.account_error_server
import com.kuts.klaf.presentation.resources.account_error_pending_signup
import com.kuts.klaf.presentation.resources.account_error_unknown
import com.kuts.klaf.presentation.resources.image_sync_error_id_reused
import com.kuts.klaf.presentation.resources.image_sync_error_invalid_file
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
    AccountFailure.PENDING_SIGNUP -> Res.string.account_error_pending_signup
    AccountFailure.UNKNOWN, null -> Res.string.account_error_unknown
}

internal fun imageSyncErrorMessage(failure: ImageSynchronizationException) = when (failure.failure) {
    ImageSyncFailure.ID_REUSED -> Res.string.image_sync_error_id_reused
    ImageSyncFailure.INVALID_FILE -> Res.string.image_sync_error_invalid_file
}

/** Passwordless account UI logic; bind only after scoped app repositories replace the legacy ones. */
class AccountAuthenticationViewModel(
    private val accountSession: IAccountSession,
    private val coroutineContextProvider: ICoroutineContextProvider,
) : BaseAuthenticationViewModel() {

    override val isPasswordless = true
    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    override val typingState = MutableStateFlow(
        AuthenticationTypingState(
            emailHolder = TypingStateHolder(),
            passwordHolder = TypingStateHolder(),
        ),
    )
    override val screenLoadingState = MutableStateFlow<LoadingState<AuthenticationAction, IAuthenticationError>>(
        LoadingState.Non,
    )
    override val pendingDeviceRegistrationEmail = MutableStateFlow<String?>(null)

    override fun updateEmail(value: String) {
        pendingDeviceRegistrationEmail.value = null
        typingState.update { state ->
            state.copy(emailHolder = state.emailHolder.copy(text = value.trim(), isError = false))
        }
    }

    override fun updatePassword(value: String) = Unit

    override fun updatePasswordConfirmation(value: String) = Unit

    override fun signIn() {
        val email = validatedEmail() ?: return
        if (screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                when (val result = accountSession.signIn(email)) {
                    AccountSignInResult.SignedIn -> {
                        screenLoadingState.value = LoadingState.Success(AuthenticationAction.SIGN_IN)
                    }

                    is AccountSignInResult.DeviceRegistrationRequired -> {
                        pendingDeviceRegistrationEmail.value = result.email
                        screenLoadingState.value = LoadingState.Non
                    }
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
        if (screenLoadingState.value is LoadingState.Loading) return
        screenLoadingState.value = LoadingState.Loading
        viewModelScope.launch(coroutineContextProvider.io) {
            try {
                accountSession.signUp(email)
                screenLoadingState.value = LoadingState.Success(AuthenticationAction.SIGN_UP)
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

    private fun reportError(error: IAuthenticationError, failure: Exception) {
        screenLoadingState.value = LoadingState.Error(error)
        logE("Account operation failed: type=${failure::class.simpleName}, " +
            "reason=${(failure as? AccountOperationException)?.failure}, cause=${failure.cause?.let { it::class.simpleName }}")
        eventMessage.tryEmitAsNegative(accountErrorMessage(failure))
    }
}
