package com.afterglowtv.app.ui.screens.settings

import androidx.compose.ui.graphics.Color
import com.afterglowtv.app.ui.design.AppPalette

internal fun themePreviewSwatches(palette: AppPalette): List<Color> = listOf(
    palette.surfaceDeep,
    palette.surfaceAccent,
    palette.accent,
    palette.accentLight,
    palette.nowLine,
    palette.info,
    palette.live,
    palette.success,
    palette.warning,
).distinct().take(6)

internal fun themeSelectionGradient(palette: AppPalette): List<Color> = listOf(
    palette.surfaceDeep,
    palette.surfaceBase,
    palette.surfaceCool,
    palette.surfaceAccent,
)
