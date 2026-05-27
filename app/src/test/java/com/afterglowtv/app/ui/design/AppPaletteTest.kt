package com.afterglowtv.app.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppPaletteTest {

    @Test
    fun `afterglow light palettes avoid whiteout brightness`() {
        val lightPalettes = listOf(
            AppPalette.AfterglowLight1,
            AppPalette.AfterglowLight2,
            AppPalette.AfterglowLight3,
            AppPalette.AfterglowLight4,
            AppPalette.AfterglowGrayLight,
            AppPalette.RachelsSunsetLight
        )

        lightPalettes.forEach { palette ->
            assertThat(palette.surfaceDeep.luminance()).isLessThan(0.82f)
            assertThat(contrastRatio(palette.textPrimary, palette.surfaceCool)).isAtLeast(4.5f)
        }
    }

    @Test
    fun `afterglow light and dark themes have distinct text families`() {
        val darkPalettes = listOf(
            AppPalette.Afterglow1,
            AppPalette.AfterglowSunset,
            AppPalette.Afterglow3,
            AppPalette.Afterglow4,
            AppPalette.AfterglowGray,
        )
        val lightPalettes = listOf(
            AppPalette.AfterglowLight1,
            AppPalette.AfterglowLight2,
            AppPalette.AfterglowLight3,
            AppPalette.AfterglowLight4,
            AppPalette.AfterglowGrayLight,
        )

        assertDistinctTextFamilies(darkPalettes)
        assertDistinctTextFamilies(lightPalettes)
    }

    @Test
    fun `bundled themes have unique preview signatures`() {
        val signatures = AppPalette.ALL.map { palette ->
            listOf(
                palette.surfaceDeep,
                palette.surfaceBase,
                palette.accent,
                palette.accentLight,
                palette.nowLine,
                palette.live,
            )
        }

        assertThat(signatures.toSet().size).isEqualTo(signatures.size)
    }

    @Test
    fun `blue steel replaces cyan cyber punk accent`() {
        val palette = AppPalette.CyberPunk

        assertThat(palette.displayName).contains("Steel")
        assertThat(palette.description.lowercase()).doesNotContain("cyan")
        assertThat(palette.accent).isEqualTo(Color(0xFF6F8FA8))
        assertThat(palette.accent.green).isGreaterThan(palette.accent.red)
        assertThat(palette.accent.blue).isGreaterThan(palette.accent.green)
    }

    @Test
    fun `afterglow dark one through four use visibly different color families`() {
        assertThat(AppPalette.Afterglow1.accent.blue).isGreaterThan(AppPalette.Afterglow1.accent.red)
        assertThat(AppPalette.AfterglowSunset.accent.red).isGreaterThan(AppPalette.AfterglowSunset.accent.blue)
        assertThat(AppPalette.Afterglow3.accent.blue).isGreaterThan(AppPalette.Afterglow3.accent.green)
        assertThat(AppPalette.Afterglow4.accent.red).isGreaterThan(AppPalette.Afterglow4.accent.blue)

        val signatures = listOf(
            AppPalette.Afterglow1,
            AppPalette.AfterglowSunset,
            AppPalette.Afterglow3,
            AppPalette.Afterglow4,
        ).map { palette ->
            listOf(palette.surfaceDeep, palette.surfaceAccent, palette.accent, palette.nowLine, palette.info)
        }

        assertThat(signatures.toSet().size).isEqualTo(signatures.size)
    }

    @Test
    fun `rachels sunset keeps mint teal as the dominant accent`() {
        listOf(AppPalette.SunsetAurora, AppPalette.RachelsSunsetLight).forEach { palette ->
            assertThat(palette.accent.green).isGreaterThan(palette.accent.red)
            assertThat(palette.accent.green).isGreaterThan(palette.accent.blue)
            assertThat(palette.success).isEqualTo(palette.accent)
        }
    }

    @Test
    fun `violet spectrum stays violet instead of becoming a blue theme`() {
        val palette = AppPalette.UltravioletSpectrum

        assertThat(palette.description.lowercase()).doesNotContain("blue")
        assertThat(palette.description.lowercase()).doesNotContain("indigo")
        assertThat(palette.description.lowercase()).doesNotContain("night")
        assertThat(palette.accent).isEqualTo(Color(0xFF7B2CBF))
        assertThat(palette.live).isEqualTo(Color(0xFF9D4EDD))
        listOf(
            palette.surfaceDeep,
            palette.surfaceBase,
            palette.surfaceCool,
            palette.surfaceAccent,
            palette.accent,
            palette.nowLine,
            palette.live,
        ).forEach { color ->
            assertThat(color.blue - color.red).isAtMost(0.32f)
            assertThat(color.green).isLessThan(color.red)
        }
    }

    @Test
    fun `classic blue uses blue surfaces instead of black surfaces`() {
        val palette = AppPalette.ClassicBlue

        assertThat(palette.description.lowercase()).doesNotContain("black")
        listOf(
            palette.surfaceDeep,
            palette.surfaceBase,
            palette.surfaceCool,
            palette.surfaceAccent,
            palette.panelScrim,
            palette.osdScrim,
        ).forEach { color ->
            assertThat(colorDistance(color, Color.Black)).isAtLeast(0.18f)
            assertThat(color.blue).isGreaterThan(color.red)
            assertThat(color.blue).isGreaterThan(color.green)
        }
    }

    @Test
    fun `light mixed palettes use readable active colors`() {
        listOf(
            AppPalette.MineralSlate,
            AppPalette.RachelsSunsetLight,
        ).forEach { palette ->
            assertThat(contrastRatio(palette.accent, palette.surfaceAccent)).isAtLeast(4.5f)
        }
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun assertDistinctTextFamilies(palettes: List<AppPalette>) {
        palettes.forEachIndexed { index, palette ->
            palettes.drop(index + 1).forEach { other ->
                assertThat(colorDistance(palette.textPrimary, other.textPrimary)).isAtLeast(0.06f)
                assertThat(colorDistance(palette.textSecondary, other.textSecondary)).isAtLeast(0.08f)
            }
        }
    }

    private fun colorDistance(a: Color, b: Color): Float {
        val red = a.red - b.red
        val green = a.green - b.green
        val blue = a.blue - b.blue
        return kotlin.math.sqrt(red * red + green * green + blue * blue)
    }
}
