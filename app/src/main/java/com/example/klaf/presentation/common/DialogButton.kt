package com.example.klaf.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

const val DIALOG_BUTTON_SIZE = 50

@Composable
fun DialogButton(
    background: Color,
    @DrawableRes iconId: Int,
    onClick: () -> Unit,
    contentDescription: String = "",
) {
    Card(
        shape = RoundedCornerShape(DIALOG_BUTTON_SIZE.dp),
        modifier = Modifier
            .size(DIALOG_BUTTON_SIZE.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .background(background)
                .clickable { onClick() }
                .padding(8.dp),
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
        )
    }
}