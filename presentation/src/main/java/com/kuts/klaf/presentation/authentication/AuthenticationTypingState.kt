package com.kuts.klaf.presentation.authentication

data class AuthenticationTypingState(
    val emailHolder: TypingStateHolder = TypingStateHolder(),
    val passwordHolder: TypingStateHolder = TypingStateHolder(),
    val passwordConfirmationHolder: TypingStateHolder? = null,
)