package com.kuts.klaf.cardManagement.common

/** The comment fields of the mnemonic management screen that accept voice dictation. */
enum class MnemonicCommentField {
    ASSOCIATION,
    IMAGE,
}

data class MnemonicSpeechInputUiState(
    val isAvailable: Boolean = false,
    val activeField: MnemonicCommentField? = null,
    val phase: Phase = Phase.IDLE,
) {

    enum class Phase {
        IDLE,
        STARTING,
        LISTENING,
        PROCESSING,
    }

    /** Only one dictation session runs at a time, so this disables every microphone button. */
    val isBusy: Boolean
        get() = phase != Phase.IDLE

    fun isBusyFor(field: MnemonicCommentField): Boolean = isBusy && activeField == field
}

/** What went wrong during dictation, in terms the presentation layer can turn into a message. */
enum class MnemonicSpeechInputFailure {
    UNSUPPORTED,
    PERMISSION_DENIED,
    PERMISSION_DENIED_ALWAYS,
    NO_SPEECH_DETECTED,
    NETWORK,
    BUSY,
    UNKNOWN,
}

data class MnemonicRecognizedText(
    val field: MnemonicCommentField,
    val text: String,
)
