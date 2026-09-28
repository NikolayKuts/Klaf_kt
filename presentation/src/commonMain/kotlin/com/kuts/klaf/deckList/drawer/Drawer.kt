package com.kuts.klaf.deckList.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.account_deleting_action
import com.kuts.klaf.presentation.resources.drawer_klaf_server_label
import com.kuts.klaf.presentation.resources.drawer_klaf_server_retry
import com.kuts.klaf.presentation.resources.drawer_klaf_server_status_connecting
import com.kuts.klaf.presentation.resources.drawer_klaf_server_status_not_ready
import com.kuts.klaf.presentation.resources.drawer_klaf_server_status_ready
import com.kuts.klaf.presentation.resources.ic_account_24
import com.kuts.klaf.presentation.resources.ic_delete_account_24
import com.kuts.klaf.presentation.resources.ic_list_clear
import com.kuts.klaf.presentation.resources.ic_login_24
import com.kuts.klaf.presentation.resources.ic_logout_24
import com.kuts.klaf.presentation.resources.log_in_action
import com.kuts.klaf.presentation.resources.log_in_negative_state
import com.kuts.klaf.presentation.resources.log_out_action
import com.kuts.klaf.presentation.resources.vocabulary_sources_title
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
    onKlafServerRetry: () -> Unit,
    onVocabularySourcesClick: () -> Unit,
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
                KlafServerSection(
                    state = state,
                    onRetry = onKlafServerRetry,
                )

                Spacer(modifier = Modifier.height(24.dp))

                DrawerItem(
                    iconRes = Res.drawable.ic_list_clear,
                    text = stringResource(resource = Res.string.vocabulary_sources_title),
                    onClick = onVocabularySourcesClick,
                )

                if (state.signedIn) {
                    DrawerItem(
                        iconRes = Res.drawable.ic_logout_24,
                        text = stringResource(resource = Res.string.log_out_action),
                        onClick = onLogOutClick,
                        enabled = state.canSignOut,
                    )
                    if (state.canDeleteAccount) {
                        DrawerItem(
                            iconRes = Res.drawable.ic_delete_account_24,
                            text = stringResource(resource = Res.string.account_deleting_action),
                            onClick = onDeleteAccountClick,
                        )
                    }
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
private fun KlafServerSection(
    state: DrawerViewState,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(resource = Res.string.drawer_klaf_server_label),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(size = 12.dp))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = klafServerStatusText(state = state),
                    color = klafServerStatusColor(state = state),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.klafServerConnectionState.canRetry()) {
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(resource = Res.string.drawer_klaf_server_retry))
                }
            }
        }
    }
}

@Composable
private fun klafServerStatusText(state: DrawerViewState): String {
    return when (val connectionState = state.klafServerConnectionState) {
        KlafServerConnectionState.Disconnected -> {
            stringResource(resource = Res.string.drawer_klaf_server_status_not_ready)
        }

        is KlafServerConnectionState.Reconnecting -> {
            stringResource(resource = Res.string.drawer_klaf_server_status_connecting)
        }

        KlafServerConnectionState.Ready -> {
            stringResource(resource = Res.string.drawer_klaf_server_status_ready)
        }

        is KlafServerConnectionState.Error -> connectionState.message
    }
}

@Composable
private fun klafServerStatusColor(state: DrawerViewState) = when (state.klafServerConnectionState) {
    KlafServerConnectionState.Ready -> MainTheme.colors.deckListScreen.drawerColors.profileIconPositiveTint
    is KlafServerConnectionState.Error -> MainTheme.colors.deckListScreen.drawerColors.profileIconNegativeTint
    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
}

private fun KlafServerConnectionState.canRetry(): Boolean {
    return this is KlafServerConnectionState.Error || this == KlafServerConnectionState.Disconnected
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
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
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
