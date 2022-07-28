package com.example.klaf.presentation.deckList.deckDeleting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R


@Composable
fun DeckDeletionDialogView(
    deckName: String,
    onCloseDialogButtonClick: () -> Unit,
    onConfirmDeckDeletingButtonClick: () -> Unit,
) {
//    Column() {
    Box {
        val closeButtonSize = 50

        Card(
            modifier = Modifier
                .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
//                    .background(Color(0xFF56BB51))
                .padding(bottom = (closeButtonSize / 2).dp)
        ) {
            Box(modifier = Modifier.padding(32.dp)) {
                Text(
//                        modifier = Modifier.background(Color(0xFF57A29E)),
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle()) {
                            append("Would you like to delete ")
                        }
                        withStyle(SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp
                        )) {
                            append("\"${deckName}\"")
                        }
                        withStyle(SpanStyle()) {
                            append("?")
                        }
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(0.5F)
                .align(alignment = Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DeckDeletingConformationButton(onClick = onConfirmDeckDeletingButtonClick)
            DialogClosingButton(onClick = onCloseDialogButtonClick)
        }

    }
//    }
}

@Composable
private fun DeckDeletingConformationButton(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
            .size(50.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFFDB645B))
                .clickable { onClick() }
                .padding(8.dp),
            painter = painterResource(id = R.drawable.ic_delete_24),
            contentDescription = "",
        )
    }
}

@Composable
private fun DialogClosingButton(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
//            .align(alignment = Alignment.BottomCenter)
            .size(50.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFF85B9C0))
                .clickable { onClick() }
                .padding(8.dp),
            painter = painterResource(id = R.drawable.ic_close_24),
            contentDescription = "",
        )
    }
}