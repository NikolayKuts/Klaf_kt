package com.kuts.klaf.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource as mppPainterResource

const val ROUNDED_ELEMENT_SIZE = 50

@Composable
fun RoundButton(
    background: Color,
    iconRes: DrawableResource,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ROUNDED_ELEMENT_SIZE.dp,
    contentDescription: String = "",
    elevation: Dp = 0.dp,
    contentColor: Color? = null,
) {
    Card(
        shape = RoundedCornerShape(size),
        modifier = modifier.size(size),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = if (contentColor == null) {
            CardDefaults.cardColors(containerColor = background)
        } else {
            CardDefaults.cardColors(containerColor = background, contentColor = contentColor)
        },
    ) {
        Icon(
            modifier = Modifier
                .size(size)
                .clickable(enabled = enabled) { onClick() }
                .padding(8.dp),
            painter = mppPainterResource(resource = iconRes),
            contentDescription = contentDescription,
            tint = contentColor ?: LocalContentColor.current,
        )
    }
}

@Composable
fun RoundedIcon(
    background: Color,
    iconRes: DrawableResource,
    modifier: Modifier = Modifier,
    size: Dp = ROUNDED_ELEMENT_SIZE.dp,
    contentDescription: String = "",
    elevation: Dp = 0.dp,
    contentColor: Color? = null,
) {
    Card(
        shape = RoundedCornerShape(size),
        modifier = modifier
            .size(size),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = if (contentColor == null) {
            CardDefaults.cardColors(containerColor = background)
        } else {
            CardDefaults.cardColors(containerColor = background, contentColor = contentColor)
        },
    ) {
        Icon(
            modifier = Modifier
                .size(size)
                .padding(8.dp),
            painter = mppPainterResource(resource = iconRes),
            contentDescription = contentDescription,
            tint = contentColor ?: LocalContentColor.current,
        )
    }
}
