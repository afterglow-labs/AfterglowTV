package com.afterglowtv.app.ui.screens.epg

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.ui.model.isArchivePlayable
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.model.guideLookupKey
import com.afterglowtv.app.ui.time.LocalAppTimeFormat
import com.afterglowtv.app.ui.time.createTimeFormat
import com.afterglowtv.app.ui.theme.FocusBorder
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.Program
import com.afterglowtv.player.PlayerEngine
import java.util.Date

internal data class GuideHeroSelection(
    val channel: Channel,
    val program: Program?,
    val isFallbackToChannel: Boolean
)

internal fun resolveGuideHeroSelection(
    uiState: EpgUiState,
    focusedChannel: Channel?,
    focusedProgram: Program?,
    now: Long
): GuideHeroSelection? {
    val resolvedChannel = focusedChannel
        ?.let { current -> uiState.channels.firstOrNull { it.id == current.id } }
        ?: uiState.channels.firstOrNull()
        ?: return null
    val programs = resolvedChannel.guideLookupKey()?.let { lookupKey ->
        uiState.programsByChannel[lookupKey].orEmpty()
    }.orEmpty()
    val resolvedProgram = focusedProgram?.let { focused ->
        programs.firstOrNull {
            it.startTime == focused.startTime &&
                it.endTime == focused.endTime &&
                it.title == focused.title
        }
    } ?: programs.firstOrNull { now in it.startTime until it.endTime } ?: programs.firstOrNull()
    return GuideHeroSelection(
        channel = resolvedChannel,
        program = resolvedProgram,
        isFallbackToChannel = resolvedProgram == null
    )
}

@Composable
internal fun GuideHeroSection(
    uiState: EpgUiState,
    focusedChannel: Channel?,
    focusedProgram: Program?,
    previewChannelId: Long?,
    previewPlayerEngine: PlayerEngine?,
    isPreviewLoading: Boolean,
    previewErrorMessage: String?,
    modifier: Modifier = Modifier
) {
    val now = currentGuideNow()
    val heroSelection by remember(uiState, focusedChannel, focusedProgram, now) {
        derivedStateOf {
            resolveGuideHeroSelection(
                uiState = uiState,
                focusedChannel = focusedChannel,
                focusedProgram = focusedProgram,
                now = now
            )
        }
    }
    val previewChannel = remember(uiState.channels, previewChannelId) {
        previewChannelId?.let { id -> uiState.channels.firstOrNull { it.id == id } }
    }

    ImmersiveGuideHero(
        selection = heroSelection,
        previewChannel = previewChannel,
        previewPlayerEngine = previewPlayerEngine,
        isPreviewLoading = isPreviewLoading,
        previewErrorMessage = previewErrorMessage,
        providerLabel = uiState.providerSourceLabel,
        selectedCategoryName = uiState.categories
            .firstOrNull { it.id == uiState.selectedCategoryId }
            ?.name
            .orEmpty(),
        isGuideStale = uiState.isGuideStale,
        channelCount = uiState.totalChannelCount,
        channelsWithSchedule = uiState.channelsWithSchedule,
        lastUpdatedAt = uiState.lastUpdatedAt,
        isRefreshing = uiState.isRefreshing,
        modifier = modifier
    )
}

