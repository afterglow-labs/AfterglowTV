package com.afterglowtv.app.ui.screens.epg

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.model.AdultGuideCategory
import com.afterglowtv.app.ui.theme.FocusBorder
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.app.ui.theme.TextPrimary
import com.afterglowtv.domain.model.Channel
import kotlinx.coroutines.launch

@Composable
internal fun AdultGuideSurface(
    modifier: Modifier = Modifier,
    categories: List<AdultGuideCategory>,
    selectedCategoryKey: String,
    favoriteChannelIds: Set<Long>,
    hasMoreChannels: Boolean,
    isChannelLocked: (Channel) -> Boolean,
    onCategorySelected: (String) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onChannelFocused: (Channel, Boolean) -> Unit,
    onRequestGuideToolbarFocus: () -> Unit,
    onRequestMoreChannels: () -> Unit
) {
    if (categories.isEmpty()) return
    val selectedCategory = categories.firstOrNull { it.key == selectedCategoryKey } ?: categories.first()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdultGuideCategoryRail(
            categories = categories,
            selectedCategoryKey = selectedCategory.key,
            onCategorySelected = onCategorySelected,
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
        )
        AdultGuideChannelList(
            category = selectedCategory,
            favoriteChannelIds = favoriteChannelIds,
            hasMoreChannels = hasMoreChannels,
            isChannelLocked = isChannelLocked,
            onChannelClick = onChannelClick,
            onChannelFocused = onChannelFocused,
            onRequestGuideToolbarFocus = onRequestGuideToolbarFocus,
            onRequestMoreChannels = onRequestMoreChannels,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun AdultGuideCategoryRail(
    categories: List<AdultGuideCategory>,
    selectedCategoryKey: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(categories, key = { it.key }) { category ->
            AdultGuideCategoryButton(
                category = category,
                selected = category.key == selectedCategoryKey,
                onClick = { onCategorySelected(category.key) }
            )
        }
    }
}

@Composable
private fun AdultGuideCategoryButton(
    category: AdultGuideCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceHighlight else SurfaceElevated,
            focusedContainerColor = SurfaceHighlight,
            contentColor = OnSurface,
            focusedContentColor = TextPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .background(if (selected) Primary else Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category.channels.size} channels",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AdultGuideChannelList(
    category: AdultGuideCategory,
    favoriteChannelIds: Set<Long>,
    hasMoreChannels: Boolean,
    isChannelLocked: (Channel) -> Boolean,
    onChannelClick: (Channel) -> Unit,
    onChannelFocused: (Channel, Boolean) -> Unit,
    onRequestGuideToolbarFocus: () -> Unit,
    onRequestMoreChannels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun jumpToGuideToolbar(): Boolean {
        scope.launch {
            if (listState.firstVisibleItemIndex > 0) {
                listState.animateScrollToItem(0)
            }
            onRequestGuideToolbarFocus()
        }
        return true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                val nativeEvent = event.nativeKeyEvent
                if (nativeEvent.action != KeyEvent.ACTION_DOWN) {
                    return@onPreviewKeyEvent false
                }
                when (nativeEvent.keyCode) {
                    KeyEvent.KEYCODE_MOVE_HOME,
                    KeyEvent.KEYCODE_PAGE_UP -> jumpToGuideToolbar()
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        nativeEvent.repeatCount >= 6 &&
                            listState.firstVisibleItemIndex > 0 &&
                            jumpToGuideToolbar()
                    }
                    else -> false
                }
            },
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item(key = "adult-guide-header:${category.key}") {
            AdultGuideGridHeader(category = category)
        }
        itemsIndexed(
            items = category.channels,
            key = { index, channel -> "adult-row:${category.key}:$index:${channel.id}" }
        ) { rowIndex, channel ->
            if (hasMoreChannels && rowIndex >= category.channels.lastIndex - 8) {
                LaunchedEffect(category.channels.size, rowIndex) {
                    onRequestMoreChannels()
                }
            }
            AdultGuideChannelCard(
                channel = channel,
                isFavorite = channel.id in favoriteChannelIds,
                isLocked = isChannelLocked(channel),
                onClick = { onChannelClick(channel) },
                onFocused = { onChannelFocused(channel, rowIndex == 0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
            )
        }
        if (hasMoreChannels) {
            item(key = "adult-guide-load-more:${category.key}") {
                LaunchedEffect(category.channels.size) {
                    onRequestMoreChannels()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(SurfaceElevated, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading more channels...",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurfaceDim
                    )
                }
            }
        }
    }
}

@Composable
private fun AdultGuideGridHeader(category: AdultGuideCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${category.channels.size} looping live channels",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdultGuideChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !isFocused) {
                onFocused()
            }
            isFocused = it.isFocused
        },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight,
            contentColor = OnSurface,
            focusedContentColor = TextPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = FocusSpec.FocusedScale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogoBadge(
                channelName = channel.name,
                logoUrl = channel.logoUrl,
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(7.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (channel.number > 0) "${channel.number}. ${channel.name}" else channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isFocused) TextPrimary else OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.categoryName?.takeIf { it.isNotBlank() }
                        ?: channel.groupTitle?.takeIf { it.isNotBlank() }
                        ?: "Loops daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdultGuideBadge(label = "Loops daily")
                    if (isFavorite) {
                        AdultGuideBadge(label = stringResource(R.string.epg_favorite_badge))
                    }
                    if (isLocked) {
                        AdultGuideBadge(label = stringResource(R.string.home_locked_short), emphasis = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultGuideBadge(
    label: String,
    emphasis: Boolean = false
) {
    Box(
        modifier = Modifier
            .background(
                color = if (emphasis) Primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasis) Primary else OnSurfaceDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
