package com.kuts.klaf.cardManagement.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_youglish_logo
import com.kuts.klaf.presentation.resources.word_insights_youglish_action
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val AUTO_COLLAPSE_DELAY_MILLIS = 5_000L
private val COLLAPSED_BUTTON_WIDTH = 36.dp
private val EXPANDED_BUTTON_WIDTH = 112.dp
private val BUTTON_HEIGHT = 36.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpandableYouGlishButton(
    enabled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonWidth by animateDpAsState(
        targetValue = if (expanded) EXPANDED_BUTTON_WIDTH else COLLAPSED_BUTTON_WIDTH,
        animationSpec = spring(),
        label = "YouGlishButtonWidth",
    )
    val actionDescription = stringResource(resource = Res.string.word_insights_youglish_action)

    LaunchedEffect(expanded, enabled) {
        if (!enabled) {
            expanded = false
        } else if (expanded) {
            delay(AUTO_COLLAPSE_DELAY_MILLIS)
            expanded = false
        }
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .width(buttonWidth)
            .height(BUTTON_HEIGHT)
            .clip(RoundedCornerShape(BUTTON_HEIGHT))
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            )
            .combinedClickable(
                enabled = enabled,
                onClickLabel = actionDescription,
                role = Role.Button,
                onLongClickLabel = actionDescription,
                onClick = {
                    if (expanded) {
                        expanded = false
                        onOpen()
                    } else {
                        expanded = true
                    }
                },
                onLongClick = {
                    expanded = false
                    onOpen()
                },
            )
            .padding(horizontal = 7.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = expanded,
            label = "YouGlishButtonContent",
        ) { isExpanded ->
            if (isExpanded) {
                Image(
                    painter = painterResource(resource = Res.drawable.ic_youglish_logo),
                    contentDescription = actionDescription,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                )
            } else {
                Text(
                    text = "YG",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
