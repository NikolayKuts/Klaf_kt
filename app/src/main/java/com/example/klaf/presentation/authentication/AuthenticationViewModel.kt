package com.example.klaf.presentation.authentication

import androidx.lifecycle.viewModelScope
import com.example.domain.common.*
import com.example.domain.interactors.AuthenticationInteractor
import com.example.klaf.R
import com.example.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningInLoadingError
import com.example.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningInLoadingError.*
import com.example.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError
import com.example.klaf.presentation.authentication.EmailValidator.EmailValidationResult.*
import com.example.klaf.presentation.authentication.PasswordConfirmationValidator.PasswordConfirmationValidationResult
import com.example.klaf.presentation.authentication.PasswordConfirmationValidator.PasswordConfirmationValidationResult.NotIdentical
import com.example.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult
import com.example.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult.ToLong
import com.example.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult.ToShort
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val authenticationInteractor: AuthenticationInteractor,
) : BaseAuthenticationViewModel() {

    override val typingState = MutableStateFlow(
        value = AuthenticationTypingState(
            emailHolder = TypingStateHolder(),
            passwordHolder = TypingStateHolder(),
        )
    )

    override val screenLoadingState = MutableStateFlow<LoadingState<Unit>?>(value = null)

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

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
        val validationResult = manageValidation(email = email, password = password)

        validationResult.ifTrue {
            viewModelScope.launch {
                authenticationInteractor.signInWithEmailAndPassword(
                    email = email,
                    password = password
                ).collect { loadingState ->
                    screenLoadingState.value = loadingState

                    if (loadingState is LoadingState.Error) {
                        handleSigningInError(loadingState)
                    }
                }
            }
        }
    }

    override fun signUp() {
        val email = typingState.value.emailHolder.text.trim()
        val password = typingState.value.passwordHolder.text.trim()
        val passwordConfirmation = typingState.value.passwordConfirmationHolder?.text?.trim() ?: ""
        val validationResult = manageValidation(
            email = email,
            password = password,
            passwordConfirmation = passwordConfirmation
        )

        validationResult.ifTrue {
            viewModelScope.launch {
                authenticationInteractor.signUpWithEmailAndPassword(
                    email = email,
                    password = password
                ).collect { loadingState ->
                    screenLoadingState.value = loadingState

                    if (loadingState is LoadingState.Error) {
                        handleSigningUpError(loadingState = loadingState)
                    }
                }
            }
        }
    }

    private fun handleSigningInError(loadingState: LoadingState.Error) {
        val errorMessageId = when (val error = loadingState.value) {
            is SigningInLoadingError -> {
                when (error) {
                    CommonError -> R.string.authentication_warning_common_error_message
                    InvalidPassword -> R.string.authentication_warning_invalid_password
                    NetworkError -> R.string.authentication_warning_network_error
                    NoUserRecord -> R.string.authentication_warning_no_user_record
                }
            }
            else -> R.string.authentication_warning_common_error_message
        }
        eventMessage.tryEmit(messageId = errorMessageId)
    }

    private fun handleSigningUpError(loadingState: LoadingState.Error) {
        val errorMessageId = when (val error = loadingState.value) {
            is SigningUpLoadingError -> {
                when (error) {
                    SigningUpLoadingError.EmailAlreadyInUse -> {
                        R.string.authentication_warning_email_already_in_use_error
                    }
                    SigningUpLoadingError.NetworkError -> {
                        R.string.authentication_warning_network_error
                    }
                    SigningUpLoadingError.CommonError -> {
                        R.string.authentication_warning_common_error_message
                    }
                }
            }
            else -> R.string.authentication_warning_common_error_message
        }
        eventMessage.tryEmit(messageId = errorMessageId)
    }

    private fun manageValidation(
        email: String,
        password: String,
        passwordConfirmation: String? = null,
    ): Boolean {
        var isValid = true
        val emailValidationMessageId: Int? = getEmailValidationMessageId(email = email)
        val passwordValidationMessageId: Int? = getPasswordValidationMessageId(password = password)

        passwordConfirmation ifNotNull { confirmation ->
            val passwordConfirmationMessageId: Int? =
                getPasswordConfirmationMessageId(password = password, confirmation = confirmation)

            passwordConfirmationMessageId ifNotNull {
                eventMessage.tryEmit(messageId = it)
                isValid = false

                typingState.update { state ->
                    val passwordConfirmationHolder =
                        state.passwordConfirmationHolder?.copy(isError = true)
                            ?: TypingStateHolder(isError = true)

                    state.copy(passwordConfirmationHolder = passwordConfirmationHolder)
                }
            }
        }

        passwordValidationMessageId ifNotNull {
            eventMessage.tryEmit(messageId = it)
            isValid = false

            typingState.update { state ->
                val passwordHolder = state.passwordHolder.copy(isError = true)
                state.copy(passwordHolder = passwordHolder)
            }
        }

        emailValidationMessageId ifNotNull  {
            eventMessage.tryEmit(messageId = it)
            isValid = false

            typingState.update { state ->
                val emailHolder = state.emailHolder.copy(isError = true)
                state.copy(emailHolder = emailHolder)
            }
        }

        return isValid
    }

    private fun getEmailValidationMessageId(email: String): Int? {
        return when (EmailValidator().validate(data = email)) {
            Empty -> R.string.authentication_warning_type_email
            WrongFormat -> R.string.authentication_warning_invalid_email_format
            Valid -> null
        }
    }

    private fun getPasswordValidationMessageId(password: String): Int? {
        return when (PasswordValidator().validate(data = password)) {
            PasswordValidationResult.Empty -> R.string.authentication_warning_type_password
            ToLong -> R.string.authentication_warning_password_too_long
            ToShort -> R.string.authentication_warning_password_too_short
            PasswordValidationResult.Valid -> null
        }
    }

    private fun getPasswordConfirmationMessageId(password: String, confirmation: String): Int? {
        val confirmationState =
            PasswordConfirmationSate(password = password, confirmation = confirmation)

        return when (PasswordConfirmationValidator().validate(data = confirmationState)) {
            PasswordConfirmationValidationResult.Empty -> {
                R.string.authentication_warning_type_password_confirmation
            }
            NotIdentical -> R.string.authentication_warning_Invalid_password_confirmation
            PasswordConfirmationValidationResult.Valid -> null
        }
    }
}