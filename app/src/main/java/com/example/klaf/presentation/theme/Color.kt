package com.example.klaf.presentation.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val LightMainPalettes = MainColors(
    material = MaterialColors.Theme.light,
    common = CommonColors.Theme.light,
    deckListScreen = DeckListScreenColors.Theme.light,
    cardManagementView = CardManagementViewColors.Theme.light,
    deckRepetitionScreen = DeckRepetitionScreenColors.Theme.light,
    dataSynchronizationView = DataSynchronizationViewColors.Theme.light,
    cardViewingScreen = CardViewingScreenColors.Theme.light,
    deckRepetitionInfoScreen = DeckRepetitionInfoScreenColors.Theme.light,
    cardTransferringScreen = CardTransferringScreenColors.Theme.light,
    authenticationScreen = AuthenticationScreenColors.Theme.light,
)

val DarkMainPalettes = MainColors(
    material = MaterialColors.Theme.dark,
    common = CommonColors.Theme.dark,
    deckListScreen = DeckListScreenColors.Theme.dark,
    cardManagementView = CardManagementViewColors.Theme.dark,
    deckRepetitionScreen = DeckRepetitionScreenColors.Theme.dark,
    dataSynchronizationView = DataSynchronizationViewColors.Theme.dark,
    cardViewingScreen = CardViewingScreenColors.Theme.dark,
    deckRepetitionInfoScreen = DeckRepetitionInfoScreenColors.Theme.dark,
    cardTransferringScreen = CardTransferringScreenColors.Theme.dark,
    authenticationScreen = AuthenticationScreenColors.Theme.dark,
)

data class MainColors(
    val material: Colors,
    val common: CommonColors,
    val deckListScreen: DeckListScreenColors,
    val cardManagementView: CardManagementViewColors,
    val deckRepetitionScreen: DeckRepetitionScreenColors,
    val dataSynchronizationView: DataSynchronizationViewColors,
    val cardViewingScreen: CardViewingScreenColors,
    val deckRepetitionInfoScreen: DeckRepetitionInfoScreenColors,
    val cardTransferringScreen: CardTransferringScreenColors,
    val authenticationScreen: AuthenticationScreenColors,
)

class MaterialColors {

    object Theme : Themable<Colors> {

        override val light: Colors = lightColors(
            primary = Color(0xFFBEDB9C),
            onPrimary = Color(0xFF636363),
            onBackground = Color(0xFF474747),
        )

        override val dark: Colors = darkColors(
            primary = Color(0xFF5DA3AC),
            onPrimary = Color(0xFFE2E2E2),
        )
    }
}

data class CommonColors(
    val statusBarBackground: Color,
    val focusedLabelColor: Color,
    val appLabelColorFilter: Color,
    val animationAppLabelColorFilter: Color,
    val positiveDialogButton: Color,
    val negativeDialogButton: Color,
    val neutralDialogButton: Color,
    val separator: Color,
) {

    object Theme : Themable<CommonColors> {

        override val light: CommonColors = CommonColors(
            statusBarBackground = Color(0xFF8AA768),
            focusedLabelColor = MaterialColors.Theme.light.onPrimary,
            appLabelColorFilter = Color(0xFF374D5E),
            animationAppLabelColorFilter = Color(0xFF4C5C3A),
            positiveDialogButton = Color(0xFFBFE295),
            negativeDialogButton = Color(0xFFEBAEB1),
            neutralDialogButton = Color(0xFFB9E5EB),
            separator = Color(0xFF818181),
        )

        override val dark: CommonColors = CommonColors(
            statusBarBackground = Color(0xFF464646),
            focusedLabelColor = MaterialColors.Theme.dark.onPrimary,
            appLabelColorFilter = Color(0xFF686868),
            animationAppLabelColorFilter = Color(0xFF4D6C85),
            positiveDialogButton = Color(0xFF809B62),
            negativeDialogButton = Color(0xFFB1645F),
            neutralDialogButton = Color(0xFF6F797C),
            separator = Color(0xFF4D4D4D),
        )
    }
}

data class DeckListScreenColors(
    val lightDeckItemBackground: Color,
    val darkDeckItemBackground: Color,
    val evenDeckItemName: Color,
    val oddDeckItemName: Color,
    val scheduledDate: Color,
    val overdueScheduledDate: Color,
    val deckItemRepetitionQuantity: Color,
    val deckItemCardQuantity: Color,
    val deckItemPointer: Color,
) {

    object Theme : Themable<DeckListScreenColors> {

        override val light = DeckListScreenColors(
            lightDeckItemBackground = Color(0xFFFFFFFF),
            darkDeckItemBackground = Color(0xFFEBEBEB),
            evenDeckItemName = Color(0xFF7C995B),
            oddDeckItemName = Color(0xFF5C5C5C),
            scheduledDate = Color(0xFF3E92D5),
            overdueScheduledDate = Color(0xFFE66259),
            deckItemRepetitionQuantity = Color(0xFFFF9800),
            deckItemCardQuantity = Color(0xFFB27DBB),
            deckItemPointer = Color(0xFF7C7C7C),
        )

        override val dark = DeckListScreenColors(
            lightDeckItemBackground = Color(0xFF464646),
            darkDeckItemBackground = Color(0xFF353535),
            evenDeckItemName = Color(0xFF76A243),
            oddDeckItemName = Color(0xFFB6B6B6),
            scheduledDate = Color(0xFFD69E4A),
            overdueScheduledDate = Color(0xFFDA8282),
            deckItemRepetitionQuantity = Color(0xFF56C2CF),
            deckItemCardQuantity = Color(0xFFD5C85B),
            deckItemPointer = Color(0xFF969696),
        )
    }
}

