package com.example.klaf.di

import com.example.klaf.presentation.deckList.DeckListFragment
import dagger.Component

@Component
interface Component {

    fun inject(fragment: DeckListFragment)
}