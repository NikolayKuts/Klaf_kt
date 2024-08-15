package com.kuts.domain.entities

import com.kuts.domain.common.Wordable

data class AutocompleteWord(private val value: String) : Wordable {

    override fun word(): String = value
}