package com.kuts.klaf.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.kuts.domain.entities.CefrLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun contrast(foreground: Color, background: Color): Float {
    val first = foreground.compositeOver(background).luminance()
    val second = background.luminance()
    return (maxOf(first, second) + 0.05F) / (minOf(first, second) + 0.05F)
}

class VocabularySourceThemeTest {
    @Test
    fun sourcePalettesAreWiredToBothApplicationThemes() {
        assertSame(VocabularySourceScreenColors.Theme.light, LightMainPalettes.vocabularySourceScreen)
        assertSame(VocabularySourceScreenColors.Theme.dark, DarkMainPalettes.vocabularySourceScreen)
        assertNotEquals(
            LightMainPalettes.vocabularySourceScreen.disabledTextModeBackground,
            DarkMainPalettes.vocabularySourceScreen.disabledTextModeBackground,
        )
        assertNotEquals(
            LightMainPalettes.vocabularySourceScreen.savedButtonContainer,
            DarkMainPalettes.vocabularySourceScreen.savedButtonContainer,
        )
        assertNotEquals(
            LightMainPalettes.vocabularySourceScreen.occurrenceHighlight,
            DarkMainPalettes.vocabularySourceScreen.occurrenceHighlight,
        )
    }

    @Test
    fun categoryAndCefrLabelsAreReadableInBothThemes() {
        for (theme in listOf(LightMainPalettes, DarkMainPalettes)) {
            val colors = theme.vocabularySourceScreen
            val itemBackground = colors.itemContainer.compositeOver(theme.material.background)
            for (badge in listOf(colors.newBadge, colors.newMeaningBadge, colors.ignoredNewMeaningBadge)) {
                val background = badge.container.compositeOver(itemBackground)
                assertTrue(contrast(badge.content, background) >= 4.5F, "Category label contrast")
            }
            assertEquals(CefrLevel.entries.size, colors.cefrBadgeContainers.size)
            for (background in colors.cefrBadgeContainers) {
                assertTrue(contrast(colors.cefrBadgeContent, background) >= 4.5F, "CEFR label contrast")
            }
        }
    }

    @Test
    fun disabledButtonsAndTransparentSurfacesUseCentralTokens() {
        for (theme in listOf(LightMainPalettes, DarkMainPalettes)) {
            val colors = theme.vocabularySourceScreen
            assertEquals(theme.material.onSurface.copy(alpha = 0.12F), colors.disabledButtonContainer)
            assertEquals(theme.material.onSurface.copy(alpha = 0.38F), colors.disabledButtonContent)
            assertEquals(0F, theme.common.transparent.alpha)
            assertTrue(colors.disabledIcon.alpha < colors.icon.alpha)
        }
    }
}
