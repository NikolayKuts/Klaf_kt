package com.kuts.klaf.deckList.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kuts.klaf.presentation.resources.*
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
                if (state.signedIn) {
                    DrawerItem(
                        iconRes = Res.drawable.ic_logout_24,
                        text = stringResource(resource = Res.string.log_out_action),
                        onClick = onLogOutClick
                    )
                    DrawerItem(
                        iconRes = Res.drawable.ic_delete_account_24,
                        text = stringResource(resource = Res.string.account_deleting_action),
                        onClick = onDeleteAccountClick
                    )
                } else {
                    DrawerItem(
                        iconRes = Res.drawable.ic_login_24,
                        text = stringResource(resource = Res.string.log_in_action),
                        onClick = onLogInClick
                    )
                }
            }
        }
    }
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
        contentAlignment = Alignment.BottomStart
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
                tint = iconTint
            )

            Text(
                modifier = Modifier
                    .padding(start = 8.dp),
                text = email ?: stringResource(resource = Res.string.log_in_negative_state),
                fontStyle = FontStyle.Italic
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
