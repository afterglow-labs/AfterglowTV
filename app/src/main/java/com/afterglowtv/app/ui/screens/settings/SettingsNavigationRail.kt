package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.store.StorePolicySnapshot
import com.afterglowtv.app.ui.theme.Primary

internal const val SETTINGS_CATEGORY_PROVIDERS = 0
internal const val SETTINGS_CATEGORY_PLAYBACK = 1
internal const val SETTINGS_CATEGORY_BROWSING = 2
internal const val SETTINGS_CATEGORY_PRIVACY = 3
internal const val SETTINGS_CATEGORY_RECORDING = 4
internal const val SETTINGS_CATEGORY_LOCAL_MEDIA = 5
internal const val SETTINGS_CATEGORY_BACKUP = 6
internal const val SETTINGS_CATEGORY_EPG_SOURCES = 7
internal const val SETTINGS_CATEGORY_ABOUT = 8
internal const val SETTINGS_CATEGORY_PROVIDERS_VOD = 9

internal fun visibleSettingsCategoryIds(
    policy: StorePolicySnapshot = StorePolicy.current,
    developerModeEnabled: Boolean = false
): List<Int> = buildList {
    if (policy.guideOnlyReviewSurface && !developerModeEnabled) {
        add(SETTINGS_CATEGORY_PROVIDERS)
        add(SETTINGS_CATEGORY_PLAYBACK)
        add(SETTINGS_CATEGORY_BROWSING)
        add(SETTINGS_CATEGORY_PRIVACY)
        add(SETTINGS_CATEGORY_BACKUP)
        add(SETTINGS_CATEGORY_EPG_SOURCES)
        add(SETTINGS_CATEGORY_ABOUT)
        return@buildList
    }
    add(SETTINGS_CATEGORY_PROVIDERS)
    add(SETTINGS_CATEGORY_PROVIDERS_VOD)
    add(SETTINGS_CATEGORY_PLAYBACK)
    add(SETTINGS_CATEGORY_BROWSING)
    add(SETTINGS_CATEGORY_PRIVACY)
    if (policy.canUseDvr(developerModeEnabled)) {
        add(SETTINGS_CATEGORY_RECORDING)
    }
    add(SETTINGS_CATEGORY_LOCAL_MEDIA)
    add(SETTINGS_CATEGORY_BACKUP)
    add(SETTINGS_CATEGORY_EPG_SOURCES)
    add(SETTINGS_CATEGORY_ABOUT)
}

private data class SettingsNavEntry(
    val categoryId: Int,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
internal fun SettingsNavigationRail(
    selectedCategory: Int,
    developerModeEnabled: Boolean,
    focusRequester: FocusRequester,
    onCategorySelected: (Int) -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    val policy = StorePolicy.currentFor(developerModeEnabled)
    val effectiveDeveloperModeEnabled = StorePolicy.effectiveDeveloperModeEnabled(developerModeEnabled)
    val visibleCategoryIds = visibleSettingsCategoryIds(
        policy = policy,
        developerModeEnabled = effectiveDeveloperModeEnabled
    )
    val entries = listOf(
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_PLAYBACK,
            label = stringResource(R.string.settings_playback),
            icon = Icons.Default.PlayArrow,
            accent = Color(0xFF9E8FFF)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_BROWSING,
            label = stringResource(R.string.settings_browsing),
            icon = Icons.Default.Search,
            accent = Color(0xFF26A69A)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_PRIVACY,
            label = stringResource(R.string.settings_privacy),
            icon = Icons.Default.Lock,
            accent = Color(0xFFFFB74D)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_RECORDING,
            label = stringResource(R.string.settings_recording_title),
            icon = Icons.Default.Star,
            accent = Color(0xFFEF5350)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_LOCAL_MEDIA,
            label = "Local Media",
            icon = Icons.Default.PlayArrow,
            accent = Color(0xFF26C6DA)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_BACKUP,
            label = stringResource(R.string.settings_backup_restore),
            icon = Icons.Default.Menu,
            accent = Color(0xFF42A5F5)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_EPG_SOURCES,
            label = "EPG Sources",
            icon = Icons.Default.Info,
            accent = Color(0xFF66BB6A)
        ),
        SettingsNavEntry(
            categoryId = SETTINGS_CATEGORY_ABOUT,
            label = stringResource(R.string.settings_about),
            icon = Icons.Default.Info,
            accent = Color(0xFF78909C)
        )
    ).filter { it.categoryId in visibleCategoryIds }

    LazyColumn(
        modifier = Modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentPadding = PaddingValues(top = 76.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (SETTINGS_CATEGORY_PROVIDERS in visibleCategoryIds) {
            item {
                SettingsNavItem(
                    label = stringResource(R.string.settings_providers),
                    badgeIcon = Icons.Default.Settings,
                    accentColor = Primary,
                    isSelected = false,
                    onClick = { onCategorySelected(SETTINGS_CATEGORY_PROVIDERS) }
                )
            }
            item {
                SettingsNavItem(
                    label = "Live TV",
                    badgeIcon = Icons.Default.PlayArrow,
                    accentColor = Primary,
                    isSelected = selectedCategory == SETTINGS_CATEGORY_PROVIDERS,
                    modifier = if (selectedCategory == SETTINGS_CATEGORY_PROVIDERS) Modifier.focusRequester(focusRequester) else Modifier,
                    indent = 28.dp,
                    compact = true,
                    onClick = { onCategorySelected(SETTINGS_CATEGORY_PROVIDERS) }
                )
            }
        }
        if (SETTINGS_CATEGORY_PROVIDERS_VOD in visibleCategoryIds) {
            item {
                SettingsNavItem(
                    label = "VOD",
                    badgeIcon = Icons.Default.Star,
                    accentColor = Color(0xFFFFA64D),
                    isSelected = selectedCategory == SETTINGS_CATEGORY_PROVIDERS_VOD,
                    modifier = if (selectedCategory == SETTINGS_CATEGORY_PROVIDERS_VOD) Modifier.focusRequester(focusRequester) else Modifier,
                    indent = 28.dp,
                    compact = true,
                    onClick = { onCategorySelected(SETTINGS_CATEGORY_PROVIDERS_VOD) }
                )
            }
        }
        items(entries, key = { it.categoryId }) { entry ->
            SettingsNavItem(
                label = entry.label,
                badgeIcon = entry.icon,
                accentColor = entry.accent,
                isSelected = selectedCategory == entry.categoryId,
                modifier = if (selectedCategory == entry.categoryId) Modifier.focusRequester(focusRequester) else Modifier,
                onClick = { onCategorySelected(entry.categoryId) }
            )
        }
        item {
            SettingsNavItem(
                label = "Appearance",
                badgeIcon = Icons.Default.Star,
                accentColor = Color(0xFFFF77FF),
                isSelected = false,
                onClick = { onNavigate(com.afterglowtv.app.navigation.Routes.THEMES) },
            )
        }
        item {
            SettingsNavItem(
                label = "Themes",
                badgeIcon = Icons.Default.Star,
                accentColor = Color(0xFFFF77FF),
                isSelected = false,
                indent = 28.dp,
                compact = true,
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
