package com.example.domain.repositories

interface CrashlyticsRepository {

    fun report(exception: Throwable)
}