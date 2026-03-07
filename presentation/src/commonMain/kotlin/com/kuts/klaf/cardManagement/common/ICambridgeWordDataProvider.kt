package com.kuts.klaf.cardManagement.common

interface ICambridgeWordDataProvider {
    suspend fun fetchWordData(word: String): CambridgeWordData?
}
