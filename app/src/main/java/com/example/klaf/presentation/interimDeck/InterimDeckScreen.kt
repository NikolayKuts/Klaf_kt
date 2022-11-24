package com.example.klaf.presentation.interimDeck

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun InterimDeckScreen(viewModel: BaseInterimDeckViewModel) {
    val deck = viewModel.interimDeck.collectAsState(initial = null).value ?: return
    val cardHolders by viewModel.cardHolders.collectAsState()
    val clickState = rememberAsMutableStateOf(value = false)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Header()

            QuantityPointers(
                total = deck.cardQuantity.toString(),
                selected = cardHolders.filter { it.isSelected }.size.toString()
            )

            Spacer(modifier = Modifier.height(28.dp))
            DividingLine()

            LazyColumn {
                itemsIndexed(items = cardHolders) { index, holder ->
                    CardItem(
                        holder = holder,
                        position = index + 1,
                        onSelectedChanged = { viewModel.changeSelectionState(position = index) },
                    )
                    DividingLine()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        ManagementButtons(clickState = clickState)
    }
}

@Composable
private fun ColumnScope.Header(
) {
    Text(
        modifier = Modifier
            .align(alignment = Alignment.CenterHorizontally)
            .padding(top = 8.dp, bottom = 8.dp),
        text = stringResource(id = R.string.Interim_deck_name),
        style = MainTheme.typographies.viewingCardDeckName
    )
}

@Composable
private fun QuantityPointers(
    total: String,
    selected: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(Color(0x4B707070))
                .padding(start = 4.dp, end = 4.dp),
            text = stringResource(R.string.pointer_interim_deck_cards),
            fontStyle = FontStyle.Italic,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.pointer_interim_deck_total))
            Text(
                text = total,
                fontStyle = FontStyle.Italic,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.pointer_interim_deck_selected))
            Text(
                text = selected,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun CardItem(
    holder: SelectableCardHolder,
    position: Int,
    onSelectedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        Text(
            text = position.toString(),
            color = Color(0xFF525252),
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = holder.card.foreignWord,
            color = Color(0xFF93B46A)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1F),
            text = holder.card.nativeWord,
        )
        Checkbox(
            checked = holder.isSelected,
            onCheckedChange = onSelectedChanged,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFA9D378))
        )
    }
}

@Composable
private fun DividingLine() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp),
        color = Color(0xF1636262),
    )
}

@Composable
private fun BoxScope.ManagementButtons(clickState: MutableState<Boolean>) {
    Box(
        modifier = Modifier.align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 48.dp),
    ) {
        with(clickState) {
            CardMovingButton(isStartPosition = value)
            CardAddingButton(isStartPosition = value)
            CardDeletingButton(isStartPosition = value)
            RoundButton(
                background = MainTheme.colors.materialColors.primary,
                iconId = R.drawable.ic_more_vert_24,
                onClick = { value = !value }
            )
        }
    }
}

@Composable
private fun CardMovingButton(
    isStartPosition: Boolean,
) {
    AnimatableOffsetButton(
        isStartPosition = isStartPosition,
        offset = -180F,
        background = Color(0xFF679AB1),
        iconId = R.drawable.ic_move_24,
        stiffness = Spring.StiffnessVeryLow,
        onClick = {}
    )
}

@Composable
private fun CardAddingButton(isStartPosition: Boolean) {
    AnimatableOffsetButton(
        isStartPosition = isStartPosition,
        offset = -120F,
        background = Color(0xFFA8CE7D),
        iconId = R.drawable.ic_add_24,
        stiffness = Spring.StiffnessLow,
        onClick = { /*TODO*/ })
}

@Composable
private fun CardDeletingButton(isStartPosition: Boolean) {
    AnimatableOffsetButton(
        isStartPosition = isStartPosition,
        offset = -60F,
        background = Color(0xFFCE756E),
        iconId = R.drawable.ic_delete_24,
        stiffness = Spring.StiffnessMediumLow,
        onClick = { /*TODO*/ })
}

@Composable
private fun AnimatableOffsetButton(
    isStartPosition: Boolean,
    offset: Float,
    background: Color,
    @DrawableRes iconId: Int,
    onClick: () -> Unit,
    rotationDegrees: Float = 180F,
    stiffness: Float = Spring.StiffnessVeryLow,
    elevation: Dp = 4.dp,
) {
    val transition = updateTransition(targetState = isStartPosition, label = null)

    val xOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = stiffness)
        },
        label = ""
    ) { state -> if (state) offset else 0F }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800) },
        label = ""
    ) { if (it) 1F else 0F }

    val degrees by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { state -> if (state) 0F else rotationDegrees }

    RoundButton(
        modifier = Modifier
            .offset(y = xOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = background,
        iconId = iconId,
        onClick = onClick,
        elevation = elevation
    )
}