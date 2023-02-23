package com.example.data.networking

import retrofit2.Retrofit
import retrofit2.create

object AutocompleteApiRetrofit {

    private const val BASE_URL = "https://wooordhunt.ru/word/"

    fun getInstance(): AutocompleteApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .build()
        .create(AutocompleteApiService::class.java)
}