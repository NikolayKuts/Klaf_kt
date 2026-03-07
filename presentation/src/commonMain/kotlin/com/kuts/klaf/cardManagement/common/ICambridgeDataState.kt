package com.kuts.klaf.cardManagement.common

sealed interface ICambridgeDataState {
    data class Fetched(val word: CambridgeWordData) : ICambridgeDataState
    data object Empty : ICambridgeDataState
}
