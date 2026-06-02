package com.afterglowtv.app.ui.screens.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.LocalAppSpacing
import com.afterglowtv.app.ui.design.AfterglowFocusRole
import com.afterglowtv.app.ui.design.afterglowFocus

/**
 * AfterglowTV right-anchored quick-settings panel. Slides in from the
 * right (275 ms) over a 75% black scrim. Each row shows a label + the
 * currently-selected value tinted in the theme accent.
 */
data class QuickSettingItem(
    val id: String,
    val label: String,
    val currentValue: String,
)

@Composable
fun QuickSettingsPanel(
    visible: Boolean,
    items: List<QuickSettingItem>,
    onItemClick: (QuickSettingItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(tween(275)) { it } + fadeIn(tween(150)),
        exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(120)),
        modifier = modifier,
    ) {
        val spacing = LocalAppSpacing.current
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(spacing.quickSettingsPanelWidth)
                    .background(AppColors.PanelScrim)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .afterglowFocus(AfterglowFocusRole.Row)
                                .clickable { onItemClick(item) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = item.currentValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TiviAccentLight,
                            )
                        }
                    }
                }
            }
        }
    }
}
