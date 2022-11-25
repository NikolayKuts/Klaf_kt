package com.example.klaf.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DialogView(
    mainContent: @Composable BoxScope.() -> Unit,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    Box {
        Card(
            modifier = Modifier
                .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
                .padding(bottom = (DIALOG_BUTTON_SIZE / 2).dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(MainTheme.dimensions.dialogContentPadding)
                    .padding(bottom = (DIALOG_BUTTON_SIZE / 4).dp),
                content = mainContent
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(0.5F)
                .align(alignment = Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround,
            content = buttonContent
        )
    }
}