package com.example.klaf.presentation.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.ClosingButton
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun SigningTypeChoosingView(
    onSignInButtonClick: () -> Unit,
    onSignUpButtonClick: () -> Unit,
    onCloseButtonClick: () -> Unit
) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseButtonClick,
        topContent = {
            val filterColor = MainTheme.colors.commonColors.appLabelColorFilter

            Image(
                modifier = Modifier
                    .size(70.dp)
                    .clip(shape = RoundedCornerShape(50.dp))
                    .background(MainTheme.colors.dataSynchronizationViewColors.labelBackground)
                    .padding(10.dp),
                painter = painterResource(id = R.drawable.color_10),
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
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor =MainTheme.colors.commonColors.positiveDialogButton
                    ),
                    onClick = onSignInButtonClick
                ) {
                    Text(text = stringResource(id = R.string.authentication_sign_in_label))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MainTheme.colors.commonColors.positiveDialogButton
                    ),
                    onClick = onSignUpButtonClick
                ) {
                    Text(text = stringResource(id = R.string.authentication_sign_up_label))
                }
            }

        },
        bottomContent = {
            ClosingButton(onClick = onCloseButtonClick)
        },
    )
}