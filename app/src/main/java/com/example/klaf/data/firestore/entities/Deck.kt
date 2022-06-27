package com.example.klaf.data.firestore.entities

data class FirestoreDeck(
    val name: String = DEFAULT_STRING_VALUE,
    val creationDate: Long = DEFAULT_LONG_VALUE,
    val id: Int = DEFAULT_INT_VALUE,
    val cardQuantity: Int = DEFAULT_INT_VALUE,
    val repeatDay: Int = DEFAULT_INT_VALUE,
    val scheduledDate: Long = DEFAULT_LONG_VALUE,
    val lastRepeatDate: Long = DEFAULT_LONG_VALUE,
    val repeatQuantity: Int = DEFAULT_INT_VALUE,
    val lastRepeatDuration: Long = DEFAULT_LONG_VALUE,
    @field: JvmField
    val isLastRepetitionSucceeded: Boolean = DEFAULT_BOOLEAN_VALUE,
)
