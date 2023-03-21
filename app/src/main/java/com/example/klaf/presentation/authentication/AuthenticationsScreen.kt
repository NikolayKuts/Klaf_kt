package com.example.klaf.presentation.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R
import com.example.klaf.presentation.common.rememberAsMutableStateOf

@Composable
fun AuthenticationScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.2f)
    ) {
        SignInView()
    }
}

@Composable
private fun SignInView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(70.dp),
            painter = painterResource(id = R.drawable.color_10),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
//            modifier = Modifier.fillMaxWidth(),
            text = "Sign in",
            fontSize = 30.sp
        )
        Spacer(modifier = Modifier.height(30.dp))

        var content by rememberAsMutableStateOf(value = "survivenik@gmail.com")
        OutlinedTextField(
            value = content,
            onValueChange = { value -> content = value},
            shape = RoundedCornerShape(50.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        var passwordContent by rememberAsMutableStateOf(value = "survivenik@gmail.com")
        OutlinedTextField(
            value = "password",
            onValueChange = { value -> passwordContent = value }
        )
        Spacer(modifier = Modifier.height(30.dp))

        Button(onClick = { /*TODO*/ }) {
            Text(text = "Enter")
        }
    }
}