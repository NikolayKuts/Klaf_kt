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
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.isNotNull
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
                    text = "Log out",
                    onClick = onLogOutClick
                )
                DrawerItem(
                    iconId = R.drawable.ic_logout_24,
                    text = "Delete account",
                    onClick = onDeleteAccountClick
                )
            } else {
                DrawerItem(
                    iconId = R.drawable.ic_login_24,
                    text = "Log in",
                    onClick = onLogInClick
                )
            }
            
            listOf(
                R.drawable.ic_close_24,
                R.drawable.ic_deck_popup_menu_24,
                R.drawable.ic_arrow_drop_down_24,
                R.drawable.ic_sync_24,
            ).forEach {
                DrawerItem(iconId = it, text = state.userEmail ?: "null", onClick = {})
                Spacer(modifier = Modifier.height(16.dp))
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

        Column() {
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
                    .padding(start = 8.dp)
                    .background(Color(0x8BC34A)),
                text = email ?: "not signed up",
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
        verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(30.dp),
            painter = painterResource(id = iconId),
            contentDescription = null,
        )

        Text(
            modifier = Modifier
                .weight(weight = 1F)
                .background(Color(0xFF5722)),
            text = text,
        )
    }
}