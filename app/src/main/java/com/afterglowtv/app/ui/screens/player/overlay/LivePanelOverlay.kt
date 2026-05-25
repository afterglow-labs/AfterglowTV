package com.afterglowtv.app.ui.screens.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * AfterglowTV left-anchored two-column overlay panel.
 *
 *  Categories column (320 dp)  |  Channels column (320 dp)
 *
 * Backed by a 75% black scrim. Slides in from the left in 275 ms with a
 * 150 ms fade. The panel does NOT cover the full screen — the player surface
 * stays visible to the right.
 */
data class LivePanelCategory(val id: String, val name: String, val count: Int)

@Composable
fun LivePanelOverlay(
    visible: Boolean,
    categories: List<LivePanelCategory>,
    selectedCategoryId: String?,
    channels: List<LivePanelChannelRowState>,
    currentChannelIndex: Int?,
    onCategorySelected: (LivePanelCategory) -> Unit,
    onChannelSelected: (LivePanelChannelRowState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(tween(275)) { -it } + fadeIn(tween(150)),
        exit = slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(120)),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(spacing.livePanelWidth)
                    .background(AppColors.PanelScrim),
            ) {
                CategoryColumn(
                    items = categories,
                    selectedId = selectedCategoryId,
                    onSelect = onCategorySelected,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(spacing.livePanelColumn)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                )
                ChannelColumn(
                    items = channels,
                    currentIndex = currentChannelIndex,
                    onSelect = onChannelSelected,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(spacing.livePanelColumn)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryColumn(
    items: List<LivePanelCategory>,
    selectedId: String?,
    onSelect: (LivePanelCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(items = items, key = { it.id }) { cat ->
            Row(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .afterglowFocus(AfterglowFocusRole.Row)
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cat.name,
                    color = if (cat.id == selectedId) AppColors.TiviAccentLight else AppColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = cat.count.toString(),
                    color = AppColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ChannelColumn(
    items: List<LivePanelChannelRowState>,
    currentIndex: Int?,
    onSelect: (LivePanelChannelRowState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val initial = remember(currentIndex) { (currentIndex ?: 0).coerceAtLeast(0) }
    LaunchedEffect(initial) { listState.scrollToItem(initial) }

    LazyColumn(state = listState, modifier = modifier) {
        items(items = items, key = { it.channelNumber + "|" + it.name }) { row ->
            LivePanelChannelRow(state = row, onClick = { onSelect(row) })
        }
    }
}
