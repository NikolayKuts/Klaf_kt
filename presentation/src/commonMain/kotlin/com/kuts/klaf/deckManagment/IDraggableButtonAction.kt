package com.kuts.klaf.deckManagment

sealed interface IDraggableButtonAction {
    data object Increase : IDraggableButtonAction
    data object Decrease : IDraggableButtonAction
    data object Reset : IDraggableButtonAction
}
