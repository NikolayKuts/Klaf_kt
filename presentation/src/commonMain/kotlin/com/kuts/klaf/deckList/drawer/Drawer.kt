package com.kuts.klaf.deckList.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kuts.domain.entities.CodexObserverSessionState
import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.account_deleting_action
import com.kuts.klaf.presentation.resources.drawer_word_insights_source_codex_observer
import com.kuts.klaf.presentation.resources.drawer_word_insights_source_label
import com.kuts.klaf.presentation.resources.drawer_word_insights_source_open_ai
import com.kuts.klaf.presentation.resources.drawer_word_insights_status_connecting
import com.kuts.klaf.presentation.resources.drawer_word_insights_status_not_ready
import com.kuts.klaf.presentation.resources.drawer_word_insights_status_ready
import com.kuts.klaf.presentation.resources.ic_account_24
import com.kuts.klaf.presentation.resources.ic_delete_account_24
import com.kuts.klaf.presentation.resources.ic_login_24
import com.kuts.klaf.presentation.resources.ic_logout_24
import com.kuts.klaf.presentation.resources.log_in_action
import com.kuts.klaf.presentation.resources.log_in_negative_state
import com.kuts.klaf.presentation.resources.log_out_action
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Drawer(
    state: DrawerViewState,
    onLogInClick: () -> Unit,
    onLogOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onWordInsightsProviderChange: (WordInsightsProvider) -> Unit,
) {
    val rightCorners = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    BoxWithConstraints {
        val drawerWidth = maxWidth / 2
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clip(rightCorners)
                .widthIn(min = drawerWidth)
                .width(IntrinsicSize.Max)
                .background(color = MainTheme.colors.deckListScreen.drawerColors.contentBackground)
        ) {
            Header(
                signedIn = state.signedIn,
                email = state.userEmail,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .width(IntrinsicSize.Max)
                    .padding(16.dp)
            ) {
                WordInsightsProviderSection(
                    state = state,
                    onProviderChange = onWordInsightsProviderChange,
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.signedIn) {
                    DrawerItem(
                        iconRes = Res.drawable.ic_logout_24,
                        text = stringResource(resource = Res.string.log_out_action),
                        onClick = onLogOutClick,
                    )
                    DrawerItem(
                        iconRes = Res.drawable.ic_delete_account_24,
                        text = stringResource(resource = Res.string.account_deleting_action),
                        onClick = onDeleteAccountClick,
                    )
                } else {
                    DrawerItem(
                        iconRes = Res.drawable.ic_login_24,
                        text = stringResource(resource = Res.string.log_in_action),
                        onClick = onLogInClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun WordInsightsProviderSection(
    state: DrawerViewState,
    onProviderChange: (WordInsightsProvider) -> Unit,
) {
    val isOpenAiSelected = state.wordInsightsProvider == WordInsightsProvider.OpenAi

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(resource = Res.string.drawer_word_insights_source_label),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(size = 12.dp))
                .clickable {
                    onProviderChange(
                        if (isOpenAiSelected) {
                            WordInsightsProvider.CodexObserver
                        } else {
                            WordInsightsProvider.OpenAi
                        },
                    )
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOpenAiSelected) {
                        stringResource(resource = Res.string.drawer_word_insights_source_open_ai)
                    } else {
                        stringResource(resource = Res.string.drawer_word_insights_source_codex_observer)
                    },
                )

                if (!isOpenAiSelected) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = codexStatusText(state = state),
                        color = codexStatusColor(state = state),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Switch(
                checked = isOpenAiSelected,
                onCheckedChange = { isChecked ->
                    onProviderChange(
                        if (isChecked) {
                            WordInsightsProvider.OpenAi
                        } else {
                            WordInsightsProvider.CodexObserver
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun codexStatusText(state: DrawerViewState): String {
    return when (val sessionState = state.codexObserverSessionState) {
        CodexObserverSessionState.Disconnected -> {
            stringResource(resource = Res.string.drawer_word_insights_status_not_ready)
        }

        CodexObserverSessionState.Connecting -> {
            stringResource(resource = Res.string.drawer_word_insights_status_connecting)
        }

        CodexObserverSessionState.Ready -> {
            stringResource(resource = Res.string.drawer_word_insights_status_ready)
        }

        is CodexObserverSessionState.Error -> sessionState.message
    }
}

@Composable
private fun codexStatusColor(state: DrawerViewState) = when (state.codexObserverSessionState) {
    CodexObserverSessionState.Ready -> MainTheme.colors.deckListScreen.drawerColors.profileIconPositiveTint
    is CodexObserverSessionState.Error -> MainTheme.colors.deckListScreen.drawerColors.profileIconNegativeTint
    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
}

@Composable
private fun Header(
    signedIn: Boolean,
    email: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MainTheme.colors.deckListScreen.drawerColors.headerBackground)
            .padding(16.dp)
            .heightIn(min = 100.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        val iconTint = if (signedIn) {
            MainTheme.colors.deckListScreen.drawerColors.profileIconPositiveTint
        } else {
            MainTheme.colors.deckListScreen.drawerColors.profileIconNegativeTint
        }

        Column {
            Icon(
                modifier = Modifier
                    .size(70.dp)
                    .padding(bottom = 8.dp),
                painter = painterResource(resource = Res.drawable.ic_account_24),
                contentDescription = null,
                tint = iconTint,
            )

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = email ?: stringResource(resource = Res.string.log_in_negative_state),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun DrawerItem(
    iconRes: DrawableResource,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(30.dp),
            painter = painterResource(resource = iconRes),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
        )

        Spacer(modifier = Modifier.height(50.dp))
    }
}