data class CardManagementViewColors(
    val checkedLetterCell: Color,
    val uncheckedLetterCell: Color,
    val textFieldBackground: Color,
    val nativeWord: Color,
    val foreignWord: Color,
    val ipa: Color,
    val ipaCellBackground: Color,
    val autocompleteMenuBackground: Color,
    val activePronunciationIcon: Color,
    val inactivePronunciationIcon: Color,
) {

    object Theme : Themable<CardManagementViewColors> {

        override val light = CardManagementViewColors(
            checkedLetterCell = Color(0xFFB0D9DF),
            uncheckedLetterCell = Color(0xFFE9E9E9),
            textFieldBackground = Color.Transparent,
            nativeWord = Color(0xFFC0914C),
            foreignWord = Color(0xFFAD7DB4),
            ipa = Color(0xFF6EA5AC),
            ipaCellBackground = Color(0xFFFAE1CB),
            autocompleteMenuBackground = Color(0xFFFFFFFF),
            activePronunciationIcon = Color(0xFF8AAF60),
            inactivePronunciationIcon = Color(0xF1A7A7A7),
        )

        override val dark = CardManagementViewColors(
            checkedLetterCell = Color(0xFF7F9961),
            uncheckedLetterCell = Color(0xFF63665F),
            textFieldBackground = Color.Transparent,
            nativeWord = Color(0xFFA9CA84),
            foreignWord = Color(0xFFD3AA6E),
            ipa = Color(0xFFB8ABD1),
            ipaCellBackground = Color(0xFF2B3A46),
            autocompleteMenuBackground = Color(0xFF222222),
            activePronunciationIcon = Color(0xFF809B62),
            inactivePronunciationIcon = Color(0xF1474747),
        )
    }
}

data class DeckRepetitionScreenColors(
    val mainButtonPressed: Color,
    val mainButtonUnpressed: Color,
    val deleteButton: Color,
    val editButton: Color,
    val addButton: Color,
    val frontSideCardButton: Color,
    val backSideCardButton: Color,
    val frontSideOrderPointer: Color,
    val backSideOrderPointer: Color,
    val timerActive: Color,
    val timerInactive: Color,
    val frontSideCardWord: Color,
    val backSideCardWord: Color,
    val ipaPromptChecked: Color,
    val ipaPromptUnchecked: Color,
) {

    object Theme : Themable<DeckRepetitionScreenColors> {

        override val light = DeckRepetitionScreenColors(
            frontSideOrderPointer = Color(0xFF6EA0A7),
            backSideOrderPointer = Color(0xFF639766),
            timerActive = Color(0xFF5E5D5D),
            timerInactive = Color(0xFFAFAFAF),
            frontSideCardWord = Color(0xFF66969C),
            backSideCardWord = Color(0xFF60A064),
            ipaPromptChecked = Color(0xFFD35147),
            ipaPromptUnchecked = Color(0xFF818181),
            mainButtonPressed = Color(0xFFB4BBAD),
            mainButtonUnpressed = Color(0xFFBEDB9C),
            deleteButton = Color(0xFFEECBCB),
            editButton = Color(0xFFEEE7AA),
            addButton = Color(0xFFCCEDF1),
            frontSideCardButton = Color(0xFF8ECDD6),
            backSideCardButton = Color(0xFF96C799),
        )

        override val dark = DeckRepetitionScreenColors(
            frontSideOrderPointer = Color(0xFF8CA86B),
            backSideOrderPointer = Color(0xFF81B7BD),
            timerActive = Color(0xFFBDBDBD),
            timerInactive = Color(0xFF7A7A7A),
            frontSideCardWord = Color(0xFF8CA86B),
            backSideCardWord = Color(0xFF81B7BD),
            ipaPromptChecked = Color(0xFFCF726B),
            ipaPromptUnchecked = Color(0xFF585857),
            mainButtonPressed = Color(0xFF888888),
            mainButtonUnpressed = Color(0xFF92AC75),
            deleteButton = Color(0xFFC97474),
            editButton = Color(0xFFC29F63),
            addButton = Color(0xFF88A568),
            frontSideCardButton = Color(0xFF8CA86B),
            backSideCardButton = Color(0xFF81B7BD),
        )
    }
}

