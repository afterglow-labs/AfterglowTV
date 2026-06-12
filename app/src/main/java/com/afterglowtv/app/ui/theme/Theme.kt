package com.afterglowtv.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppShapes
import com.afterglowtv.app.ui.design.LocalAppShapes
import com.afterglowtv.app.ui.design.LocalAppSpacing
import com.afterglowtv.app.ui.design.LocalSafeArea
import com.afterglowtv.app.ui.design.rememberAppTypography
import com.afterglowtv.app.ui.design.resolveSafeArea

@Composable
fun AfterglowTVTheme(content: @Composable () -> Unit) {
    val typography = rememberAppTypography()
    // Read AppColors.palette EXPLICITLY at the top so this composable is
    // subscribed to palette state changes. Without this explicit read,
    // Compose's snapshot tracking through chained property getters on a
    // singleton (AppColors.TiviAccent → palette.accent) was unreliable —
    // theme swaps would persist to DataStore but only render on app
    // restart, not live. Reading `palette` directly + keying the
    // ColorScheme via `remember(palette)` guarantees the recomposition.
    val palette = AppColors.palette
    val colorScheme = remember(palette) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = palette.accentLight,
            onPrimaryContainer = AppColors.contentColorFor(palette.accentLight),
            secondary = palette.accentLight,
            surface = palette.surfaceBase,
            onSurface = AppColors.contentColorFor(palette.surfaceBase),
            surfaceVariant = palette.surfaceCool,
            onSurfaceVariant = AppColors.secondaryContentColorFor(palette.surfaceCool),
            background = palette.surfaceDeep,
            onBackground = AppColors.contentColorFor(palette.surfaceDeep),
            error = palette.nowLine,
            onError = Color(0xFFFFFFFF),
        )
    }
    CompositionLocalProvider(
        LocalAppSpacing provides com.afterglowtv.app.ui.design.AppSpacing(),
        LocalAppShapes provides AppShapes(),
        LocalSafeArea provides resolveSafeArea()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
