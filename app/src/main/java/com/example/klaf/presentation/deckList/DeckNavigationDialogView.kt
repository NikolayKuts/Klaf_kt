package com.example.klaf.presentation.deckList

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R


@Composable
fun DeckNavigationDialogView(
    deckName: String,
    deckId: Int,
    onDeleteDeckClick: () -> Unit,
    onRenameDeckClick: () -> Unit,
    onBrowseDeckClick: () -> Unit,
    onAddCardsClick: () -> Unit,
    onCloseDialogClick: () -> Unit,
) {
    Column() {
        Box {
            val closeButtonSize = 50

            Card(
                modifier = Modifier
                    .defaultMinSize(minHeight = 300.dp, minWidth = 300.dp)
                    .padding(bottom = (closeButtonSize / 2).dp)
            ) {
                Column(
                    Modifier.padding(32.dp)
                ) {
                    Text(
                        text = deckName,
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Delete deck",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeleteDeckClick() }
                    )
                    Spacer()
                    Text(
                        text = "Rename deck",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRenameDeckClick() }
                    )
                    Spacer()
                    Text(
                        text = "Browse cards",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBrowseDeckClick() })
                    Spacer()
                    Text(
                        text = "Add cards",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddCardsClick() }
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .align(alignment = Alignment.BottomCenter)
                    .size(closeButtonSize.dp),
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color(0xFFDB645B))
                        .clickable { onCloseDialogClick() }
                        .padding(8.dp),
                    painter = painterResource(id = R.drawable.ic_close_24),
                    contentDescription = "",
                )
            }
        }
    }
}

@Composable
private fun Spacer() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .height(1.dp)
            .background(Color(0x2DFFFFFF))
    )
}