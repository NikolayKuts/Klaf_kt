package com.kuts.domain.repositories

interface CrashlyticsRepository {

    fun report(exception: Throwable)
}