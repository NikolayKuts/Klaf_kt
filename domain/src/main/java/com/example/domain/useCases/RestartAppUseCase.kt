package com.example.domain.useCases

import com.example.domain.repositories.PerformanceRepository

class RestartAppUseCase(
    private val appRestartWorkPerformer: PerformanceRepository
) {

    operator fun invoke() {
        appRestartWorkPerformer.perform()
    }
}