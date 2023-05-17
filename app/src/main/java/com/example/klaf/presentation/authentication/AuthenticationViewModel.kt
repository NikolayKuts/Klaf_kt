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
import com.example.klaf.presentation.common.tryEmitAsNegative
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
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
        MutableStateFlow<LoadingState<AuthenticationAction>>(value = LoadingState.Non)

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

    private fun handleSigningInError(loadingState: LoadingState.Error) {
        val errorMessageId = when (val error = loadingState.value) {
            is SigningInLoadingError -> {
                when (error) {
                    CommonError -> R.string.authentication_warning_common_error_message
                    NetworkError -> R.string.authentication_warning_network_error
                    InvalidPassword -> {
                        setErrorStateForPasswordHolder()
                        R.string.authentication_warning_invalid_password
                    }
                    NoUserRecord -> {
                        setErrorStateForEmailHolder()
                        R.string.authentication_warning_no_user_record
                    }
                }
            }
            else -> R.string.authentication_warning_common_error_message
        }

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
    }

    private fun handleSigningUpError(loadingState: LoadingState.Error) {
        val errorMessageId = when (val error = loadingState.value) {
            is SigningUpLoadingError -> {
                when (error) {
                    SigningUpLoadingError.EmailAlreadyInUse -> {
                        setErrorStateForEmailHolder()
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

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
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