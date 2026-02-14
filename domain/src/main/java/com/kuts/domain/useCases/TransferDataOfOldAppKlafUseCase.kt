package com.kuts.domain.useCases

import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransferDataOfOldAppKlafUseCase(
    private val oldAppKlafDataTransferRepository: IOldAppKlafDataTransferRepository,
) {

    suspend operator fun invoke() {
        withContext(Dispatchers.IO) { oldAppKlafDataTransferRepository.transferOldData() }
    }
}