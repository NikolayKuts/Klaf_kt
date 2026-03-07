package com.kuts.klaf.cardManagement.common

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.klaf.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.common.IEventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardManagementViewModel(
    val audioPlayer: IAudioPlayerManager,
    protected val cambridgeWordDataProvider: ICambridgeWordDataProvider,
) : ViewModel(), IEventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit, Unit>>
    abstract val nativeWordSuggestionsState: StateFlow<NativeWordSuggestionsState>
    abstract val transcriptionState: StateFlow<String>
    abstract val cardManagementState: StateFlow<CardManagementState>
    abstract val cambridgeDataState: StateFlow<ICambridgeDataState>
    abstract val ipaKeyboardState: StateFlow<IpaKeyboardState>

    abstract fun sendAction(action: ICardManagementAction)
}
