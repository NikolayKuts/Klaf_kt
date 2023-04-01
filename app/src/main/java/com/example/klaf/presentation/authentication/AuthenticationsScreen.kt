package com.example.klaf.presentation.authentication

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import com.example.domain.common.*
import com.example.klaf.R
import com.example.klaf.presentation.authentication.AuthenticationAction.SIGN_IN
import com.example.klaf.presentation.authentication.AuthenticationAction.SIGN_UP
import com.example.klaf.presentation.common.AdaptiveBox
import com.example.klaf.presentation.common.ConfirmationButton
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.theme.MainTheme

private const val LABEL_ID = "labelBox"
private const val AUTHENTICATION_ACTION_LABEL_ID = "AuthenticationActionLabel"
private const val EMAIL_TEXT_FIELD_ID = "EmailTextField"
private const val PASSWORD_TEXT_FIELD_ID = "PasswordTextField"
private const val PASSWORD_CONFIRMATION_TEXT_FIELD_ID = "PasswordConfirmationTextField"
private const val CONFIRMATION_BUTTON_ID = "ConfirmationButton"

@Composable
fun AuthenticationScreen(
    viewModel: BaseAuthenticationViewModel,
    action: AuthenticationAction,
    onAuthenticationFinished: () -> Unit,
) {
    val inputState by viewModel.typingState.collectAsState()
    val loadingState by viewModel.screenLoadingState.collectAsState()

    if (loadingState is LoadingState.Success) {
        onAuthenticationFinished()
    }

    AdaptiveBox { adaptiveModifier ->
        ConstraintLayout(
            constraintSet = getConstraints(),
            modifier = adaptiveModifier
        ) {
            val (
                actionLabelTextId: Int,
                onConfirmationClick: () -> Unit,
                isPasswordConfirmationEnabled: Boolean,
            ) = when (action) {
                SIGN_IN -> {
                    Triple(
                        first = R.string.authentication_sign_in_label,
                        second = viewModel::signIn,
                        third = false
                    )
                }
                SIGN_UP -> {
                    Triple(
                        first = R.string.authentication_sign_up_label,
                        second = viewModel::signUp,
                        third = true
                    )
                }
            }

            AuthenticationView(
                typingState = inputState,
                actionLabelText = stringResource(id = actionLabelTextId),
                isLoading = loadingState is LoadingState.Loading,
                isPasswordConfirmationEnabled = isPasswordConfirmationEnabled,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onPasswordConfirmationChange = viewModel::updatePasswordConfirmation,
                onConfirmationClick = onConfirmationClick,
            )
        }
    }
}

@Composable
private fun getConstraints(): ConstraintSet = ConstraintSet {
    val imageBox = createRefFor(id = LABEL_ID)
    val passwordTextField = createRefFor(id = PASSWORD_TEXT_FIELD_ID)
    val emailTextField = createRefFor(id = EMAIL_TEXT_FIELD_ID)
    val authenticationActionLabel = createRefFor(id = AUTHENTICATION_ACTION_LABEL_ID)
    val passwordConfirmationTextField = createRefFor(id = PASSWORD_CONFIRMATION_TEXT_FIELD_ID)
    val confirmationButton = createRefFor(id = CONFIRMATION_BUTTON_ID)
    val guideLine = createGuidelineFromTop(fraction = 0.45F)

    val imageBoxMargin = 8.dp
    val authenticationActionLabelMargin = 6.dp
    val textFieldMargin = 8.dp

    constrain(ref = imageBox) {
        top.linkTo(parent.top, margin = imageBoxMargin)
        bottom.linkTo(authenticationActionLabel.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }

    constrain(ref = authenticationActionLabel) {
        top.linkTo(imageBox.bottom, margin = authenticationActionLabelMargin)
        bottom.linkTo(emailTextField.top, margin = authenticationActionLabelMargin)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = emailTextField) {
        bottom.linkTo(passwordTextField.top, margin = textFieldMargin)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = passwordTextField) {
        top.linkTo(guideLine)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = passwordConfirmationTextField) {
        top.linkTo(passwordTextField.bottom, margin = textFieldMargin)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = confirmationButton) {
        top.linkTo(passwordConfirmationTextField.bottom)
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
}

@Composable
fun AuthenticationView(
    typingState: AuthenticationTypingState,
    actionLabelText: String,
    isLoading: Boolean,
    isPasswordConfirmationEnabled: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onConfirmationClick: () -> Unit,
) {
    val filterColor = getImageColor(isLoading = isLoading)

    Image(
        modifier = Modifier
            .layoutId(LABEL_ID)
            .size(70.dp),
        painter = painterResource(id = R.drawable.color_10),
        contentDescription = null,
        colorFilter = ColorFilter.lighting(filterColor, filterColor),
        alignment = BiasAlignment(horizontalBias = 0F, verticalBias = 0.2F)
    )

    Text(
        modifier = Modifier.layoutId(AUTHENTICATION_ACTION_LABEL_ID),
        text = actionLabelText,
        fontSize = 20.sp,
        fontStyle = FontStyle.Italic,
    )

    AuthenticationTextField(
        layoutId = EMAIL_TEXT_FIELD_ID,
        value = typingState.emailHolder.text,
        onValueChange = onEmailChange,
        labelText = stringResource(R.string.authentication_email_label),
        isError = typingState.emailHolder.isError
    )

    AuthenticationTextField(
        layoutId = PASSWORD_TEXT_FIELD_ID,
        value = typingState.passwordHolder.text,
        onValueChange = onPasswordChange,
        labelText = stringResource(R.string.authentication_password_label),
        isError = typingState.passwordHolder.isError
    )

    isPasswordConfirmationEnabled.ifTrue {
        AuthenticationTextField(
            layoutId = PASSWORD_CONFIRMATION_TEXT_FIELD_ID,
            value = typingState.passwordConfirmationHolder?.text ?: "",
            onValueChange = onPasswordConfirmationChange,
            labelText = stringResource(R.string.authentication_password_confirmation),
            isError = typingState.passwordConfirmationHolder?.isError ?: false
        )
    }

    Box(
        modifier = Modifier.layoutId(CONFIRMATION_BUTTON_ID),
        contentAlignment = BiasAlignment(horizontalBias = 0F, verticalBias = -0.2F)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(DIALOG_BUTTON_SIZE.dp)
            )
        } else {
            ConfirmationButton(onClick = onConfirmationClick)
        }
    }
}

@Composable
private fun AuthenticationTextField(
    layoutId: String,
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    isError: Boolean,
) {
    OutlinedTextField(
        modifier = Modifier.layoutId(layoutId),
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = labelText, fontStyle = FontStyle.Italic) },
        singleLine = true,
        isError = isError,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.authenticationScreenColors.textFieldBackground,
            focusedLabelColor = MainTheme.colors.commonColors.focusedLabelColor,
        )
    )
}

@Composable
private fun getImageColor(isLoading: Boolean): Color = if (isLoading) {
    rememberInfiniteTransition().animateColor(
        initialValue = MainTheme.colors.commonColors.appLabelColorFilter,
        targetValue = MainTheme.colors.commonColors.animationAppLabelColorFilter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value
} else {
    MainTheme.colors.commonColors.appLabelColorFilter
}