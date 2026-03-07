package com.kuts.klaf.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlin.reflect.KType

internal inline fun <reified T : Any> NavGraphBuilder.buildComposable(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    noinline content: @Composable (route: T) -> Unit,
) {
    composable<T>(typeMap = typeMap) { backStackEntry ->
        content(backStackEntry.toRoute<T>())
    }
}

internal inline fun <reified T : Any> NavGraphBuilder.buildComposable(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    noinline content: @Composable (entry: NavBackStackEntry, route: T) -> Unit,
) {
    composable<T>(typeMap = typeMap) { backStackEntry ->
        content(backStackEntry, backStackEntry.toRoute<T>())
    }
}

internal inline fun <reified T : Any> NavGraphBuilder.buildComposableWithEntry(
    noinline content: @Composable (entry: NavBackStackEntry) -> Unit,
) {
    composable<T> { backStackEntry ->
        content(backStackEntry)
    }
}

internal inline fun <reified T : Any> NavGraphBuilder.buildDialog(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    noinline content: @Composable (route: T) -> Unit,
) {
    dialog<T>(typeMap = typeMap) { backStackEntry ->
        content(backStackEntry.toRoute<T>())
    }
}

internal inline fun <reified T : Any> NavGraphBuilder.buildDialog(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    noinline content: @Composable (entry: NavBackStackEntry, route: T) -> Unit,
) {
    dialog<T>(typeMap = typeMap) { backStackEntry ->
        content(backStackEntry, backStackEntry.toRoute<T>())
    }
}

internal inline fun <reified T : Any> NavGraphBuilder.buildDialogWithEntry(
    noinline content: @Composable (entry: NavBackStackEntry) -> Unit,
) {
    dialog<T> { backStackEntry ->
        content(backStackEntry)
    }
}
