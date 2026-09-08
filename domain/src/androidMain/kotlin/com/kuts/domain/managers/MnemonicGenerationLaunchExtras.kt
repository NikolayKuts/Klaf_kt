package com.kuts.domain.managers

object MnemonicGenerationLaunchExtras {

    // Preserve the old values so notifications created by the image-only implementation still work.
    const val DESTINATION_KEY = "mnemonic_image_launch_destination"
    const val DECK_ID_KEY = "mnemonic_image_launch_deck_id"
    const val CARD_ID_KEY = "mnemonic_image_launch_card_id"

    const val DESTINATION_CARD_ADDITION = "card_addition_mnemonic_management"
    const val DESTINATION_CARD_EDITING = "card_editing_mnemonic_management"
}
