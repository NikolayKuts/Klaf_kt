package com.kuts.domain.managers

import com.kuts.domain.entities.KlafServerConnectionState
import kotlinx.coroutines.flow.StateFlow

interface IKlafServerConnectionManager {

    val state: StateFlow<KlafServerConnectionState>

    suspend fun retry()
}
