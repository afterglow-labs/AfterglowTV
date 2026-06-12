package com.afterglowtv.app.ui.screens.epg

import androidx.compose.ui.graphics.Color
import com.afterglowtv.app.ui.design.AppColors

private const val AFTERGLOW_LABS_THEME_ID = "afterglow_labs"

internal data class EpgThemeColors(
    val screenBackground: Color,
    val heroSurface: Color,
    val heroAccentSurface: Color,
    val toolbarSurface: Color,
    val toolbarFocusedSurface: Color,
    val timelineSurface: Color,
    val timelineText: Color,
    val channelRailSurface: Color,
    val channelFocusedSurface: Color,
    val emptyGridSurface: Color,
    val programSurface: Color,
    val programCurrentSurface: Color,
    val programFocusedSurface: Color,
    val badgeSurface: Color,
    val badgeHighlightSurface: Color,
    val gridLine: Color,
    val focusBorder: Color,
    val nowLine: Color,
    val accentStripe: Color,
) {
    val heroText: Color get() = AppColors.primaryContentColorFor(heroSurface)
    val heroSecondaryText: Color get() = AppColors.secondaryContentColorFor(heroSurface)
    val toolbarText: Color get() = AppColors.primaryContentColorFor(toolbarSurface)
    val toolbarFocusedText: Color get() = AppColors.primaryContentColorFor(toolbarFocusedSurface)
    val badgeText: Color get() = AppColors.primaryContentColorFor(badgeSurface)
}

internal fun epgThemeColors(): EpgThemeColors {
    val palette = AppColors.palette
    if (palette.id == AFTERGLOW_LABS_THEME_ID) {
        return EpgThemeColors(
            screenBackground = Color(0xFFFFBE78),
            heroSurface = Color(0xFFFF73AD),
            heroAccentSurface = Color(0xFFFFA35F),
            toolbarSurface = Color(0xFFFF75AD),
            toolbarFocusedSurface = Color(0xFFFFA35F),
            timelineSurface = Color(0xFFFFC083),
            timelineText = Color(0xFF2B1855),
            channelRailSurface = Color(0xFFFF93BD),
            channelFocusedSurface = Color(0xFFFFB367),
            emptyGridSurface = Color(0xFFFFBFA0),
            programSurface = Color(0xFFF96BAA),
            programCurrentSurface = Color(0xFFFFA15F),
            programFocusedSurface = Color(0xFFFFC063),
            badgeSurface = Color(0xFFFFC083),
            badgeHighlightSurface = Color(0xFF2B1855),
            gridLine = Color(0x66331A5C),
            focusBorder = Color(0xFF2B1855),
            nowLine = Color(0xFF2B1855),
            accentStripe = Color(0xFF6F35D8),
        )
    }

    val programSurface = palette.surfaceCool.copy(alpha = 0.94f)
    return EpgThemeColors(
        screenBackground = palette.surfaceDeep,
        heroSurface = palette.surfaceCool.copy(alpha = 0.94f),
        heroAccentSurface = palette.surfaceAccent.copy(alpha = 0.92f),
        toolbarSurface = palette.surfaceBase.copy(alpha = 0.92f),
        toolbarFocusedSurface = palette.surfaceAccent.copy(alpha = 0.96f),
        timelineSurface = palette.surfaceAccent.copy(alpha = 0.82f),
        timelineText = AppColors.secondaryContentColorFor(palette.surfaceAccent),
        channelRailSurface = palette.surfaceBase.copy(alpha = 0.94f),
        channelFocusedSurface = palette.surfaceAccent.copy(alpha = 0.96f),
        emptyGridSurface = palette.surfaceBase.copy(alpha = 0.70f),
        programSurface = programSurface,
        programCurrentSurface = palette.nowFill.copy(alpha = 0.72f),
        programFocusedSurface = palette.surfaceAccent.copy(alpha = 0.98f),
        badgeSurface = palette.surfaceAccent.copy(alpha = 0.86f),
        badgeHighlightSurface = palette.accent.copy(alpha = 0.24f),
        gridLine = AppColors.contentColorFor(palette.surfaceBase).copy(alpha = 0.16f),
        focusBorder = palette.accentLight,
        nowLine = palette.nowLine,
        accentStripe = palette.accent,
    )
}
