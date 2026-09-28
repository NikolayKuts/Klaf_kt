package com.kuts.klaf.mnemonic

import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.MnemonicGenerationHandle
import com.kuts.domain.managers.MnemonicGenerationOutcome
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.managers.MnemonicGenerationType

class NoOpMnemonicGenerationBackgroundManager : IMnemonicGenerationBackgroundManager {

    override fun startGeneration(
        type: MnemonicGenerationType,
        source: MnemonicGenerationSource,
    ): MnemonicGenerationHandle =
        MnemonicGenerationHandle { _: MnemonicGenerationOutcome, _: Boolean -> Unit }
}
