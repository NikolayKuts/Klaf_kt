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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.ClosingButton
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun SigningTypeChoosingView() {
    FullBackgroundDialog(
        onBackgroundClick = {},
        topContent = {
            Image(
                modifier = Modifier
                    .size(70.dp)
                    .clip(shape = RoundedCornerShape(50.dp))
                    .background(MainTheme.colors.dataSynchronizationViewColors.labelBackground)
                    .padding(10.dp),
                painter = painterResource(id = R.drawable.color_10),
                contentDescription = null,
                colorFilter = ColorFilter.lighting(
                    MainTheme.colors.appLabel,
                    MainTheme.colors.appLabel
                )
            )
        },
        mainContent = {
            Column {
                Text(text = "To continue synchronization you should do authentication")
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF809962)),
                    onClick = { /*TODO*/ }
                ) {
                    Text(text = "Sine in")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF809962)),
                    onClick = { /*TODO*/ }
                ) {
                    Text(text = "Sine up")
                }
            }

        },
        bottomContent = {
            ClosingButton {

            }
        },
    )
}