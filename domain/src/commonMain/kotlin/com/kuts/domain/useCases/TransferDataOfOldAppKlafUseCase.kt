package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import kotlinx.coroutines.withContext

class TransferDataOfOldAppKlafUseCase(
    private val oldAppKlafDataTransferRepository: IOldAppKlafDataTransferRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke() {
        withContext(context = coroutineContextProvider.io) {
            oldAppKlafDataTransferRepository.transferOldData()
        }
    }
}
