package com.example.klaf.domain.entities

data class Deck(
    val name: String,
    val creationDate: Long,
    val id: Int = 0,
    val cardQuantity: Int = 0,
    val repeatDay: Int = 0,
    val scheduledDate: Long = 0,
    val lastRepeatDate: Long = 0,
    val repeatQuantity: Int = 0,
    val lastRepeatDuration: Long = 0,
    val isLastRepetitionSucceeded: Boolean = true,
)
