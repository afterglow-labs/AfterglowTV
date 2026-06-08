package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.ui.design.AppPalette
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreviewSwatchesTest {

    @Test
    fun `afterglow light previews expose actual accents instead of only pale surfaces`() {
        val lightPalettes = listOf(
            AppPalette.AfterglowLight1,
            AppPalette.AfterglowLight2,
            AppPalette.AfterglowLight3,
            AppPalette.AfterglowLight4,
            AppPalette.AfterglowLabs,
            AppPalette.RachelsSunrise,
        )

        lightPalettes.forEach { palette ->
            val swatches = themePreviewSwatches(palette)

            assertThat(swatches).contains(palette.accent)
            assertThat(swatches).contains(palette.accentLight)
            assertThat(swatches).contains(palette.nowLine)
            assertThat(swatches).contains(palette.info)
            assertThat(swatches.toSet().size).isEqualTo(swatches.size)
        }
    }

    @Test
    fun `afterglow dark previews expose actual accents instead of only matching dark surfaces`() {
        val darkPalettes = listOf(
            AppPalette.Afterglow1,
            AppPalette.AfterglowSunset,
            AppPalette.Afterglow4,
            AppPalette.AfterglowGray,
        )

        darkPalettes.forEach { palette ->
            val swatches = themePreviewSwatches(palette)

            assertThat(swatches).contains(palette.accent)
            assertThat(swatches).contains(palette.accentLight)
            assertThat(swatches).contains(palette.nowLine)
            assertThat(swatches).contains(palette.surfaceAccent)
            assertThat(swatches).contains(palette.info)
            assertThat(swatches.toSet().size).isEqualTo(swatches.size)
        }
    }

    @Test
    fun `afterglow dark one through four previews show distinct role colors`() {
        val previews = listOf(
            AppPalette.Afterglow1,
            AppPalette.AfterglowSunset,
            AppPalette.Afterglow3,
            AppPalette.Afterglow4,
        ).map(::themePreviewSwatches)

        assertThat(previews.toSet().size).isEqualTo(previews.size)
        previews.forEach { swatches ->
            assertThat(swatches).hasSize(6)
        }
    }

    @Test
    fun `preview swatches are ordered by usable UI roles`() {
        val palette = AppPalette.UltravioletSpectrum

        assertEquals(
            listOf(
                palette.surfaceDeep,
                palette.surfaceAccent,
                palette.accent,
                palette.accentLight,
                palette.nowLine,
                palette.live,
            ),
            themePreviewSwatches(palette),
        )
    }
}
