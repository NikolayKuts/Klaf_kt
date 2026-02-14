package com.kuts.klaf.presentation.authentication

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.AuthenticationAction.SIGN_IN
import com.kuts.domain.common.AuthenticationAction.SIGN_UP
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.presentation.R
import com.kuts.klaf.presentation.common.AdaptiveScalableBox
import com.kuts.klaf.presentation.common.ConfirmationButton
import com.kuts.klaf.presentation.common.ROUNDED_ELEMENT_SIZE
import com.kuts.klaf.presentation.theme.MainTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AuthenticationScreen(
    viewModel: BaseAuthenticationViewModel,
    action: AuthenticationAction,
    onAuthenticationFinished: (authenticationAction: AuthenticationAction) -> Unit,
) {
    val inputState by viewModel.typingState.collectAsState()
    val loadingState = viewModel.screenLoadingState.collectAsState().value
    val keyboardController = LocalSoftwareKeyboardController.current

    AdaptiveScalableBox { adaptiveModifier ->
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

        Box(modifier = adaptiveModifier.padding(horizontal = 16.dp)) {
            AuthenticationView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
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

        LaunchedEffect(key1 = loadingState) {
            if (loadingState is LoadingState.Success<AuthenticationAction>) {
                keyboardController?.hide()
                onAuthenticationFinished(loadingState.data)
            }
        }
    }
}

@Composable
private fun AuthenticationView(
    modifier: Modifier = Modifier,
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(70.dp),
            painter = painterResource(id = R.drawable.ic_app_labale),
            contentDescription = null,
            colorFilter = ColorFilter.lighting(filterColor, filterColor),
            alignment = BiasAlignment(horizontalBias = 0F, verticalBias = 0.2F)
        )
        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = actionLabelText,
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic,
        )
        Spacer(modifier = Modifier.size(24.dp))

        AuthenticationTextField(
            modifier = Modifier.fillMaxWidth(),
            value = typingState.emailHolder.text,
            onValueChange = onEmailChange,
            labelText = stringResource(R.string.authentication_email_label),
            isError = typingState.emailHolder.isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.size(8.dp))

        AuthenticationTextField(
            modifier = Modifier.fillMaxWidth(),
            value = typingState.passwordHolder.text,
            onValueChange = onPasswordChange,
            labelText = stringResource(R.string.authentication_password_label),
            isError = typingState.passwordHolder.isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )

        isPasswordConfirmationEnabled.ifTrue {
            Spacer(modifier = Modifier.size(8.dp))
            AuthenticationTextField(
                modifier = Modifier.fillMaxWidth(),
                value = typingState.passwordConfirmationHolder?.text ?: "",
                onValueChange = onPasswordConfirmationChange,
                labelText = stringResource(R.string.authentication_password_confirmation),
                isError = typingState.passwordConfirmationHolder?.isError ?: false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(ROUNDED_ELEMENT_SIZE.dp)
                )
            } else {
                ConfirmationButton(onClick = onConfirmationClick)
            }
        }
    }
}

@Composable
private fun AuthenticationTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = labelText, fontStyle = FontStyle.Italic) },
        singleLine = true,
        isError = isError,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MainTheme.colors.authenticationScreen.textFieldBackground,
            unfocusedContainerColor = MainTheme.colors.authenticationScreen.textFieldBackground,
            focusedLabelColor = MainTheme.colors.common.focusedLabelColor,
        ),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
    )
}

@Composable
private fun getImageColor(isLoading: Boolean): Color = if (isLoading) {
    rememberInfiniteTransition().animateColor(
        initialValue = MainTheme.colors.common.appLabelColorFilter,
        targetValue = MainTheme.colors.common.animationAppLabelColorFilter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value
} else {
    MainTheme.colors.common.appLabelColorFilter
}
