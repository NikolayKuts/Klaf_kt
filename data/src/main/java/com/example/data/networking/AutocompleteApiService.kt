package com.example.data.networking

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface AutocompleteApiService {

    @GET("{word}")
    suspend fun getAutoCompletion(@Path("word") word: String): ResponseBody
}