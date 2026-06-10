package com.afterglowtv.app.ui.screens.epg

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppShapeSet
import com.afterglowtv.app.ui.design.AppStyles
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.app.ui.interaction.TvButton
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.model.guideLookupKey
import com.afterglowtv.app.ui.time.LocalAppTimeFormat
import com.afterglowtv.app.ui.time.createTimeFormatter
import com.afterglowtv.app.ui.theme.FocusBorder
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.app.ui.theme.TextPrimary
import com.afterglowtv.app.ui.theme.TextSecondary
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.Program
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlinx.coroutines.launch

private const val AFTERGLOW_LABS_THEME_ID = "afterglow_labs"
private val LabsGuidePanel = Color(0xFFE6865E)
private val LabsGuidePanelFocused = Color(0xFFFFA15F)
private val LabsGuideRail = Color(0xFFE07D56)
private val LabsGuideSlot = Color(0xFFF1A06A)
private val LabsGuideOutline = Color(0xFF6F35D8)
private val LabsGuideOutlineWarm = Color(0xFFFF7A18)
private val LabsGuideText = Color(0xFF101426)
private val LabsGuideTextMuted = Color(0xB0101426)
private val LabsGuideDivider = Color(0x44331A5C)

private val isAfterglowLabsGuideTheme: Boolean
    get() = AppColors.palette.id == AFTERGLOW_LABS_THEME_ID

