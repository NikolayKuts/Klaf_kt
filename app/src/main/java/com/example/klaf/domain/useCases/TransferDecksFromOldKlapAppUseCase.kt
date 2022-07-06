package com.example.klaf.domain.useCases

import com.example.klaf.domain.repositories.OldAppKlafDataTransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransferDecksFromOldKlapAppUseCase @Inject constructor(
    private val oldAppKlafDataTransferRepository: OldAppKlafDataTransferRepository,
) {

    suspend operator fun invoke() {
        withContext(Dispatchers.IO) { oldAppKlafDataTransferRepository.transferOldData() }
    }
}