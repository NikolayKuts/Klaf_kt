package com.kuts.klaf.presentation.deckManagment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun DeckManagementScreen(
    viewModel: BaseDeckManagementViewModel
) {
    val deckManagementState by viewModel.deckState.collectAsState()

    with(deckManagementState) {
        Column {
            StateItem(pointer = "name", value = name)
            StateItem(pointer = "creationDate", value = creationDate)
//        StateItem(pointer = "repetitionIterationDates", value = repetitionIterationDates)
//        StateItem(pointer = "scheduledIterationDates", value = scheduledIterationDates)
            StateItem(pointer = "scheduledDateInterval", value = scheduledDateInterval.toString())
            StateItem(pointer = "repetitionQuantity", value = repetitionQuantity.toString())
            StateItem(pointer = "cardQuantity", value = cardQuantity.toString())
            StateItem(
                pointer = "lastFirstRepetitionDuration",
                value = lastFirstRepetitionDuration.toString()
            )
            StateItem(
                pointer = "lastSecondRepetitionDuration",
                value = lastSecondRepetitionDuration.toString()
            )
            StateItem(
                pointer = "lastRepetitionIterationDuration",
                value = lastRepetitionIterationDuration.toString()
            )
            StateItem(
                pointer = "isLastIterationSucceeded",
                value = isLastIterationSucceeded.toString()
            )
            StateItem(pointer = "id", value = id.toString())

        }
    }
}

@Composable
private fun StateItem(pointer: String, value: String) {
    Row {
        Text(text = pointer)
        Text(text = ": ")
        Text(text = value)
    }
}