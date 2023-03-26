package com.example.klaf.presentation.authentication

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.common.LoadingState
import com.example.domain.common.ifTrue
import com.example.klaf.R
import com.example.klaf.presentation.authentication.AuthenticationAction.SIGN_IN
import com.example.klaf.presentation.authentication.AuthenticationAction.SIGN_UP
import com.example.klaf.presentation.common.ConfirmationButton
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun AuthenticationScreen(
    viewModel: BaseAuthenticationViewModel,
    action: AuthenticationAction,
) {
    val inputState by viewModel.typingState.collectAsState()
    val loadingState by viewModel.screenLoadingState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)
    ) {
        val (
            actionLabelTextId: Int,
            onConfirmationClick: () -> Unit
        ) = when (action) {
            SIGN_IN -> R.string.authentication_sign_in_label to viewModel::signIn
            SIGN_UP -> R.string.authentication_sign_up_label to viewModel::signUp
        }

        AuthenticationView(
            typingState = inputState,
            actionLabelText = stringResource(id = actionLabelTextId),
            isLoading = loadingState is LoadingState.Loading,
            onEmailChange = viewModel::updateEmail,
            onPasswordChange = viewModel::updatePassword,
            onConfirmationClick = onConfirmationClick,
        )
    }
}

@Composable
fun AuthenticationView(
    typingState: AuthenticationTypingState,
    actionLabelText: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val filterColor = getImageColor(isLoading = isLoading)

        Box(
            modifier = Modifier
                .size(100.dp)
                .weight(0.8F),
            contentAlignment = Alignment.Center,
        ) {
            isLoading.ifTrue { CircularProgressIndicator(modifier = Modifier.size(100.dp)) }
            Image(
                modifier = Modifier.size(70.dp),
                painter = painterResource(id = R.drawable.color_10),
                contentDescription = null,
                colorFilter = ColorFilter.lighting(filterColor, filterColor),
                alignment = BiasAlignment(horizontalBias = 0F, verticalBias = 0.2F)
            )
        }

        Text(
            text = actionLabelText,
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic,
        )
        Spacer(modifier = Modifier.height(30.dp))

        AuthenticationTextField(
            value = typingState.emailHolder.text,
            onValueChange = onEmailChange,
            labelText = stringResource(R.string.authentication_email_label),
            isError = typingState.emailHolder.isError
        )

        Spacer(modifier = Modifier.height(20.dp))

        AuthenticationTextField(
            value = typingState.passwordHolder.text,
            onValueChange = onPasswordChange,
            labelText = stringResource(R.string.authentication_password_label),
            isError = typingState.passwordHolder.isError
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = BiasAlignment(horizontalBias = 0F, verticalBias = -0.2F)
        ) {
            ConfirmationButton(onClick = onConfirmationClick)
        }
    }
}

@Composable
private fun AuthenticationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    isError: Boolean,
) {
    OutlinedTextField(
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