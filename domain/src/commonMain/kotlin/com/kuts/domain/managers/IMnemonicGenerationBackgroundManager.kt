package com.kuts.domain.managers

enum class MnemonicGenerationType {
    Text,
    Image,
}

enum class MnemonicGenerationOutcome {
    Succeeded,
    Failed,
    Cancelled,
}

sealed interface MnemonicGenerationSource {

    data class CardCreation(val deckId: Int) : MnemonicGenerationSource

    data class CardEditing(
        val deckId: Int,
        val cardId: Int,
    ) : MnemonicGenerationSource
}

fun interface MnemonicGenerationHandle {
    fun finish(outcome: MnemonicGenerationOutcome)
}

/** Platform hook that keeps generation visible to the OS and reports its terminal outcome. */
interface IMnemonicGenerationBackgroundManager {

    fun startGeneration(
        type: MnemonicGenerationType,
        source: MnemonicGenerationSource,
    ): MnemonicGenerationHandle
}
