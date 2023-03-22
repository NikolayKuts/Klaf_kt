package com.example.klaf.presentation.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R
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
        when (action) {
            AuthenticationAction.SIGN_IN -> {
                SignInView(
                    typingState = inputState,
                    onEmailChange = { email -> viewModel.updateEmail(value = email) },
                    onPasswordChange = { password -> viewModel.updatePassword(value = password) },
                    onEnterClick = { viewModel.signIn() },
                )
            }
            AuthenticationAction.SIGN_UP -> {
                SignUpView(
                    typingState = inputState,
                    onEmailChange = { email -> viewModel.updateEmail(value = email) },
                    onPasswordChange = { password -> viewModel.updatePassword(value = password) },
                    onRejisterClick = { viewModel.signUp() },
                )
            }
        }
    }
}

@Composable
private fun SignInView(
    typingState: AuthenticationTypingState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEnterClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val color = MainTheme.colors.appLabel
        Image(
            modifier = Modifier.size(70.dp),
            painter = painterResource(id = R.drawable.color_10),
            contentDescription = null,
            colorFilter = ColorFilter.lighting(color, color)
        )
        Spacer(modifier = Modifier.height(70.dp))
        Text(
//            modifier = Modifier.fillMaxWidth(),
            text = "Sign in",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = typingState.emailHolder.text,
            onValueChange = onEmailChange,
            label = { Text(text = "email")},
            placeholder = { Text(text = "email") },
            singleLine = true,
            isError = typingState.emailHolder.isError,

//            shape = RoundedCornerShape(50.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = typingState.passwordHolder.text,
            onValueChange = onPasswordChange,
            label = { Text(text = "password")},
            placeholder = { Text(text = "password") },
            isError = typingState.passwordHolder.isError,
        )
        Spacer(modifier = Modifier.height(30.dp))

        Button(onClick = onEnterClick) {
            Text(text = "Enter")
        }
    }
}

@Composable
private fun SignUpView(
    typingState: AuthenticationTypingState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRejisterClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(70.dp),
            painter = painterResource(id = R.drawable.color_10),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
//            modifier = Modifier.fillMaxWidth(),
            text = "Sing up",
            fontSize = 30.sp
        )
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = typingState.emailHolder.text,
            onValueChange = onEmailChange,
            label = { Text(text = "email")},
            placeholder = { Text(text = "email") },
            singleLine = true,
            isError = typingState.emailHolder.isError,

//            shape = RoundedCornerShape(50.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = typingState.passwordHolder.text,
            onValueChange = onPasswordChange,
            label = { Text(text = "password")},
            placeholder = { Text(text = "password") },
            isError = typingState.passwordHolder.isError,
        )
        Spacer(modifier = Modifier.height(30.dp))

        Button(onClick = onRejisterClick) {
            Text(text = "Register")
        }
    }
}