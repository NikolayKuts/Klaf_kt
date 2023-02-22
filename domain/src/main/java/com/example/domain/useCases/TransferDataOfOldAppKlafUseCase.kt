package com.example.domain.useCases

import com.example.domain.repositories.OldAppKlafDataTransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransferDataOfOldAppKlafUseCase @Inject constructor(
    private val oldAppKlafDataTransferRepository: OldAppKlafDataTransferRepository,
) {

    suspend operator fun invoke() {
        withContext(Dispatchers.IO) { oldAppKlafDataTransferRepository.transferOldData() }
    }
}