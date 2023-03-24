package com.example.klaf.presentation.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val inputState by viewModel.inputState.collectAsState()

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
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val filterColor = MainTheme.colors.commonColors.appLabelColorFilter

        Image(
            modifier = Modifier
                .size(70.dp)
                .weight(0.8F),
            painter = painterResource(id = R.drawable.color_10),
            contentDescription = null,
            colorFilter = ColorFilter.lighting(filterColor, filterColor),
            alignment = BiasAlignment(horizontalBias = 0F, verticalBias = 0.2F)
        )
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