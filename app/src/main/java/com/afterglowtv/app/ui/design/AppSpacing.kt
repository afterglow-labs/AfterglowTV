package com.afterglowtv.app.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppSpacing(
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp,
    val screenGutter: Dp = 56.dp,
    val railWidth: Dp = 124.dp,
    val sectionGap: Dp = 32.dp,
    val cardGap: Dp = 16.dp,
    val chipGap: Dp = 10.dp,
    val safeTop: Dp = 32.dp,
    val safeBottom: Dp = 32.dp,
    val safeHoriz: Dp = 56.dp,
    // --- AfterglowTV ground-truth dimensions ---------------------------
    val livePanelColumn: Dp = 320.dp,
    val livePanelWidth: Dp = 640.dp,
    val livePanelRowHeight: Dp = 56.dp,
    val livePanelRowGap: Dp = 1.dp,
    val focusStrokeWidth: Dp = 4.dp,
    val focusCornerRadius: Dp = 4.dp,
    val epgSlotWidth: Dp = 270.dp,
    val epgRowHeight: Dp = 72.dp,
    val epgChannelGutter: Dp = 200.dp,
    val epgPipWidth: Dp = 260.dp,
    val epgPipHeight: Dp = 146.dp,
    val infoOsdHeight: Dp = 180.dp,
    val quickSettingsPanelWidth: Dp = 384.dp,
    val tabBarHeight: Dp = 52.dp,
    val tiviNavRailWidth: Dp = 120.dp,
    val nowLineWidth: Dp = 2.dp
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
