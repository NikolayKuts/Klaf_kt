package com.example.klaf.presentation.authentication

import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.LoadingState
import com.example.domain.common.ifFalse
import com.example.domain.common.ifTrue
import com.example.domain.interactors.AuthenticationInteractor
import com.example.klaf.R
import com.example.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.FirebaseLoadingError
import com.example.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.FirebaseLoadingError.*
import com.example.klaf.presentation.authentication.EmailValidator.EmailValidationResult.*
import com.example.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult
import com.example.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult.ToLong
import com.example.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult.ToShort
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
            passwordHolder = TypingStateHolder()
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
                        handleError(loadingState)
                    }
                }
            }
        }
    }

    private fun handleError(loadingState: LoadingState.Error) {
        val errorMessageId = when (val error = loadingState.value) {
            is FirebaseLoadingError -> {
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

    override fun signUp() {
        val email = typingState.value.emailHolder.text.trim()
        val password = typingState.value.passwordHolder.text.trim()
        val validationResult = manageValidation(email = email, password = password)

        validationResult.ifTrue {
            viewModelScope.launchWithState {
//                auth.createUserWithEmailAndPassword(email, password).await()
            } onException { _, error ->
                val errorMessageId = when (error) {
                    is FirebaseAuthUserCollisionException -> {
                        R.string.authentication_warning_email_already_in_use_error
                    }
                    is FirebaseNetworkException -> R.string.authentication_warning_network_error
                    else -> R.string.authentication_warning_common_error_message
                }

                eventMessage.tryEmit(messageId = errorMessageId)
            }
        }
    }

    private fun manageValidation(email: String, password: String): Boolean {
        var isEmailValid = false
        var isPasswordValid = false

        val emailValidationMessageId: Int? = when (EmailValidator().validate(data = email)) {
            Empty -> R.string.authentication_warning_type_email
            WrongFormat -> R.string.authentication_warning_invalid_email_format
            Valid -> {
                isEmailValid = true
                null
            }
        }

        val passwordValidationMessageId: Int? =
            when (PasswordValidator().validate(data = password)) {
                PasswordValidationResult.Empty -> R.string.authentication_warning_type_password
                ToLong -> R.string.authentication_warning_password_too_long
                ToShort -> R.string.authentication_warning_password_too_short
                PasswordValidationResult.Valid -> {
                    isPasswordValid = true
                    null
                }
            }

        passwordValidationMessageId?.let { eventMessage.tryEmit(messageId = it) }
        emailValidationMessageId?.let { eventMessage.tryEmit(messageId = it) }

        isEmailValid.ifFalse {
            typingState.update { state ->
                val emailHolder = state.emailHolder.copy(isError = true)
                state.copy(emailHolder = emailHolder)
            }
        }

        isPasswordValid.ifFalse {
            typingState.update { state ->
                val passwordHolder = state.passwordHolder.copy(isError = true)
                state.copy(passwordHolder = passwordHolder)
            }
        }

        return isEmailValid and isPasswordValid
    }
}