data class DataSynchronizationViewColors(
    val initialLabelBackground: Color,
    val targetLabelBackground: Color,
    val progressIndicator: Color,
    val initialWarning: Color,
    val targetWarning: Color,
) {

    object Theme : Themable<DataSynchronizationViewColors> {

        override val light = DataSynchronizationViewColors(
            initialLabelBackground = Color(0xFFFFFFFF),
            targetLabelBackground = Color(0xFFC4ECB0),
            progressIndicator = Color(0xFF92CFC3),
            initialWarning = Color(0x4FD67A7A),
            targetWarning = Color(0x19D57A7A),
        )

        override val dark = DataSynchronizationViewColors(
            initialLabelBackground = Color(0xFF1F1F1F),
            targetLabelBackground = Color(0xFF576F58),
            progressIndicator = Color(0xF0AC6761),
            initialWarning = Color(0x4FD67A7A),
            targetWarning = Color(0x19D57A7A),
        )
    }
}

data class CardViewingScreenColors(
    val foreignWord: Color,
    val ipa: Color,
    val ordinal: Color,
) {

    object Theme : Themable<CardViewingScreenColors> {

        override val light = CardViewingScreenColors(
            foreignWord = Color(0xFFA078AA),
            ipa = Color(0xFF5E949C),
            ordinal = Color(0xFFBEBEBE),
        )

        override val dark = CardViewingScreenColors(
            foreignWord = Color(0xFFA5CA79),
            ipa = Color(0xFF86BBC9),
            ordinal = Color(0xFF6B6B6B),
        )
    }
}

data class DeckRepetitionInfoScreenColors(
    val pointerBackground: Color,
    val itemDivider: Color,
    val successMark: Color,
    val failureMark: Color,
) {

    object Theme : Themable<DeckRepetitionInfoScreenColors> {

        override val light = DeckRepetitionInfoScreenColors(
            pointerBackground = Color(0x2F868686),
            itemDivider = Color(0xF1575757),
            successMark = Color(0x759BDA51),
            failureMark = Color(0x4DE98077),
        )

        override val dark = DeckRepetitionInfoScreenColors(
            pointerBackground = Color(0x2F868686),
            itemDivider = Color(0xF1575757),
            successMark = Color(0x3BAED382),
            failureMark = Color(0x4DE98077),
        )
    }
}

data class CardTransferringScreenColors(
    val quantityPointerBackground: Color,
    val cardOrdinal: Color,
    val foreignWord: Color,
    val selectedCheckBox: Color,
    val unCheckedBorder: Color,
    val itemDivider: Color,
    val transferringButton: Color,
    val cardAddingButton: Color,
    val deletingButton: Color,
    val chosenDeckBoxBorder: Color,
    val clickedMoreButton: Color,
    val unClickedMoreButton: Color,
) {

    object Theme : Themable<CardTransferringScreenColors> {

        override val light = CardTransferringScreenColors(
            quantityPointerBackground = Color(0x4B707070),
            cardOrdinal = Color(0xFFC4C4C4),
            foreignWord = Color(0xFF8F3CA3),
            selectedCheckBox = Color(0xFFA9D378),
            unCheckedBorder = Color(0xFF7E7E7E),
            itemDivider = Color(0xF17E7E7E),
            transferringButton = Color(0xFF87BAE2),
            cardAddingButton = Color(0xFFB3CC96),
            deletingButton = Color(0xFFDA9B96),
            chosenDeckBoxBorder = Color(0xFFB3CC96),
            clickedMoreButton = MaterialColors.Theme.light.primary,
            unClickedMoreButton = Color(0xFFB8B8B8),
        )

        override val dark = CardTransferringScreenColors(
            quantityPointerBackground = Color(0x4B707070),
            cardOrdinal = Color(0xFF525252),
            foreignWord = Color(0xFF93B46A),
            selectedCheckBox = Color(0xFF85A560),
            unCheckedBorder = Color(0xFF646464),
            itemDivider = Color(0xF1636262),
            transferringButton = Color(0xFF4E7383),
            cardAddingButton = Color(0xFF809C5F),
            deletingButton = Color(0xFFC4716A),
            chosenDeckBoxBorder = Color(0xFF8CA76D),
            clickedMoreButton = MaterialColors.Theme.dark.primary,
            unClickedMoreButton = Color(0xFF636363),
        )
    }
}

data class AuthenticationScreenColors(
    val textFieldBackground: Color,
) {

    object Theme : Themable<AuthenticationScreenColors> {

        override val light = AuthenticationScreenColors(
            textFieldBackground = Color(0xFFF7F6F6),
        )

        override val dark = AuthenticationScreenColors(
            textFieldBackground = Color(0xFF1A1A1A),
        )
    }
}