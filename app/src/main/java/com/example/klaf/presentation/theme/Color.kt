package com.example.klaf.presentation.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

private val DarkPrimaryColor = Color(0xFF5DA3AC)
private val DarkOnPrimaryColor = Color(0xFFE2E2E2)

data class MainColors(
    val materialColors: Colors,
    val statusBarBackground: Color,
    val lightDeckItemBackground: Color,
    val darkDeckItemBackground: Color,
    val evenDeckItemName: Color,
    val oddDeckItemName: Color,
    val scheduledDate: Color,
    val overdueScheduledDate: Color,
    val deckItemRepetitionQuantity: Color,
    val deckItemCardQuantity: Color,
    val deckItemPointer: Color,
    val positiveDialogButton: Color,
    val negativeDialogButton: Color,
    val neutralDialogButton: Color,
    val deckNavigationDialogSeparator: Color,
    val checkedLetterCell: Color,
    val uncheckedLetterCell: Color,
    val cardTextFieldBackground: Color,
    val cardNativeWord: Color,
    val cardForeignWord: Color,
    val cardIpa: Color,
    val deckRepetitionScreenColors: DeckRepetitionScreenColors,
    val viewingCardForeignWord: Color,
    val viewingCardIpa: Color,
    val viewingCardOrdinal: Color,
    val dataSynchronizationViewColors: DataSynchronizationViewColors,
    val deckRepetitionInfoScreenColors: DeckRepetitionInfoScreenColors,
    val cardTransferringScreenColors: CardTransferringScreenColors,
)

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
)

data class DataSynchronizationViewColors(
    val labelBackground: Color,
    val labelBackgroundSecond: Color,
    val progressIndicator: Color,
)

data class DeckRepetitionInfoScreenColors(
    val pointerBackground: Color,
    val itemDivider: Color,
    val successMark: Color,
    val failureMark: Color,
)

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
)

private val LightMaterialColors = lightColors(
    primary = Color(0xFFBEDB9C),
    onPrimary = Color(0xFF636363),
    onBackground = Color(0xFF474747)
)

private val LightDeckRepetitionScreenColors = DeckRepetitionScreenColors(
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

private val LightDataSynchronizationViewColors = DataSynchronizationViewColors(
    labelBackground = Color(0xFFFFFFFF),
    labelBackgroundSecond = Color(0xFFE2F7D7),
    progressIndicator = Color(0xFF92CFC3),
)

private val LightDeckRepetitionInfoScreenColors = DeckRepetitionInfoScreenColors(
    pointerBackground = Color(0x2F868686),
    itemDivider = Color(0xF1575757),
    successMark = Color(0x759BDA51),
    failureMark = Color(0x4DE98077),
)

val LightCadTransferringScreenColors = CardTransferringScreenColors(
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
    clickedMoreButton = LightMaterialColors.primary,
    unClickedMoreButton = Color(0xFFB8B8B8),
)

val LightMainPalettes = MainColors(
    materialColors = LightMaterialColors,
    statusBarBackground = Color(0xFF8AA768),
    lightDeckItemBackground = Color(0xFFFFFFFF),
    darkDeckItemBackground = Color(0xFFEBEBEB),
    evenDeckItemName = Color(0xFF7C995B),
    oddDeckItemName = Color(0xFF5C5C5C),
    scheduledDate = Color(0xFF3E92D5),
    overdueScheduledDate = Color(0xFFE66259),
    deckItemRepetitionQuantity = Color(0xFFFF9800),
    deckItemCardQuantity = Color(0xFFB27DBB),
    deckItemPointer = Color(0xFF7C7C7C),
    positiveDialogButton = Color(0xFFBFE295),
    negativeDialogButton = Color(0xFFEBAEB1),
    neutralDialogButton = Color(0xFFB9E5EB),
    deckNavigationDialogSeparator = Color(0xFF818181),
    checkedLetterCell = Color(0xFFB0D9DF),
    uncheckedLetterCell = Color(0xFFE9E9E9),
    cardTextFieldBackground = Color.Transparent,
    cardNativeWord = Color(0xFFC0914C),
    cardForeignWord = Color(0xFFAD7DB4),
    cardIpa = Color(0xFF6EA5AC),
    deckRepetitionScreenColors = LightDeckRepetitionScreenColors,
    viewingCardForeignWord = Color(0xFFA078AA),
    viewingCardIpa = Color(0xFF5E949C),
    viewingCardOrdinal = Color(0xFFBEBEBE),
    dataSynchronizationViewColors = LightDataSynchronizationViewColors,
    deckRepetitionInfoScreenColors = LightDeckRepetitionInfoScreenColors,
    cardTransferringScreenColors = LightCadTransferringScreenColors,
)

private val DarkMaterialColors = darkColors(
    primary = DarkPrimaryColor,
    onPrimary = DarkOnPrimaryColor,
)

private val DarkDeckRepetitionScreenColors = DeckRepetitionScreenColors(
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

private val DarkDataSynchronizationViewColors = DataSynchronizationViewColors(
    labelBackground = Color(0xFF1F1F1F),
    labelBackgroundSecond = Color(0xFF576F58),
    progressIndicator = Color(0xF0AC6761),
)

private val DarkDeckRepetitionInfoScreenColors = DeckRepetitionInfoScreenColors(
    pointerBackground = Color(0x2F868686),
    itemDivider = Color(0xF1575757),
    successMark = Color(0x3BAED382),
    failureMark = Color(0x4DE98077),
)

private val DarkCadTransferringScreenColors = CardTransferringScreenColors(
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
    clickedMoreButton = DarkMaterialColors.primary,
    unClickedMoreButton = Color(0xFF636363),
)

val DarkMainPalettes = MainColors(
    materialColors = DarkMaterialColors,
    statusBarBackground = Color(0xFF464646),
    lightDeckItemBackground = Color(0xFF464646),
    darkDeckItemBackground = Color(0xFF353535),
    evenDeckItemName = Color(0xFF76A243),
    oddDeckItemName = Color(0xFFB6B6B6),
    scheduledDate = Color(0xFFD69E4A),
    overdueScheduledDate = Color(0xFFDA8282),
    deckItemRepetitionQuantity = Color(0xFF56C2CF),
    deckItemCardQuantity = Color(0xFFD5C85B),
    deckItemPointer = Color(0xFF969696),
    positiveDialogButton = Color(0xFF96B671),
    negativeDialogButton = Color(0xFFD17670),
    neutralDialogButton = DarkPrimaryColor,
    deckNavigationDialogSeparator = Color(0xFF4D4D4D),
    checkedLetterCell = Color(0xFF94B172),
    uncheckedLetterCell = Color(0xFF63665F),
    cardTextFieldBackground = Color.Transparent,
    cardNativeWord = Color(0xFFA9CA84),
    cardForeignWord = Color(0xFFD3AA6E),
    cardIpa = Color(0xFFB8ABD1),
    deckRepetitionScreenColors = DarkDeckRepetitionScreenColors,
    viewingCardForeignWord = Color(0xFFA5CA79),
    viewingCardIpa = Color(0xFF86BBC9),
    viewingCardOrdinal = Color(0xFF6B6B6B),
    dataSynchronizationViewColors = DarkDataSynchronizationViewColors,
    deckRepetitionInfoScreenColors = DarkDeckRepetitionInfoScreenColors,
    cardTransferringScreenColors = DarkCadTransferringScreenColors,
)