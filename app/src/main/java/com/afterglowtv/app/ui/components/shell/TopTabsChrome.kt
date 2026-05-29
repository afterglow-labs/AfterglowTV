package com.afterglowtv.app.ui.components.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.LocalAppSpacing
import com.afterglowtv.app.ui.design.AfterglowFocusRole
import com.afterglowtv.app.ui.design.afterglowFocus

/**
 * Afterglow TV top tabs row. Alternative chrome to the existing left rail —
 * meant for the Home / Live TV / Guide / Movies / Series / Recordings /
 * Search / Settings landing screens. The player route ignores this and
 * renders full-bleed.
 */
data class TopTab(val id: String, val label: String)

fun defaultTopTabs(
    developerModeEnabled: Boolean = false,
    showAdultGuideTab: Boolean = false
): List<TopTab> = buildList {
    add(TopTab("home", "Home"))
    add(TopTab("live_tv", "Live TV"))
    add(TopTab("epg", "TV Guide"))
    if (developerModeEnabled) {
        add(TopTab("vod_guide", "VOD Guide"))
        if (showAdultGuideTab) {
            add(TopTab("adult_guide", "Adult Guide"))
        }
    }
    add(TopTab("movies", "Movies"))
    add(TopTab("series", "Series"))
    if (developerModeEnabled) {
        add(TopTab("local_media", "Personal Guide"))
    }
    add(TopTab("favorites", "Favorites"))
    add(TopTab("search", "Search"))
    add(TopTab("settings", "Settings"))
}

val DefaultTopTabs: List<TopTab> = defaultTopTabs()

@Composable
fun TopTabsChrome(
    tabs: List<TopTab>,
    selectedId: String,
    onTabClick: (TopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.tabBarHeight)
            .background(AppColors.TiviSurfaceDeep)
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { tab ->
            val selected = tab.id == selectedId
            Row(
                modifier = Modifier
                    .afterglowFocus(
                        role = AfterglowFocusRole.Pill,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable { onTabClick(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) AppColors.TiviAccentLight else AppColors.TextSecondary,
                )
            }
        }
    }
}
