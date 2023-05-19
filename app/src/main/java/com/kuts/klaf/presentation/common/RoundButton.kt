package com.kuts.klaf.presentation.common

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val ROUNDED_ELEMENT_SIZE = 50

@Composable
fun RoundButton(
    background: Color,
    @DrawableRes iconId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    elevation: Dp = 0.dp,
) {
    Card(
        shape = RoundedCornerShape(ROUNDED_ELEMENT_SIZE.dp),
        modifier = modifier.size(ROUNDED_ELEMENT_SIZE.dp),
        elevation = elevation,
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

@Composable
fun RoundedIcon(
    background: Color,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    elevation: Dp = 0.dp,
) {
    Card(
        shape = RoundedCornerShape(ROUNDED_ELEMENT_SIZE.dp),
        modifier = modifier
            .size(ROUNDED_ELEMENT_SIZE.dp),
        elevation = elevation,
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .background(background)
                .padding(8.dp),
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
        )
    }
}