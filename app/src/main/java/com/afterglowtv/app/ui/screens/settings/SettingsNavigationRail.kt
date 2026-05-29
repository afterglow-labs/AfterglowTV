package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.theme.Primary

private data class SettingsNavEntry(
    val category: Int,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
internal fun SettingsNavigationRail(
    selectedCategory: Int,
    focusRequester: FocusRequester,
    developerModeEnabled: Boolean,
    onCategorySelected: (Int) -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    val entries = buildList {
        add(
            SettingsNavEntry(
                category = 0,
                label = stringResource(R.string.settings_providers),
                icon = Icons.Default.Settings,
                accent = Primary
            )
        )
        add(
            SettingsNavEntry(
                category = 1,
                label = stringResource(R.string.settings_playback),
                icon = Icons.Default.PlayArrow,
                accent = Color(0xFF9E8FFF)
            )
        )
        add(
            SettingsNavEntry(
                category = 2,
                label = stringResource(R.string.settings_browsing),
                icon = Icons.Default.Search,
                accent = Color(0xFF26A69A)
            )
        )
        add(
            SettingsNavEntry(
                category = 3,
                label = stringResource(R.string.settings_privacy),
                icon = Icons.Default.Lock,
                accent = Color(0xFFFFB74D)
            )
        )
        if (developerModeEnabled) {
            add(
                SettingsNavEntry(
                    category = 4,
                    label = stringResource(R.string.settings_recording_title),
                    icon = Icons.Default.Star,
                    accent = Color(0xFFEF5350)
                )
            )
            add(
                SettingsNavEntry(
                    category = 5,
                    label = "Local Media",
                    icon = Icons.Default.PlayArrow,
                    accent = Color(0xFF26C6DA)
                )
            )
        }
        add(
            SettingsNavEntry(
                category = 6,
                label = stringResource(R.string.settings_backup_restore),
                icon = Icons.Default.Menu,
                accent = Color(0xFF42A5F5)
            )
        )
        if (developerModeEnabled) {
            add(
                SettingsNavEntry(
                    category = 7,
                    label = "Guide Sources",
                    icon = Icons.Default.Info,
                    accent = Color(0xFF66BB6A)
                )
            )
        }
        add(
            SettingsNavEntry(
                category = 8,
                label = stringResource(R.string.settings_about),
                icon = Icons.Default.Info,
                accent = Color(0xFF78909C)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentPadding = PaddingValues(top = 76.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(entries) { index, entry ->
            SettingsNavItem(
                label = entry.label,
                badgeIcon = entry.icon,
                accentColor = entry.accent,
                isSelected = selectedCategory == entry.category,
                modifier = if (selectedCategory == entry.category) Modifier.focusRequester(focusRequester) else Modifier,
                onClick = { onCategorySelected(entry.category) }
            )
        }
        item {
            SettingsNavItem(
                label = "Themes",
                badgeIcon = Icons.Default.Star,
                accentColor = Color(0xFFFF77FF),
                isSelected = false,
                onClick = { onNavigate(com.afterglowtv.app.navigation.Routes.THEMES) },
            )
        }
        item {
            SettingsNavItem(
                label = "Glow",
                badgeIcon = Icons.Default.Info,
                accentColor = Color(0xFF5EEAD4),
                isSelected = false,
                indent = 28.dp,
                compact = true,
                onClick = { onNavigate(com.afterglowtv.app.navigation.Routes.GLOW_SETTINGS) },
            )
        }
        if (developerModeEnabled) {
            item {
                SettingsNavItem(
                    label = "Customize",
                    badgeIcon = Icons.Default.Edit,
                    accentColor = Color(0xFFFF7A38),
                    isSelected = false,
                    indent = 28.dp,
                    compact = true,
                    onClick = { onNavigate(com.afterglowtv.app.navigation.Routes.STYLE_CUSTOMIZER) },
                )
            }
        }
    }
}
