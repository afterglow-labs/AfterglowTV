package com.afterglowtv.app.ui.design

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Tiny façade over the currently-active [AppPalette]. Every color in the app
 * resolves through here. `palette` is reactive (mutableStateOf), so changing
 * it from any composable triggers a recomposition and every screen repaints
 * in the new theme — no restart needed.
 *
 * For non-composable callers (e.g. notifications), reads still work but are
 * a one-shot snapshot of the value at call time.
 */
object AppColors {
    /** The active theme. Mutate via [applyPalette]. */
    var palette: AppPalette by mutableStateOf(AppPalette.AfterglowSunset)
        private set

    /** Whether large app backdrops should use blended gradients instead of solid surfaces. */
    var backgroundGradientsEnabled: Boolean by mutableStateOf(true)
        private set

    /** Swap the active palette. Triggers a Compose recomposition. */
    fun applyPalette(next: AppPalette) {
        palette = next
    }

    fun applyBackgroundGradientsEnabled(enabled: Boolean) {
        backgroundGradientsEnabled = enabled
    }

    // --- AfterglowTV identity palette accessors (delegate to active palette) --
    val TiviSurfaceDeep: Color get() = palette.surfaceDeep
    val TiviSurfaceBase: Color get() = palette.surfaceBase
    val TiviSurfaceCool: Color get() = palette.surfaceCool
    val TiviSurfaceAccent: Color get() = palette.surfaceAccent
    val TiviAccent: Color get() = palette.accent
    val TiviAccentLight: Color get() = palette.accentLight
    val TiviAccentMuted: Color get() = palette.accentMuted
    val PanelScrim: Color get() = palette.panelScrim
    val OsdScrim: Color get() = palette.osdScrim
    val EpgNowLine: Color get() = palette.nowLine
    val EpgNowFill: Color get() = palette.nowFill
    val PipPreviewOutline: Color get() = palette.pipPreviewOutline
    val FocusFill: Color get() = palette.focusFill

    // --- Legacy aliases (re-pointed at the active palette) -------------------
    val Canvas: Color get() = palette.surfaceDeep
    val CanvasElevated: Color get() = palette.surfaceBase
    val Surface: Color get() = palette.surfaceBase
    val SurfaceElevated: Color get() = palette.surfaceCool
    val SurfaceEmphasis: Color get() = palette.surfaceCool
    val SurfaceAccent: Color get() = palette.surfaceAccent

    val Brand: Color get() = palette.accent
    val BrandMuted: Color get() = palette.accentMuted
    val BrandStrong: Color get() = palette.accentLight
    val Focus: Color get() = palette.accentLight

    val TextPrimary: Color get() = primaryContentColorFor(Surface)
    val TextSecondary: Color get() = secondaryContentColorFor(Surface)
    val TextTertiary: Color get() = tertiaryContentColorFor(Surface)
    val TextDisabled: Color get() = contentColorFor(Surface).copy(alpha = 0.38f)

    val Live: Color get() = palette.live
    val Success: Color get() = palette.success
    val Warning: Color get() = palette.warning
    val Info: Color get() = palette.info

    val Divider: Color get() = palette.divider
    val Outline: Color get() = palette.outline

    val HeroTop: Color get() = palette.surfaceDeep.copy(alpha = 0.8f)
    val HeroBottom: Color get() = palette.surfaceDeep.copy(alpha = 0.95f)

    fun primaryContentColorFor(background: Color): Color =
        contentColorFor(background).copy(alpha = 1f)

    fun secondaryContentColorFor(background: Color): Color =
        contentColorFor(background).copy(alpha = 0.78f)

    fun tertiaryContentColorFor(background: Color): Color =
        contentColorFor(background).copy(alpha = 0.58f)

    fun contentColorFor(background: Color): Color {
        val effectiveBackground = background.compositeOver(palette.surfaceDeep)
        val light = Color.White
        val dark = Color(0xFF121624)
        return if (contrastRatio(light, effectiveBackground) >= contrastRatio(dark, effectiveBackground)) {
            light
        } else {
            dark
        }
    }

    fun ensureReadableColor(
        color: Color,
        background: Color,
        minimumContrast: Float = 4.5f
    ): Color {
        val effectiveBackground = background.compositeOver(palette.surfaceDeep)
        return if (contrastRatio(color, effectiveBackground) >= minimumContrast) {
            color
        } else {
            contentColorFor(background)
        }
    }

    private fun Color.compositeOver(background: Color): Color {
        if (alpha >= 1f) return this
        val outAlpha = alpha + background.alpha * (1f - alpha)
        if (outAlpha <= 0f) return Color.Transparent
        return Color(
            red = (red * alpha + background.red * background.alpha * (1f - alpha)) / outAlpha,
            green = (green * alpha + background.green * background.alpha * (1f - alpha)) / outAlpha,
            blue = (blue * alpha + background.blue * background.alpha * (1f - alpha)) / outAlpha,
            alpha = outAlpha
        )
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val foregroundLuminance = foreground.relativeLuminance()
        val backgroundLuminance = background.relativeLuminance()
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun Color.relativeLuminance(): Float {
        fun channel(value: Float): Float =
            if (value <= 0.03928f) {
                value / 12.92f
            } else {
                ((value + 0.055f) / 1.055f).pow(2.4f)
            }
        return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
    }

    private fun Float.pow(power: Float): Float =
        Math.pow(this.toDouble(), power.toDouble()).toFloat()
}
