package com.kuts.domain.useCases

import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransferDataOfOldAppKlafUseCase @Inject constructor(
    private val oldAppKlafDataTransferRepository: IOldAppKlafDataTransferRepository,
) {

    suspend operator fun invoke() {
        withContext(Dispatchers.IO) { oldAppKlafDataTransferRepository.transferOldData() }
    }
}