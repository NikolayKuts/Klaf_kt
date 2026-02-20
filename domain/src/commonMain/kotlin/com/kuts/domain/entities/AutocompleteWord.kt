package com.kuts.domain.entities

import com.kuts.domain.common.IWordable

data class AutocompleteWord(private val value: String) : IWordable {

    override fun word(): String = value
}