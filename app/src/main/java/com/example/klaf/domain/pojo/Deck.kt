package com.example.klaf.domain.pojo

data class Deck(
    private val id: Int,
    private val name: String,
    private val creationData: Long,
    private val repeatDay: Long,
    private val lastRepeatDate: Long,
    private val lastRepeatDuration: Long,
    private val repeatQuantity: Int,
    private val isLastRepetitionSucceeded: Boolean
) {
}