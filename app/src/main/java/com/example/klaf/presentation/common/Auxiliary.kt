package com.example.klaf.presentation.common

import android.util.Log

fun log(
    message: String,
    tag: String = "app_log",
    pointerMessage: String = "",
    pointer: String =
        if (pointerMessage.isEmpty()) "***********" else  "****** $pointerMessage ******",
) {
    Log.i(tag, "$pointer $message")
}