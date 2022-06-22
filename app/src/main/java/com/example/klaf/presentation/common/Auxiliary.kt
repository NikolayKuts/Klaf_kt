package com.example.klaf.presentation.common

import android.util.Log

fun log(message: String, tag: String = "app_log", pointer: String = "************") {
    Log.i(tag, "$pointer $message")
}