@Composable
internal fun ImmersiveGuideHero(
    selection: GuideHeroSelection?,
    previewChannel: Channel?,
    previewPlayerEngine: PlayerEngine?,
    isPreviewLoading: Boolean,
    previewErrorMessage: String?,
    providerLabel: String,
    selectedCategoryName: String,
    isGuideStale: Boolean,
    channelCount: Int,
    channelsWithSchedule: Int,
    lastUpdatedAt: Long?,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val appTimeFormat = LocalAppTimeFormat.current
    val format = remember(appTimeFormat) { appTimeFormat.createTimeFormat() }
    val currentTime = System.currentTimeMillis()
    val lastUpdatedLabel = remember(lastUpdatedAt, currentTime) {
        lastUpdatedAt?.let { updatedAt ->
            val minutes = ((currentTime - updatedAt).coerceAtLeast(0L) / 60_000L).toInt()
            if (minutes <= 0) {
                "Updated just now"
            } else {
                "$minutes min ago"
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val showInlinePreview = maxWidth >= 860.dp && selection != null
        val heroHeight = if (showInlinePreview) 164.dp else 96.dp

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
            colors = SurfaceDefaults.colors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val channel = selection?.channel
                ChannelLogoBadge(
                    channelName = channel?.name ?: stringResource(R.string.epg_title),
                    logoUrl = channel?.logoUrl,
                    modifier = Modifier
                        .width(54.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val program = selection?.program
                    Text(
                        text = program?.title ?: channel?.name ?: stringResource(R.string.epg_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 18.sp
                        ),
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            channel?.let { append("${it.number}. ${it.name}") }
                            if (providerLabel.isNotBlank()) {
                                if (isNotEmpty()) append("  |  ")
                                append(providerLabel)
                            }
                            if (selectedCategoryName.isNotBlank()) {
                                if (isNotEmpty()) append("  |  ")
                                append(selectedCategoryName)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (program != null) {
                        Text(
                            text = "${format.format(Date(program.startTime))} - ${format.format(Date(program.endTime))}",
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 11.sp),
                            color = OnSurface
                        )
                        if (currentTime in program.startTime until program.endTime) {
                            LinearProgressIndicator(
                                progress = { ((currentTime - program.startTime).toFloat() / (program.endTime - program.startTime).toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Primary,
                                trackColor = SurfaceHighlight
                            )
                        }
                        Text(
                            text = program.description.ifBlank { stringResource(R.string.epg_hero_no_program_description) },
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.epg_no_schedule),
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurface
                        )
                        Text(
                            text = stringResource(R.string.epg_hero_no_schedule_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = if (showInlinePreview) {
                        Modifier.widthIn(min = 150.dp, max = 176.dp)
                    } else {
                        Modifier.widthIn(min = 176.dp, max = 220.dp)
                    },
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    GuideHeroBadge(text = stringResource(R.string.epg_schedule_summary_short, channelsWithSchedule, channelCount))
                    if (isRefreshing) {
                        GuideHeroBadge(text = stringResource(R.string.epg_loading))
                    }
                    if (selection?.program != null && selection.channel.isArchivePlayable(selection.program, currentGuideNow())) {
                        GuideHeroBadge(
                            text = if (selection.program.hasArchive) {
                                stringResource(R.string.epg_program_replay_ready)
                            } else {
                                stringResource(R.string.epg_program_replay_partial)
                            },
                            highlight = true
                        )
                    }
                    if (isGuideStale) {
                        GuideHeroBadge(
                            text = stringResource(R.string.epg_stale_short),
                            accentColor = Color(0xFFFF6B6B)
                        )
                    }
                    if (!lastUpdatedLabel.isNullOrBlank()) {
                        Text(
                            text = lastUpdatedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceDim
                        )
                    }
                }

                if (showInlinePreview) {
                    EpgPipPreview(
                        channel = previewChannel,
                        playerEngine = previewPlayerEngine,
                        isLoading = isPreviewLoading,
                        errorMessage = previewErrorMessage
                    )
                }
            }
        }
    }
}

@Composable
internal fun GuideHeroBadge(
    text: String,
    highlight: Boolean = false,
    accentColor: Color = Primary
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        colors = SurfaceDefaults.colors(
            containerColor = if (highlight) accentColor.copy(alpha = 0.18f) else SurfaceHighlight
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (highlight) accentColor else OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun GuideToolbarRow(
    selectedCategoryName: String,
    firstButtonFocusRequester: FocusRequester? = null,
    manualSortLabel: String? = null,
    onOpenCategoryPicker: () -> Unit,
    onJumpToNow: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenOptions: () -> Unit,
    onManualSort: () -> Unit = {},
    onGuideInteract: () -> Unit,
    appNavigationVisible: Boolean = false,
    onRequestAppNavigation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.onPreviewKeyEvent { event ->
            val nativeEvent = event.nativeKeyEvent
            if (!appNavigationVisible &&
                nativeEvent.action == KeyEvent.ACTION_DOWN &&
                nativeEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
            ) {
                onRequestAppNavigation()
                true
            } else {
                false
            }
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GuideToolbarButton(
            label = selectedCategoryName,
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .then(firstButtonFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            onClick = onOpenCategoryPicker,
            onFocused = onGuideInteract
        )
        GuideToolbarButton(
            label = stringResource(R.string.epg_jump_now),
            onClick = onJumpToNow,
            onFocused = onGuideInteract
        )
        GuideToolbarButton(
            label = stringResource(R.string.epg_search_label),
            onClick = onOpenSearch,
            onFocused = onGuideInteract
        )
        GuideToolbarButton(
            label = stringResource(R.string.epg_options_short),
            onClick = onOpenOptions,
            onFocused = onGuideInteract
        )
        manualSortLabel?.let { label ->
            GuideToolbarButton(
                label = label,
                onClick = onManualSort,
                onFocused = onGuideInteract
            )
        }
    }
}

@Composable
internal fun GuideToolbarButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !focused) onFocused()
            focused = it.isFocused
        },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(14.dp)
            )
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
