package com.example.klaf.presentation.deckList.sygningTypeChoosing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.ClosingButton
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.ScrollableBox
import com.example.klaf.presentation.common.noRippleClickable
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun SigningTypeChoosingView(
    onSignInButtonClick: () -> Unit,
    onSignUpButtonClick: () -> Unit,
    onCloseButtonClick: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseButtonClick() },
        dialogMode = true,
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCloseButtonClick,
            topContent = {
                val filterColor = MainTheme.colors.common.appLabelColorFilter

                Image(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(shape = RoundedCornerShape(50.dp))
                        .background(MainTheme.colors.dataSynchronizationView.initialLabelBackground)
                        .padding(10.dp),
                    painter = painterResource(id = R.drawable.ic_app_labale),
                    contentDescription = null,
                    colorFilter = ColorFilter.lighting(filterColor, filterColor)
                )
            },
            mainContent = {
                Column {
                    Text(
                        style = MainTheme.typographies.dialogTextStyle,
                        text = stringResource(R.string.authentication_sync_data_message),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SigningButton(
                        text = stringResource(id = R.string.authentication_sign_in_label),
                        onClick = onSignInButtonClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SigningButton(
                        text = stringResource(id = R.string.authentication_sign_up_label),
                        onClick = onSignUpButtonClick
                    )
                }
            },
            bottomContent = {
                ClosingButton(onClick = onCloseButtonClick)
            },
        )
    }
}

@Composable
private fun SigningButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.small)
            .background(MainTheme.colors.common.positiveDialogButton)
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            )
            .clickable { onClick() }
            .padding(ButtonDefaults.ContentPadding),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button
        )
    }
}