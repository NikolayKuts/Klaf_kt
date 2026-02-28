package com.kuts.domain.repositories

interface ICrashlyticsRepository {

    fun report(exception: Throwable)
}