package com.kuts.klaf.authentication

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.common.ifTrue
import com.kuts.domain.common.launchIn
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.IAuthenticationRepository.IAuthenticationError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError.CommonError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError.InvalidPassword
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError.NetworkError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError.NoUserRecord
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningUpError
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.authentication.EmailValidator.IEmailValidationResult.Empty
import com.kuts.klaf.authentication.EmailValidator.IEmailValidationResult.Valid
import com.kuts.klaf.authentication.EmailValidator.IEmailValidationResult.WrongFormat
import com.kuts.klaf.authentication.PasswordConfirmationValidator.IPasswordConfirmationValidationResult
import com.kuts.klaf.authentication.PasswordConfirmationValidator.IPasswordConfirmationValidationResult.NotIdentical
import com.kuts.klaf.authentication.PasswordValidator.IPasswordValidationResult
import com.kuts.klaf.authentication.PasswordValidator.IPasswordValidationResult.ToLong
import com.kuts.klaf.authentication.PasswordValidator.IPasswordValidationResult.ToShort
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

class AuthenticationViewModel(
    private val authenticationInteractor: AuthenticationInteractor,
) : BaseAuthenticationViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val typingState = MutableStateFlow(
        value = AuthenticationTypingState(
            emailHolder = TypingStateHolder(),
            passwordHolder = TypingStateHolder(),
        )
    )

    override val screenLoadingState =
        MutableStateFlow<LoadingState<AuthenticationAction, IAuthenticationError>>(value = LoadingState.Non)

    override fun updateEmail(value: String) {
        typingState.update { state ->
            val emailHolder = state.emailHolder.copy(text = value.trim(), isError = false)
            state.copy(emailHolder = emailHolder)
        }
    }

    override fun updatePassword(value: String) {
        typingState.update { state ->
            val passwordHolder = state.passwordHolder.copy(text = value.trim(), isError = false)
            state.copy(passwordHolder = passwordHolder)
        }
    }

    override fun updatePasswordConfirmation(value: String) {
        typingState.update { state ->
            val passwordConfirmationHolder =
                state.passwordConfirmationHolder?.copy(text = value.trim(), isError = false)
                    ?: TypingStateHolder(text = value)

            state.copy(passwordConfirmationHolder = passwordConfirmationHolder)
        }
    }

    override fun signIn() {
        val email = typingState.value.emailHolder.text.trim()
        val password = typingState.value.passwordHolder.text.trim()
        val isInputValid = manageValidation(email = email, password = password)

        isInputValid.ifTrue {
            authenticationInteractor.signInWithEmailAndPassword(
                email = email,
                password = password
            ).onEach { loadingState ->
                if (loadingState is LoadingState.Error) {
                    handleSigningInError(loadingState)
                }

                screenLoadingState.value = loadingState
            }.launchIn(scope = viewModelScope, context = Dispatchers.IO)
        }
    }

    override fun signUp() {
        val email = typingState.value.emailHolder.text.trim()
        val password = typingState.value.passwordHolder.text.trim()
        val passwordConfirmation = typingState.value.passwordConfirmationHolder?.text?.trim() ?: ""
        val isInputValid = manageValidation(
            email = email,
            password = password,
            passwordConfirmation = passwordConfirmation
        )

        isInputValid.ifTrue {
            authenticationInteractor.signUpWithEmailAndPassword(
                email = email,
                password = password
            ).onEach { loadingState ->
                if (loadingState is LoadingState.Error) {
                    handleSigningUpError(loadingState = loadingState)
                }

                screenLoadingState.value = loadingState
            }.launchIn(scope = viewModelScope, context = Dispatchers.IO)
        }
    }

    private fun handleSigningInError(loadingState: LoadingState.Error<IAuthenticationError>) {
        logE("Sign-in failed with state error: ${loadingState.value}")
        val errorMessageId = when (val error = loadingState.value) {
            is ISigningInError -> {
                when (error) {
                    CommonError -> Res.string.authentication_warning_common_error_message
                    NetworkError -> Res.string.authentication_warning_network_error
                    InvalidPassword -> {
                        setErrorStateForPasswordHolder()
                        Res.string.authentication_warning_invalid_password
                    }

                    NoUserRecord -> {
                        setErrorStateForEmailHolder()
                        Res.string.authentication_warning_no_user_record
                    }
                }
            }

            else -> Res.string.authentication_warning_common_error_message
        }

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
    }

    private fun handleSigningUpError(loadingState: LoadingState.Error<IAuthenticationError>) {
        logE("Sign-up failed with state error: ${loadingState.value}")
        val errorMessageId = when (val error = loadingState.value) {
            is ISigningUpError -> {
                when (error) {
                    ISigningUpError.EmailAlreadyInUse -> {
                        setErrorStateForEmailHolder()
                        Res.string.authentication_warning_email_already_in_use_error
                    }

                    ISigningUpError.NetworkError -> {
                        Res.string.authentication_warning_network_error
                    }

                    ISigningUpError.CommonError -> {
                        Res.string.authentication_warning_common_error_message
                    }
                }
            }

            else -> Res.string.authentication_warning_common_error_message
        }

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
    }

    private fun manageValidation(
        email: String,
        password: String,
        passwordConfirmation: String? = null,
    ): Boolean {
        var isValid = true
        val emailValidationMessageId: StringResource? = getEmailValidationMessageId(email = email)
        val passwordValidationMessageId: StringResource? =
            getPasswordValidationMessageId(password = password)

        passwordConfirmation ifNotNull { confirmation ->
            val passwordConfirmationMessageId: StringResource? =
                getPasswordConfirmationMessageId(password = password, confirmation = confirmation)

            passwordConfirmationMessageId ifNotNull {
                eventMessage.tryEmitAsNegative(resId = it)
                isValid = false
                setErrorStateForPasswordConfirmationHolder()
            }
        }

        passwordValidationMessageId ifNotNull {
            eventMessage.tryEmitAsNegative(resId = it)
            isValid = false
            setErrorStateForPasswordHolder()
        }

        emailValidationMessageId ifNotNull {
            eventMessage.tryEmitAsNegative(resId = it)
            isValid = false
            setErrorStateForEmailHolder()
        }

        return isValid
    }

    private fun getEmailValidationMessageId(email: String): StringResource? {
        return when (EmailValidator().validate(data = email)) {
            Empty -> Res.string.authentication_warning_type_email
            WrongFormat -> Res.string.authentication_warning_invalid_email_format
            Valid -> null
        }
    }

    private fun getPasswordValidationMessageId(password: String): StringResource? {
        return when (PasswordValidator().validate(data = password)) {
            IPasswordValidationResult.Empty -> Res.string.authentication_warning_type_password
            ToLong -> Res.string.authentication_warning_password_too_long
            ToShort -> Res.string.authentication_warning_password_too_short
            IPasswordValidationResult.Valid -> null
        }
    }

    private fun getPasswordConfirmationMessageId(
        password: String,
        confirmation: String,
    ): StringResource? {
        val confirmationState =
            PasswordConfirmationSate(password = password, confirmation = confirmation)

        return when (PasswordConfirmationValidator().validate(data = confirmationState)) {
            IPasswordConfirmationValidationResult.Empty -> {
                Res.string.authentication_warning_type_password_confirmation
            }

            NotIdentical -> Res.string.authentication_warning_invalid_password_confirmation
            IPasswordConfirmationValidationResult.Valid -> null
        }
    }

    private fun setErrorStateForEmailHolder() {
        typingState.update { state ->
            val emailHolder = state.emailHolder.copy(isError = true)
            state.copy(emailHolder = emailHolder)
        }
    }

    private fun setErrorStateForPasswordHolder() {
        typingState.update { state ->
            val passwordHolder = state.passwordHolder.copy(isError = true)
            state.copy(passwordHolder = passwordHolder)
        }
    }

    private fun setErrorStateForPasswordConfirmationHolder() {
        typingState.update { state ->
            val passwordConfirmationHolder =
                state.passwordConfirmationHolder?.copy(isError = true)
                    ?: TypingStateHolder(isError = true)

            state.copy(passwordConfirmationHolder = passwordConfirmationHolder)
        }
    }
}
