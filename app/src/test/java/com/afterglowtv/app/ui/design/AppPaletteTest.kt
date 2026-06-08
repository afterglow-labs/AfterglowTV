package com.afterglowtv.app.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
            AppPalette.AfterglowLabs,
            AppPalette.RachelsSunrise
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
    fun `bundled themes are alphabetized by display name`() {
        val displayNames = AppPalette.ALL.map { it.displayName }

        assertThat(displayNames).containsExactlyElementsIn(
            displayNames.sortedBy { it.lowercase() }
        ).inOrder()
    }

    @Test
    fun `blue steel uses blue steel surfaces instead of black surfaces`() {
        val palette = AppPalette.BlueSteel

        assertThat(palette.displayName).contains("Steel")
        assertThat(palette.description.lowercase()).doesNotContain("cyan")
        assertThat(palette.description.lowercase()).doesNotContain("black")
        assertThat(palette.accent).isEqualTo(Color(0xFF89A8BC))
        assertThat(palette.accent.green).isGreaterThan(palette.accent.red)
        assertThat(palette.accent.blue).isGreaterThan(palette.accent.green)
        listOf(
            palette.surfaceDeep,
            palette.surfaceBase,
            palette.surfaceCool,
            palette.surfaceAccent,
            palette.panelScrim,
            palette.osdScrim,
        ).forEach { color ->
            assertThat(colorDistance(color, Color.Black)).isAtLeast(0.18f)
            assertThat(color.blue).isGreaterThan(color.green)
            assertThat(color.green).isGreaterThan(color.red)
        }
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
    fun `rachels themes keep peach pink mint teal violet and soft blue energy`() {
        val sunset = AppPalette.SunsetAurora
        assertThat(sunset.surfaceCool.red).isGreaterThan(sunset.surfaceCool.green)
        assertThat(sunset.surfaceCool.green).isGreaterThan(sunset.surfaceCool.blue)
        assertThat(sunset.surfaceAccent.red).isGreaterThan(sunset.surfaceAccent.green)
        assertThat(sunset.surfaceAccent.blue).isGreaterThan(sunset.surfaceAccent.green)
        assertThat(sunset.surfaceBase.green).isGreaterThan(sunset.surfaceBase.red)
        assertThat(sunset.surfaceBase.blue).isGreaterThan(sunset.surfaceBase.red)
        assertThat(sunset.accent.blue).isGreaterThan(sunset.accent.red)
        assertThat(sunset.info.blue).isGreaterThan(sunset.info.red)

        val sunrise = AppPalette.RachelsSunrise
        assertThat(sunrise.surfaceDeep.red).isGreaterThan(sunrise.surfaceDeep.blue)
        assertThat(sunrise.surfaceBase.red).isGreaterThan(sunrise.surfaceBase.blue)
        assertThat(sunrise.surfaceCool.red).isGreaterThan(sunrise.surfaceCool.green)
        assertThat(sunrise.surfaceCool.blue).isGreaterThan(sunrise.surfaceCool.green)
        assertThat(sunrise.surfaceAccent.green).isGreaterThan(sunrise.surfaceAccent.red)
        assertThat(sunrise.surfaceAccent.green).isGreaterThan(sunrise.surfaceAccent.blue)
        assertThat(sunrise.accent.blue).isGreaterThan(sunrise.accent.red)
        assertThat(sunrise.accent.luminance()).isLessThan(0.08f)
    }

    @Test
    fun `afterglow labs theme matches the website wash with dark controls`() {
        val palette = AppPalette.AfterglowLabs

        assertThat(palette.displayName).isEqualTo("Afterglow Labs")
        assertThat(palette.surfaceDeep.red).isGreaterThan(palette.surfaceDeep.blue)
        assertThat(palette.surfaceBase.red).isGreaterThan(palette.surfaceBase.blue)
        assertThat(palette.surfaceCool.red).isGreaterThan(palette.surfaceCool.blue)
        assertThat(palette.surfaceAccent.red).isGreaterThan(palette.surfaceAccent.green)
        assertThat(palette.surfaceAccent.blue).isGreaterThan(palette.surfaceAccent.green)
        assertThat(palette.accent.blue).isGreaterThan(palette.accent.red)
        assertThat(palette.accent.luminance()).isLessThan(0.08f)
        assertThat(contrastRatio(palette.accent, palette.surfaceAccent)).isAtLeast(4.5f)
    }

    @Test
    fun `mint and teal background themes avoid green text and controls`() {
        listOf(
            AppPalette.AfterglowLight3,
            AppPalette.RachelsSunrise,
            AppPalette.SunsetAurora,
        ).forEach { palette ->
            assertWithMessage("${palette.displayName} has a green or teal background")
                .that(
                    listOf(
                        palette.surfaceBase,
                        palette.surfaceCool,
                        palette.surfaceAccent,
                    ).any(::isGreenOrTealBackground)
                )
                .isTrue()

            listOf(
                palette.textPrimary,
                palette.textSecondary,
                palette.accent,
                palette.accentLight,
                palette.success,
                palette.info,
            ).forEach { color ->
                assertWithMessage("${palette.displayName} uses green foreground/control $color")
                    .that(isGreenDominant(color))
                    .isFalse()
            }
        }
    }

    @Test
    fun `violet spectrum stays violet instead of becoming a blue theme`() {
        val palette = AppPalette.UltravioletSpectrum

        assertThat(palette.description.lowercase()).doesNotContain("blue")
        assertThat(palette.description.lowercase()).doesNotContain("indigo")
        assertThat(palette.description.lowercase()).doesNotContain("night")
        assertThat(palette.accent).isEqualTo(Color(0xFFA855F0))
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
            AppPalette.AfterglowLabs,
            AppPalette.RachelsSunrise,
        ).forEach { palette ->
            assertWithMessage(palette.displayName)
                .that(contrastRatio(palette.accent, palette.surfaceAccent))
                .isAtLeast(4.5f)
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

    private fun isGreenDominant(color: Color): Boolean =
        color.green > color.red + 0.08f && color.green > color.blue + 0.04f

    private fun isGreenOrTealBackground(color: Color): Boolean =
        color.green > color.red + 0.06f &&
            (color.green > color.blue + 0.04f || color.blue > color.red + 0.06f)
}
