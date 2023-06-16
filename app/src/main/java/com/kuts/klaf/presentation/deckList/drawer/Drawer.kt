package com.kuts.klaf.presentation.deckList.drawer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kuts.klaf.R

@Composable
fun Drawer(
    state: DrawerViewState,
    onLogInClick: () -> Unit,
    onLogOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Header(
            signedIn = state.signedIn,
            email = state.userEmail,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.signedIn) {
                DrawerItem(
                    iconId = R.drawable.ic_logout_24,
                    text = stringResource(R.string.log_out_action),
                    onClick = onLogOutClick
                )
                DrawerItem(
                    iconId = R.drawable.ic_delete_account_24,
                    text = stringResource(R.string.account_deleting_action),
                    onClick = onDeleteAccountClick
                )
            } else {
                DrawerItem(
                    iconId = R.drawable.ic_login_24,
                    text = stringResource(R.string.log_in_action),
                    onClick = onLogInClick
                )
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
            .background(Color(0xF04D4D4D))
            .padding(16.dp)
            .heightIn(min = 100.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        val iconTint = if (signedIn) Color(0xFFC3E799) else Color(0xF1FFB0B0)

        Column {
            Icon(
                modifier = Modifier
                    .size(70.dp)
                    .padding(bottom = 8.dp),
                painter = painterResource(id = R.drawable.ic_account_24),
                contentDescription = null,
                tint = iconTint
            )

            Text(
                modifier = Modifier
                    .padding(start = 8.dp),
                text = email ?: stringResource(R.string.log_in_negative_state),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun DrawerItem(
    @DrawableRes iconId: Int,
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
            painter = painterResource(id = iconId),
            contentDescription = null,
        )

        Text(
            modifier = Modifier
                .weight(weight = 1F),
            text = text,
        )

        Spacer(modifier = Modifier.height(50.dp))
    }
}