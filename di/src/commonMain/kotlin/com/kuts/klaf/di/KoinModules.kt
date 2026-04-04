package com.kuts.klaf.di

import org.koin.core.module.Module

private val commonAppModules = listOf(
    commonDataModule,
    commonPresentationModule,
)

internal fun buildAppModules(vararg platformModules: Module): List<Module> {
    return commonAppModules + platformModules
}
