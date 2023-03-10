package com.example.klaf.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun MainTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkMainPalettes else LightMainPalettes
    val shapes = Shapes
    val typographies = if (darkTheme) DarkMainTypographies else LightMainTypographies
    val dimensions = CommonDimension

    CompositionLocalProvider(
        LocalMainColors provides colors,
        LocalCustomTypographies provides typographies,
        LocalCustomShapes provides shapes,
        LocaleCustomDimensions provides dimensions
    ) {
        MaterialTheme(
            colors = colors.materialColors,
            typography = typographies.materialTypographies,
            shapes = shapes,
            content = content
        )
    }
}

interface Themable<T> {

    val light: T
    val dark: T
}

object MainTheme {

    val colors: MainColors
        @Composable
        get() = LocalMainColors.current

    val typographies: MainTopographies
        @Composable
        get() = LocalCustomTypographies.current

    val shapes: Shapes
        @Composable
        get() = LocalCustomShapes.current

    val dimensions: MainDimensions
        @Composable
        get() = LocaleCustomDimensions.current
}

val LocalMainColors = staticCompositionLocalOf<MainColors> {
    error("No colors provided")
}

private val LocalCustomShapes = staticCompositionLocalOf<Shapes> {
    error("No shapes provided")
}

val LocalCustomTypographies = staticCompositionLocalOf<MainTopographies> {
    error("No typographies provided")
}

val LocaleCustomDimensions = staticCompositionLocalOf<MainDimensions> {
    error("No typographies provided")
}