@Composable
internal fun GuideMessageState(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim
                )
            }
            if (actionLabel != null && onAction != null) {
                TvButton(
                    onClick = onAction,
                    colors = ButtonDefaults.colors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
internal fun EpgGrid(
    modifier: Modifier = Modifier,
    channels: List<Channel>,
    favoriteChannelIds: Set<Long>,
    programsByChannel: Map<String, List<Program>>,
    guideWindowStart: Long,
    guideWindowEnd: Long,
    density: GuideDensity,
    onChannelClick: (Channel) -> Unit,
    onProgramClick: (Channel, Program) -> Unit,
    onChannelFocused: (Channel, Program?, Boolean) -> Unit,
    onProgramFocused: (Channel, Program, Boolean) -> Unit,
    onRequestGuideToolbarFocus: () -> Unit = {},
    onRequestMoreChannels: () -> Unit = {}
) {
    val channelRailWidth = 180.dp
    val timelineGap = 0.dp
    val rowHeight = when (density) {
        GuideDensity.COMPACT -> 36.dp
        GuideDensity.COMFORTABLE -> 52.dp
        GuideDensity.CINEMATIC -> 68.dp
    }
    val horizontalScrollState = rememberScrollState()
    val verticalListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun jumpToGuideToolbar(): Boolean {
        scope.launch {
            if (verticalListState.firstVisibleItemIndex > 0) {
                verticalListState.animateScrollToItem(0)
            }
            onRequestGuideToolbarFocus()
        }
        return true
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        val timelineViewportWidth = (maxWidth - channelRailWidth - timelineGap).coerceAtLeast(640.dp)
        val totalDuration = (guideWindowEnd - guideWindowStart).coerceAtLeast(1L)
        val visibleDurationMs = 3 * 60 * 60 * 1000L
        val calculatedTimelineWidth = timelineViewportWidth * (totalDuration.toFloat() / visibleDurationMs.toFloat())
        val totalTimelineWidth = if (calculatedTimelineWidth > timelineViewportWidth) {
            calculatedTimelineWidth
        } else {
            timelineViewportWidth
        }
        val markerStepMs = EpgViewModel.HALF_HOUR_SHIFT_MS

        Column(modifier = Modifier.fillMaxSize()) {
            GuideTimelineHeader(
                windowStart = guideWindowStart,
                windowEnd = guideWindowEnd,
                channelRailWidth = channelRailWidth,
                timelineGap = timelineGap,
                timelineViewportWidth = timelineViewportWidth,
                totalTimelineWidth = totalTimelineWidth,
                markerStepMs = markerStepMs,
                scrollState = horizontalScrollState
            )
            Spacer(modifier = Modifier.height(0.dp))
            LazyColumn(
                modifier = Modifier
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
                                    verticalListState.firstVisibleItemIndex > 0 &&
                                    jumpToGuideToolbar()
                            }
                            else -> false
                        }
                },
                state = verticalListState,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(
                    items = channels,
                    key = { index, channel -> epgChannelKey(channel, index) }
                ) { index, channel ->
                    if (index >= channels.size - 15) {
                        LaunchedEffect(channels.size) { onRequestMoreChannels() }
                    }
                    val programs = channel.guideLookupKey()?.let { lookupKey ->
                        programsByChannel[lookupKey].orEmpty()
                    }.orEmpty()
                    val isFirstRow = index == 0
                    EpgRow(
                        channel = channel,
                        isFavorite = channel.id in favoriteChannelIds,
                        programs = programs,
                        windowStart = guideWindowStart,
                        windowEnd = guideWindowEnd,
                        channelRailWidth = channelRailWidth,
                        timelineGap = timelineGap,
                        timelineViewportWidth = timelineViewportWidth,
                        totalTimelineWidth = totalTimelineWidth,
                        density = density,
                        rowHeight = rowHeight,
                        markerStepMs = markerStepMs,
                        scrollState = horizontalScrollState,
                        onChannelClick = { onChannelClick(channel) },
                        onChannelFocused = { onChannelFocused(channel, it, isFirstRow) },
                        onProgramClick = { program -> onProgramClick(channel, program) },
                        onProgramFocused = { program -> onProgramFocused(channel, program, isFirstRow) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideTimelineHeader(
    windowStart: Long,
    windowEnd: Long,
    channelRailWidth: Dp,
    timelineGap: Dp,
    timelineViewportWidth: Dp,
    totalTimelineWidth: Dp,
    markerStepMs: Long,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val now = currentGuideNow()
    val appTimeFormat = LocalAppTimeFormat.current
    val hourFormat = remember(appTimeFormat) { appTimeFormat.createTimeFormatter() }
    val zone = remember { ZoneId.systemDefault() }
    val totalDuration = (windowEnd - windowStart).coerceAtLeast(1L)
    val clampedNow = now.coerceIn(windowStart, windowEnd)
    val elapsedRatio = ((clampedNow - windowStart).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val markerLabel = if (now in windowStart..windowEnd) {
        stringResource(R.string.epg_now_marker, hourFormat.format(Instant.ofEpochMilli(now).atZone(zone)))
    } else {
        stringResource(R.string.epg_outside_window)
    }
    val timeSlots = buildList {
        var marker = windowStart
        while (marker < windowEnd) {
            add(marker)
            marker += markerStepMs
        }
    }
    val labsGuide = isAfterglowLabsGuideTheme
    val slotShape = RoundedCornerShape(0.dp)
    val timelineTextColor = if (labsGuide) LabsGuideTextMuted else OnSurfaceDim
    val gridLineColor = if (labsGuide) LabsGuideDivider else Color.White.copy(alpha = 0.14f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(modifier = Modifier.width(channelRailWidth + timelineGap))
        Column(
            modifier = Modifier.width(timelineViewportWidth),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.epg_timeline_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = timelineTextColor
                )
                Text(
                    text = markerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        labsGuide -> LabsGuideText
                        now in windowStart..windowEnd -> Primary
                        else -> timelineTextColor
                    }
                )
            }
            Box(
                modifier = Modifier
                    .width(timelineViewportWidth)
                    .height(28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .width(totalTimelineWidth)
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(totalTimelineWidth)
                            .height(28.dp)
                    ) {
                        timeSlots.forEach { marker ->
                            val slotStartRatio = ((marker - windowStart).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                            val slotEnd = (marker + markerStepMs).coerceAtMost(windowEnd)
                            val slotWidthRatio = ((slotEnd - marker).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .offset(x = totalTimelineWidth * slotStartRatio)
                                    .width(totalTimelineWidth * slotWidthRatio)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (labsGuide) LabsGuideSlot else SurfaceElevated,
                                        shape = slotShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = gridLineColor,
                                        shape = slotShape
                                    ),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = hourFormat.format(Instant.ofEpochMilli(marker).atZone(zone)),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (labsGuide) LabsGuideText else OnSurfaceDim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (now in windowStart..windowEnd) {
                            Box(
                                modifier = Modifier
                                    .offset(x = totalTimelineWidth * elapsedRatio)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(if (labsGuide) LabsGuideText else Primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpgRow(
    channel: Channel,
    isFavorite: Boolean,
    programs: List<Program>,
    windowStart: Long,
    windowEnd: Long,
    channelRailWidth: Dp,
    timelineGap: Dp,
    timelineViewportWidth: Dp,
    totalTimelineWidth: Dp,
    density: GuideDensity,
    rowHeight: Dp,
    markerStepMs: Long,
    scrollState: androidx.compose.foundation.ScrollState,
    onChannelClick: () -> Unit,
    onChannelFocused: (Program?) -> Unit,
    onProgramClick: (Program) -> Unit,
    onProgramFocused: (Program) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val now = currentGuideNow()
    val currentProgram by remember(programs, now) {
        derivedStateOf { programs.currentProgramAt(now) }
    }
    val totalDuration = (windowEnd - windowStart).coerceAtLeast(1L)
    val channelPaddingVertical = when (density) {
        GuideDensity.COMPACT -> 3.dp
        GuideDensity.COMFORTABLE -> 4.dp
        GuideDensity.CINEMATIC -> 5.dp
    }
    val channelLogoSize = when (density) {
        GuideDensity.COMPACT -> 22.dp
        GuideDensity.COMFORTABLE -> 30.dp
        GuideDensity.CINEMATIC -> 38.dp
    }
    val labsGuide = isAfterglowLabsGuideTheme
    val channelContainerColor = if (labsGuide) LabsGuideRail else SurfaceElevated
    val channelFocusedColor = if (labsGuide) LabsGuidePanelFocused else SurfaceHighlight
    val channelFocusedBorderColor = if (labsGuide) LabsGuideOutline else FocusBorder
    val timelineTrackColor = if (labsGuide) LabsGuideRail else SurfaceElevated
    val guidePrimaryTextColor = if (labsGuide) LabsGuideText else OnSurface
    val guideFocusedTextColor = if (labsGuide) LabsGuideText else TextPrimary
    val guideSecondaryTextColor = if (labsGuide) LabsGuideTextMuted else OnSurfaceDim
    val rowShape = RoundedCornerShape(0.dp)
    val rowGridLineColor = if (labsGuide) LabsGuideDivider else Color.White.copy(alpha = 0.10f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvClickableSurface(
            onClick = onChannelClick,
            modifier = Modifier
                .width(channelRailWidth)
                .fillMaxHeight()
                .onFocusChanged {
                    if (it.isFocused && !isFocused) {
                        onChannelFocused(currentProgram)
                    }
                    isFocused = it.isFocused
                },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = channelContainerColor,
                focusedContainerColor = channelFocusedColor
            ),
            shape = ClickableSurfaceDefaults.shape(rowShape),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(1.dp, rowGridLineColor),
                    shape = rowShape
                ),
                focusedBorder = Border(
                    border = BorderStroke(2.dp, channelFocusedBorderColor),
                    shape = rowShape
                )
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 7.dp, vertical = channelPaddingVertical)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogoBadge(
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    modifier = Modifier
                        .width(channelLogoSize)
                        .height(channelLogoSize),
                    shape = RoundedCornerShape(6.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = if (channel.number > 0) "${channel.number}. ${channel.name}" else channel.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isFocused) guideFocusedTextColor else guidePrimaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentProgram?.title ?: stringResource(R.string.epg_no_schedule_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = guideSecondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isFavorite || channel.catchUpSupported) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Primary.copy(alpha = 0.16f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (channel.catchUpSupported) stringResource(R.string.player_archive_badge) else stringResource(R.string.epg_favorite_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(timelineGap))

        Box(
            modifier = Modifier
                .width(timelineViewportWidth)
                .fillMaxHeight()
                .background(timelineTrackColor, rowShape)
                .clip(rowShape)
        ) {
            Row(
                modifier = Modifier
                    .width(totalTimelineWidth)
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(totalTimelineWidth)
                        .fillMaxHeight()
                ) {
                val markers = remember(windowStart, windowEnd, markerStepMs) {
                    buildList {
                        var marker = windowStart
                        while (marker <= windowEnd) {
                            add(marker)
                            marker += markerStepMs
                        }
                    }
                }
                markers.forEach { marker ->
                    val markerRatio = ((marker - windowStart).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .offset(x = totalTimelineWidth * markerRatio)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(if (labsGuide) LabsGuideDivider else Color.White.copy(alpha = 0.08f))
                    )
                }
                if (programs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.epg_no_schedule_short), color = guideSecondaryTextColor)
                    }
                } else {
                    programs.forEach { program ->
                        ProgramItem(
                            program = program,
                            density = density,
                            windowStart = windowStart,
                            windowEnd = windowEnd,
                            totalTimelineWidth = totalTimelineWidth,
                            onClick = { onProgramClick(program) },
                            onFocused = { onProgramFocused(program) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun ProgramItem(
    program: Program,
    density: GuideDensity,
    windowStart: Long,
    windowEnd: Long,
    totalTimelineWidth: Dp,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val now = currentGuideNow()
    val isPlaceholder = program.isPlaceholder
    val isCurrent = !isPlaceholder && now in program.startTime until program.endTime
    val cellStyle = AppStyles.value.epgCell
    val liveStyle = AppStyles.value.epgLiveCell
    val cellShape: Shape = RoundedCornerShape(0.dp)

    val appTimeFormat = LocalAppTimeFormat.current
    val format = remember(appTimeFormat) { appTimeFormat.createTimeFormatter() }
    val zone = remember { ZoneId.systemDefault() }
    val startStr = format.format(Instant.ofEpochMilli(program.startTime).atZone(zone))
    val endStr = format.format(Instant.ofEpochMilli(program.endTime).atZone(zone))
    val totalDuration = (windowEnd - windowStart).coerceAtLeast(1L)
    val positionedStartTime = remember(program.startTime, windowStart, windowEnd, zone) {
        snapProgramTimeToDisplayedMinuteBoundary(program.startTime, zone, windowStart, windowEnd)
    }
    val positionedEndTime = remember(program.endTime, positionedStartTime, windowStart, windowEnd, zone) {
        snapProgramTimeToDisplayedMinuteBoundary(program.endTime, zone, windowStart, windowEnd)
            .coerceAtLeast(positionedStartTime + 1)
    }
    val visibleStart = max(positionedStartTime, windowStart)
    val visibleEnd = max(visibleStart + 1, minOf(positionedEndTime, windowEnd))
    val startRatio = ((visibleStart - windowStart).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val widthRatio = ((visibleEnd - visibleStart).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val itemStart = totalTimelineWidth * startRatio
    val minimumItemWidth = when (density) {
        GuideDensity.COMPACT -> 40.dp
        GuideDensity.COMFORTABLE -> 58.dp
        GuideDensity.CINEMATIC -> 76.dp
    }
    val itemWidth = (totalTimelineWidth * widthRatio).coerceAtLeast(minimumItemWidth)
    val isCompactCell = itemWidth < 148.dp
    val isVeryCompactCell = itemWidth < 116.dp
    val outerVerticalPadding = 0.dp
    val innerHorizontalPadding = when {
        isVeryCompactCell -> 5.dp
        isCompactCell -> 6.dp
        else -> 8.dp
    }
    val innerVerticalPadding = when (density) {
        GuideDensity.COMPACT -> 2.dp
        GuideDensity.COMFORTABLE -> 5.dp
        GuideDensity.CINEMATIC -> 7.dp
    }
    val titleStyle = when {
        isVeryCompactCell -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            lineHeight = 11.sp
        )
        isCompactCell || density == GuideDensity.COMPACT -> MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.sp,
            lineHeight = 12.sp
        )
        density == GuideDensity.CINEMATIC -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 15.sp,
            lineHeight = 17.sp
        )
        else -> MaterialTheme.typography.labelLarge.copy(
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
    val timeStyle = when {
        isCompactCell || density == GuideDensity.COMPACT -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            lineHeight = 10.sp
        )
        density == GuideDensity.CINEMATIC -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            lineHeight = 13.sp
        )
        else -> MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            lineHeight = 11.sp
        )
    }
    val leadingStripeWidth = when (cellStyle) {
        AppShapeSet.EpgCellStyle.ACCENT_STRIPE -> 4.dp
        else -> 0.dp
    }
    val contentStartPadding = innerHorizontalPadding + leadingStripeWidth
    val showCornerTag = isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.CORNER_TAG && !isVeryCompactCell
    val modifierWithGlow = Modifier
        .offset(x = itemStart)
        .padding(top = outerVerticalPadding, bottom = outerVerticalPadding)
        .width(itemWidth)
        .fillMaxHeight()
        .let { base ->
            if (isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.GLOW) {
                base.afterglow(
                    listOf(
                        GlowSpec(AppColors.EpgNowLine, 12.dp, 0.55f),
                        GlowSpec(AppColors.TiviAccent, 26.dp, 0.28f),
                    ),
                    cellShape
                )
            } else {
                base
            }
        }

    TvClickableSurface(
        onClick = onClick,
        modifier = modifierWithGlow
            .onFocusChanged {
                if (it.isFocused && !isFocused) {
                    onFocused()
                }
                isFocused = it.isFocused
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = epgCellContainerColor(cellStyle, liveStyle, isCurrent),
            focusedContainerColor = epgCellFocusedColor(cellStyle, isCurrent)
        ),
        shape = ClickableSurfaceDefaults.shape(cellShape),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = epgCellBorderWidth(cellStyle, liveStyle, isCurrent),
                    color = epgCellBorderColor(cellStyle, liveStyle, isCurrent)
                ),
                shape = cellShape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, if (isAfterglowLabsGuideTheme) LabsGuideOutline else FocusBorder),
                shape = cellShape
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cellStyle == AppShapeSet.EpgCellStyle.ACCENT_STRIPE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            if (isAfterglowLabsGuideTheme) {
                                LabsGuideOutlineWarm
                            } else if (isCurrent) {
                                AppColors.EpgNowLine
                            } else {
                                AppColors.TiviAccent
                            }
                        )
                )
            }
            if (cellStyle == AppShapeSet.EpgCellStyle.BEVELED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isAfterglowLabsGuideTheme) LabsGuideDivider else Color.White.copy(alpha = 0.22f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isAfterglowLabsGuideTheme) LabsGuideDivider else Color.Black.copy(alpha = 0.28f))
                )
            }
            if (
                cellStyle == AppShapeSet.EpgCellStyle.DOUBLE_EDGE ||
                (isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.DOUBLE_EDGE)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(
                            width = 1.dp,
                            color = if (isAfterglowLabsGuideTheme) {
                                LabsGuideDivider
                            } else if (isCurrent) {
                                AppColors.EpgNowLine.copy(alpha = 0.78f)
                            } else {
                                Color.White.copy(alpha = 0.16f)
                            },
                            shape = cellShape
                        )
                )
            }
            if (showCornerTag) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = AppColors.EpgNowLine,
                            shape = RoundedCornerShape(bottomStart = 5.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "NOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            lineHeight = 9.sp
                        ),
                        color = AppColors.TiviSurfaceDeep,
                        maxLines = 1
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(
                        start = contentStartPadding,
                        end = innerHorizontalPadding,
                        top = innerVerticalPadding,
                        bottom = innerVerticalPadding
                    )
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                val shouldDrawPlaceholderText = !isPlaceholder || program.title.isNotBlank() || program.description.isNotBlank()
                if (shouldDrawPlaceholderText) {
                    Text(
                        text = program.title,
                        style = titleStyle,
                        color = if (isPlaceholder && !isFocused) {
                            epgProgramMetaColor(isFocused)
                        } else {
                            epgProgramTitleColor(liveStyle, isCurrent, isFocused)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isVeryCompactCell && !showCornerTag) {
                        Text(
                            text = if (isPlaceholder) {
                                program.description.ifBlank { stringResource(R.string.epg_no_schedule_short) }
                            } else {
                                "$startStr - $endStr"
                            },
                            style = timeStyle,
                            color = epgProgramMetaColor(isFocused),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun epgCellContainerColor(
    style: AppShapeSet.EpgCellStyle,
    liveStyle: AppShapeSet.EpgLiveCellStyle,
    isCurrent: Boolean
): Color {
    if (isAfterglowLabsGuideTheme) {
        return if (isCurrent) {
            LabsGuidePanelFocused
        } else {
            when (style) {
                AppShapeSet.EpgCellStyle.RECTANGULAR -> LabsGuidePanel
                AppShapeSet.EpgCellStyle.SOFT -> LabsGuidePanel
                AppShapeSet.EpgCellStyle.ACCENT_STRIPE -> LabsGuidePanel
                AppShapeSet.EpgCellStyle.BEVELED -> Color(0xFFD97754)
                AppShapeSet.EpgCellStyle.DOUBLE_EDGE -> LabsGuidePanel
            }
        }
    }
    if (isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.NOW_FILL) {
        return AppColors.EpgNowFill.copy(alpha = 0.34f)
    }
    return when (style) {
        AppShapeSet.EpgCellStyle.RECTANGULAR -> AppColors.TiviSurfaceBase.copy(alpha = 0.92f)
        AppShapeSet.EpgCellStyle.SOFT -> AppColors.TiviSurfaceCool.copy(alpha = 0.94f)
        AppShapeSet.EpgCellStyle.ACCENT_STRIPE -> AppColors.TiviSurfaceCool.copy(alpha = 0.92f)
        AppShapeSet.EpgCellStyle.BEVELED -> AppColors.SurfaceAccent.copy(alpha = 0.86f)
        AppShapeSet.EpgCellStyle.DOUBLE_EDGE -> AppColors.TiviSurfaceBase.copy(alpha = 0.94f)
    }
}

private fun epgCellFocusedColor(
    style: AppShapeSet.EpgCellStyle,
    isCurrent: Boolean
): Color =
    when {
        isAfterglowLabsGuideTheme -> LabsGuidePanelFocused
        isCurrent -> AppColors.FocusFill.copy(alpha = 0.55f)
        style == AppShapeSet.EpgCellStyle.RECTANGULAR -> AppColors.SurfaceAccent.copy(alpha = 0.95f)
        else -> SurfaceHighlight
    }

private fun epgCellBorderColor(
    style: AppShapeSet.EpgCellStyle,
    liveStyle: AppShapeSet.EpgLiveCellStyle,
    isCurrent: Boolean
): Color =
    when {
        isAfterglowLabsGuideTheme -> LabsGuideDivider
        isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.DOUBLE_EDGE -> AppColors.EpgNowLine.copy(alpha = 0.52f)
        isCurrent -> AppColors.EpgNowLine.copy(alpha = 0.46f)
        style == AppShapeSet.EpgCellStyle.ACCENT_STRIPE -> AppColors.TiviAccent.copy(alpha = 0.28f)
        style == AppShapeSet.EpgCellStyle.BEVELED -> Color.White.copy(alpha = 0.18f)
        style == AppShapeSet.EpgCellStyle.DOUBLE_EDGE -> Color.White.copy(alpha = 0.18f)
        else -> Color.White.copy(alpha = 0.12f)
    }

private fun epgCellBorderWidth(
    style: AppShapeSet.EpgCellStyle,
    liveStyle: AppShapeSet.EpgLiveCellStyle,
    isCurrent: Boolean
): Dp =
    when {
        isAfterglowLabsGuideTheme -> 1.dp
        isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.DOUBLE_EDGE -> 1.dp
        style == AppShapeSet.EpgCellStyle.DOUBLE_EDGE -> 1.dp
        else -> 1.dp
    }

private fun epgProgramTitleColor(
    liveStyle: AppShapeSet.EpgLiveCellStyle,
    isCurrent: Boolean,
    isFocused: Boolean
): Color =
    when {
        isAfterglowLabsGuideTheme -> LabsGuideText
        isFocused -> TextPrimary
        isCurrent && liveStyle == AppShapeSet.EpgLiveCellStyle.NOW_FILL -> AppColors.TiviAccentLight
        isCurrent -> AppColors.EpgNowLine
        else -> OnSurface
    }

private fun epgProgramMetaColor(isFocused: Boolean): Color =
    if (isAfterglowLabsGuideTheme) {
        if (isFocused) LabsGuideText else LabsGuideTextMuted
    } else if (isFocused) {
        TextSecondary
    } else {
        OnSurfaceDim
    }

private fun List<Program>.currentProgramAt(now: Long): Program? =
    firstOrNull { now in it.startTime until it.endTime }

internal fun formatWindowDuration(durationMs: Long): String {
    val hours = durationMs / (60 * 60 * 1000L)
    val minutes = (durationMs / (60 * 1000L)) % 60
    return if (minutes == 0L) {
        "${hours}h"
    } else {
        "${hours}h ${minutes}m"
    }
}

private fun snapProgramTimeToDisplayedMinuteBoundary(
    timestamp: Long,
    zone: ZoneId,
    windowStart: Long,
    windowEnd: Long
): Long {
    if (timestamp <= windowStart || timestamp >= windowEnd) return timestamp
    val roundedToMinute = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone)
        .withSecond(0)
        .withNano(0)
        .toInstant()
        .toEpochMilli()
    return roundedToMinute.takeIf { kotlin.math.abs(timestamp - it) < 60_000L } ?: timestamp
}

internal fun epgChannelKey(channel: Channel, index: Int): String {
    val epgId = channel.guideLookupKey().orEmpty()
    return "channel:${channel.id}:${channel.streamId}:${epgId}:${channel.name.trim()}:$index"
}
