package com.example.klaf.domain

import android.content.Context
import android.widget.Toast
import com.example.klaf.R

fun<T> MutableList<T>.update(newData: List<T>) {
    this.clear()
    this.addAll(newData)
}

fun String.showToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}