package com.example.klaf.presentation.authentication

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import com.example.domain.common.*
import com.example.klaf.R
import com.example.klaf.presentation.authentication.AuthenticationAction.SIGN_IN
import com.example.klaf.presentation.authentication.AuthenticationAction.SIGN_UP
import com.example.klaf.presentation.common.AdaptiveBox
import com.example.klaf.presentation.common.ConfirmationButton
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme
import kotlinx.coroutines.delay

@Composable
fun AuthenticationScreen(
    viewModel: BaseAuthenticationViewModel,
    action: AuthenticationAction,
) {
    val inputState by viewModel.typingState.collectAsState()
    val loadingState by viewModel.screenLoadingState.collectAsState()

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

private const val ImageBox = "labelBox"
private const val AuthenticationActionLabelId = "AuthenticationActionLabel"
private const val EmailTextFieldId = "EmailTextField"
private const val PasswordTextFieldId = "PasswordTextField"
private const val PasswordConfirmationTextFieldId = "PasswordConfirmationTextField"
private const val ConfirmationButtonId = "ConfirmationButton"

@Composable
private fun getConstraints(): ConstraintSet = ConstraintSet {
    val imageBox = createRefFor(id = ImageBox)
    val passwordTextField = createRefFor(id = PasswordTextFieldId)
    val emailTextField = createRefFor(id = EmailTextFieldId)
    val authenticationActionLabel = createRefFor(id = AuthenticationActionLabelId)
    val passwordConfirmationTextField = createRefFor(id = PasswordConfirmationTextFieldId)
    val confirmationButton = createRefFor(id = ConfirmationButtonId)
    val guideLine = createGuidelineFromTop(fraction = 0.45F)

//    imageBox.
    constrain(ref = imageBox) {
        top.linkTo(parent.top)
        bottom.linkTo(authenticationActionLabel.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }

    constrain(ref = authenticationActionLabel) {
        top.linkTo(imageBox.bottom)
        bottom.linkTo(emailTextField.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = emailTextField) {
        bottom.linkTo(passwordTextField.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = passwordTextField) {
        top.linkTo(guideLine)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    constrain(ref = passwordConfirmationTextField) {
        top.linkTo(passwordTextField.bottom)
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
    var size by rememberAsMutableStateOf(value = 0.dp)
    val local = LocalDensity.current

    Box(
        modifier = Modifier
            .layoutId(ImageBox)
//                .weight(0.8F)
            .background(Color(0xFFAACE80))
            .padding(8.dp)
            .sizeIn(
                minWidth = 20.dp,
                minHeight = 20.dp,
                maxWidth = 100.dp,
                maxHeight = 100.dp,
            )
            .width(intrinsicSize = IntrinsicSize.Max),
        contentAlignment = Alignment.Center,
    ) {


        Image(
            modifier = Modifier
                .background(Color(0xFFDAA19D))
                .size(70.dp)
                .align(Alignment.Center)
                .onGloballyPositioned {
                    size = local.run { it.size.height.toDp() }
                },
            painter = painterResource(id = R.drawable.color_10),
            contentDescription = null,
            colorFilter = ColorFilter.lighting(filterColor, filterColor),
            alignment = BiasAlignment(horizontalBias = 0F, verticalBias = 0.2F)
        )

        isLoading.ifTrue {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x7576B8B1))
//                        .defaultMinSize(100.dp, 1000.dp)
//                        .fillMaxWidth()
//                        .sizeIn(
//                            maxWidth = 100.dp,
//                            maxHeight = 100.dp,
//                            minWidth = 30.dp,
//                            minHeight = 30.dp,
//                        )

//                        .width(intrinsicSize = IntrinsicSize.Max)
//                        .width(intrinsicSize = )
                    .size(size)
//                        .fillMaxSize()
//                        .size(100.dp)
            )
        }
    }

    Text(
        modifier = Modifier.layoutId(AuthenticationActionLabelId),
        text = actionLabelText,
        fontSize = 20.sp,
        fontStyle = FontStyle.Italic,
    )
//        Spacer(modifier = Modifier.height(30.dp))

    AuthenticationTextField(
        layoutId = EmailTextFieldId,
        value = typingState.emailHolder.text,
        onValueChange = onEmailChange,
        labelText = stringResource(R.string.authentication_email_label),
        isError = typingState.emailHolder.isError
    )

//        Spacer(modifier = Modifier.height(20.dp))

    AuthenticationTextField(
        layoutId = PasswordTextFieldId,
        value = typingState.passwordHolder.text,
        onValueChange = onPasswordChange,
        labelText = stringResource(R.string.authentication_password_label),
        isError = typingState.passwordHolder.isError
    )

    isPasswordConfirmationEnabled.ifTrue {
//            Spacer(modifier = Modifier.height(20.dp))

        AuthenticationTextField(
            layoutId = PasswordConfirmationTextFieldId,
            value = typingState.passwordConfirmationHolder?.text ?: "",
            onValueChange = onPasswordConfirmationChange,
            labelText = stringResource(R.string.authentication_password_confirmation),
            isError = typingState.passwordConfirmationHolder?.isError ?: false
        )
    }

    Box(
        modifier = Modifier
            .layoutId(ConfirmationButtonId)
//                .weight(1f)
        ,
        contentAlignment = BiasAlignment(horizontalBias = 0F, verticalBias = -0.2F)
    ) {
        ConfirmationButton(onClick = onConfirmationClick)
    }
//    }
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