package com.afterglowtv.app.ui.screens.epg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.interaction.TvButton
import com.afterglowtv.app.ui.time.LocalAppTimeFormat
import com.afterglowtv.app.ui.time.createTimeFormat
import com.afterglowtv.app.ui.theme.FocusBorder
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.EpgMatchType
import com.afterglowtv.domain.model.EpgOverrideCandidate
import com.afterglowtv.domain.model.EpgSourceType
import com.afterglowtv.domain.model.Program
import java.util.Date

@Composable
private fun GuideModalDialog(
    onDismiss: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = contentAlignment
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .clickable(
                        onClick = onDismiss,
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    )
            )
            content()
        }
    }
}

@Composable
internal fun GuideSearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val searchFocusRequester = remember { FocusRequester() }
    val applySearchAndClose = remember(onQueryChange, onDismiss) {
        { submittedQuery: String ->
            onQueryChange(submittedQuery)
            onDismiss()
        }
    }
    val clearAndClose = remember(onClear, onDismiss) {
        {
            onClear()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    GuideModalDialog(
        onDismiss = onDismiss,
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .padding(top = 32.dp)
                .focusGroup(),
            colors = SurfaceDefaults.colors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.epg_search_label),
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuideShortcutChip(
                            label = stringResource(R.string.epg_search_apply),
                            onClick = { applySearchAndClose(query.trim()) }
                        )
                        GuideShortcutChip(
                            label = stringResource(R.string.epg_clear_search_close),
                            onClick = clearAndClose
                        )
                    }
                }
                GuideProgramSearchRow(
                    query = query,
                    onQueryChange = onQueryChange,
                    onClear = onClear,
                    onSearch = applySearchAndClose,
                    focusRequester = searchFocusRequester,
                    autoRequestFocus = true,
                    contentPadding = PaddingValues(0.dp),
                    showLabel = false
                )
            }
        }
    }
}

@Composable
internal fun GuideOptionsOverlay(
    uiState: EpgUiState,
    onDismiss: () -> Unit,
    onShowAppNavigation: () -> Unit,
    onJumpToPreviousDay: () -> Unit,
    onPageBackward: () -> Unit,
    onJumpBackwardHalfHour: () -> Unit,
    onJumpBackward: () -> Unit,
    onJumpToNow: () -> Unit,
    onJumpForwardHalfHour: () -> Unit,
    onJumpForward: () -> Unit,
    onPageForward: () -> Unit,
    onJumpToPrimeTime: () -> Unit,
    onJumpToTomorrow: () -> Unit,
    onJumpToNextDay: () -> Unit,
    onDaySelected: (Long) -> Unit,
    onModeSelected: (GuideChannelMode) -> Unit,
    onDensitySelected: (GuideDensity) -> Unit,
    onToggleScheduledOnly: () -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onRefresh: () -> Unit,
    onManageEpgMatch: (() -> Unit)? = null
) {
    val optionsFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        optionsFocusRequester.requestFocus()
    }
    GuideModalDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .fillMaxHeight(0.78f)
                .focusGroup(),
            colors = SurfaceDefaults.colors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.epg_options_short),
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuideShortcutChip(
                            label = stringResource(R.string.epg_show_app_navigation),
                            onClick = onShowAppNavigation
                        )
                        GuideShortcutChip(
                            label = stringResource(R.string.settings_cancel),
                            onClick = onDismiss
                        )
                    }
                }
                GuideTimeControlsRow(
                    onJumpToPreviousDay = onJumpToPreviousDay,
                    onPageBackward = onPageBackward,
                    onJumpBackwardHalfHour = onJumpBackwardHalfHour,
                    onJumpBackward = onJumpBackward,
                    onJumpToNow = onJumpToNow,
                    onJumpForwardHalfHour = onJumpForwardHalfHour,
                    onJumpForward = onJumpForward,
                    onPageForward = onPageForward,
                    onJumpToPrimeTime = onJumpToPrimeTime,
                    onJumpToTomorrow = onJumpToTomorrow,
                    onJumpToNextDay = onJumpToNextDay,
                    firstChipFocusRequester = optionsFocusRequester
                )
                GuideDayRow(
                    selectedDayStart = startOfDay(uiState.guideWindowStart + EpgViewModel.LOOKBACK_MS),
                    onDaySelected = onDaySelected
                )
                GuideModeRow(
                    selectedMode = uiState.selectedChannelMode,
                    onModeSelected = onModeSelected
                )
                GuideDensityRow(
                    selectedDensity = uiState.selectedDensity,
                    onDensitySelected = onDensitySelected
                )
                GuideViewOptionsRow(
                    showScheduledOnly = uiState.showScheduledOnly,
                    onToggleScheduledOnly = onToggleScheduledOnly
                )
                GuideFavoritesRow(
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    onToggleFavoritesOnly = onToggleFavoritesOnly
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (onManageEpgMatch != null) {
                        GuideShortcutChip(
                            label = stringResource(R.string.epg_override_manage),
                            onClick = onManageEpgMatch
                        )
                    }
                    GuideShortcutChip(
                        label = stringResource(R.string.epg_refresh_guide),
                        onClick = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
internal fun CompactGuideProgramDialog(
    channel: Channel,
    program: Program,
    providerLabel: String,
    now: Long,
    onDismiss: () -> Unit,
    onWatchLive: () -> Unit,
    onWatchArchive: (() -> Unit)?,
    reminderButtonLabel: String?,
    onToggleReminder: (() -> Unit)?,
    onScheduleRecording: (() -> Unit)?,
    onScheduleDailyRecording: (() -> Unit)?,
    onScheduleWeeklyRecording: (() -> Unit)?
) {
    var showDetails by rememberSaveable(program.startTime, program.endTime, program.title, program.isPlaceholder) {
        mutableStateOf(program.isPlaceholder)
    }
    val appTimeFormat = LocalAppTimeFormat.current
    val format = remember(appTimeFormat) { appTimeFormat.createTimeFormat() }
    val firstButtonFocusRequester = remember { FocusRequester() }
    val actionButtonShape = RoundedCornerShape(12.dp)
    val noScheduleLabel = stringResource(R.string.epg_no_schedule_short)
    val displayTitle = program.title.ifBlank { channel.name }
    LaunchedEffect(Unit) { firstButtonFocusRequester.requestFocus() }
    GuideModalDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 420.dp, max = 640.dp),
            colors = SurfaceDefaults.colors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (channel.number > 0) { append(channel.number); append(". ") }
                        append(channel.name)
                        if (program.isPlaceholder) {
                            append("  |  ")
                            append(program.category?.takeIf { it.isNotBlank() } ?: noScheduleLabel)
                        } else {
                            append("  |  ")
                            append(format.format(Date(program.startTime)))
                            append(" - ")
                            append(format.format(Date(program.endTime)))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (providerLabel.isNotBlank()) {
                    Text(
                        text = providerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                }
                if (!program.isPlaceholder && now in program.startTime until program.endTime) {
                    LinearProgressIndicator(
                        progress = { ((now - program.startTime).toFloat() / (program.endTime - program.startTime).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Primary,
                        trackColor = SurfaceHighlight
                    )
                }
                if (showDetails) {
                    Text(
                        text = program.description.ifBlank { stringResource(R.string.epg_no_info) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideDialogActionButton(
                        onClick = onWatchLive,
                        modifier = Modifier.fillMaxWidth().focusRequester(firstButtonFocusRequester),
                        shape = actionButtonShape
                    ) {
                        Text(stringResource(R.string.epg_watch_live))
                    }
                    if (onWatchArchive != null) {
                        GuideDialogActionButton(
                            onClick = onWatchArchive,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(
                                containerColor = Primary,
                                contentColor = Color.White
                            ),
                            shape = actionButtonShape
                        ) {
                            Text(stringResource(R.string.epg_watch_archive))
                        }
                    }
                    if (reminderButtonLabel != null && onToggleReminder != null) {
                        GuideDialogActionButton(
                            onClick = onToggleReminder,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceHighlight,
                                contentColor = OnSurface
                            ),
                            shape = actionButtonShape
                        ) {
                            Text(reminderButtonLabel)
                        }
                    }
                    if (onScheduleRecording != null) {
                        GuideDialogActionButton(
                            onClick = { onScheduleRecording(); onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(
                                containerColor = com.afterglowtv.app.ui.theme.AccentRed,
                                contentColor = Color.White
                            ),
                            shape = actionButtonShape
                        ) {
                            Text(stringResource(R.string.epg_schedule_recording))
                        }
                    }
                    if (onScheduleDailyRecording != null) {
                        GuideDialogActionButton(
                            onClick = { onScheduleDailyRecording(); onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceHighlight,
                                contentColor = OnSurface
                            ),
                            shape = actionButtonShape
                        ) {
                            Text(stringResource(R.string.epg_schedule_daily_recording))
                        }
                    }
                    if (onScheduleWeeklyRecording != null) {
                        GuideDialogActionButton(
                            onClick = { onScheduleWeeklyRecording(); onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceHighlight,
                                contentColor = OnSurface
                            ),
                            shape = actionButtonShape
                        ) {
                            Text(stringResource(R.string.epg_schedule_weekly_recording))
                        }
                    }
                    GuideDialogActionButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceHighlight,
                            contentColor = OnSurface
                        ),
                        shape = actionButtonShape
                    ) {
                        Text(
                            if (showDetails) stringResource(R.string.epg_program_details_hide)
                            else stringResource(R.string.epg_program_details_show)
                        )
                    }
                    GuideDialogActionButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurface
                        ),
                        shape = actionButtonShape
                    ) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideDialogActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.colors(
        containerColor = SurfaceHighlight,
        contentColor = OnSurface
    ),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    TvButton(
        onClick = onClick,
        modifier = modifier,
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = colors,
        shape = ButtonDefaults.shape(shape = shape),
        border = ButtonDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, FocusBorder),
                shape = shape
            )
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
internal fun EpgOverrideDialog(
    state: EpgOverrideUiState,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCandidateSelected: (EpgOverrideCandidate) -> Unit,
    onClearOverride: () -> Unit
) {
    val channel = state.channel ?: return
    val unknownValue = stringResource(R.string.epg_program_unknown_value)
    val currentMapping = state.currentMapping
    val currentCandidate = remember(currentMapping, state.candidates) {
        currentMapping?.let { mapping ->
            state.candidates.firstOrNull {
                it.epgSourceId == mapping.epgSourceId &&
                    it.xmltvChannelId == mapping.xmltvChannelId
            }
        }
    }
    val currentDescriptor = currentCandidate?.let {
        "${it.displayName}  •  ${it.epgSourceName}  •  ${it.xmltvChannelId}"
    } ?: (currentMapping?.xmltvChannelId ?: unknownValue)
    val currentSummary = when {
        currentMapping == null || currentMapping.sourceType == EpgSourceType.NONE ->
            stringResource(R.string.epg_override_current_none)
        state.currentMapping.isManualOverride || state.currentMapping.matchType == EpgMatchType.MANUAL ->
            stringResource(R.string.epg_override_current_manual, currentDescriptor)
        state.currentMapping.sourceType == EpgSourceType.PROVIDER ->
            stringResource(R.string.epg_override_current_provider, currentDescriptor)
        else ->
            stringResource(R.string.epg_override_current_external, currentDescriptor)
    }

    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }

    GuideModalDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 560.dp, max = 760.dp),
            colors = SurfaceDefaults.colors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.epg_override_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface
                )
                Text(
                    text = if (channel.number > 0) "${channel.number}. ${channel.name}" else channel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.epg_override_current_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                    Text(
                        text = currentSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
                if (!state.error.isNullOrBlank()) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.isLoading || state.isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Primary,
                        trackColor = SurfaceHighlight
                    )
                }
                GuideSearchField(
                    value = state.searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.epg_override_search_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = searchFocusRequester,
                    onSearch = { onQueryChange(it) }
                )
                if (state.candidates.isEmpty()) {
                    Text(
                        text = if (state.searchQuery.isBlank()) {
                            stringResource(R.string.epg_override_no_candidates)
                        } else {
                            stringResource(R.string.epg_override_no_search_results)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceDim
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .focusGroup(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.candidates,
                            key = { candidate -> "${candidate.epgSourceId}:${candidate.xmltvChannelId}" }
                        ) { candidate ->
                            val isCurrent = currentMapping?.let { mapping ->
                                mapping.epgSourceId == candidate.epgSourceId &&
                                    mapping.xmltvChannelId == candidate.xmltvChannelId
                            } == true
                            TvClickableSurface(
                                onClick = {
                                    if (!state.isSaving) {
                                        onCandidateSelected(candidate)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isCurrent) SurfaceHighlight else SurfaceElevated,
                                    focusedContainerColor = SurfaceHighlight,
                                    contentColor = OnSurface,
                                    focusedContentColor = OnSurface
                                ),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        border = BorderStroke(2.dp, FocusBorder),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = candidate.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = OnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrent) {
                                            Text(
                                                text = stringResource(R.string.epg_override_selected_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${candidate.epgSourceName}  •  ${candidate.xmltvChannelId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceDim,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.currentMapping?.isManualOverride == true) {
                        TvButton(
                            onClick = onClearOverride,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            scale = ButtonDefaults.scale(focusedScale = 1f),
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceHighlight,
                                contentColor = OnSurface
                            )
                        ) {
                            Text(stringResource(R.string.epg_override_clear))
                        }
                    }
                    TvButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        scale = ButtonDefaults.scale(focusedScale = 1f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurface
                        )
                    ) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            }
        }
    }
}

@Composable
internal fun GuideCategoryPickerDialog(
    categories: List<Category>,
    selectedCategoryId: Long,
    parentalControlLevel: Int,
    onDismiss: () -> Unit,
    onCategorySelected: (Category) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var shouldFocusFirstCategory by rememberSaveable { mutableStateOf(true) }
    val searchFocusRequester = remember { FocusRequester() }
    val firstCategoryFocusRequester = remember { FocusRequester() }
    val filteredCategories = remember(categories, query) {
        val trimmed = query.trim()
        val baseCategories = if (trimmed.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
        val selectedCategory = baseCategories.firstOrNull { it.id == selectedCategoryId }
        buildList {
            if (selectedCategory != null) add(selectedCategory)
            addAll(baseCategories.filterNot { it.id == selectedCategoryId })
        }
    }
    GuideModalDialog(onDismiss = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .fillMaxHeight(0.78f)
                .focusGroup(),
            colors = SurfaceDefaults.colors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.epg_filter_label),
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface
                        )
                        Text(
                            text = "${filteredCategories.size} matches",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                    }
                    GuideShortcutChip(
                        label = stringResource(R.string.settings_cancel),
                        onClick = onDismiss
                    )
                }

                GuideProgramSearchRow(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    focusRequester = searchFocusRequester,
                    contentPadding = PaddingValues(0.dp),
                    showLabel = false,
                    onSearchFieldActivated = {
                        shouldFocusFirstCategory = false
                    }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = filteredCategories,
                        key = { index, category -> epgCategoryKey(category, index) }
                    ) { _, category ->
                        val isSelected = category.id == selectedCategoryId
                        val isLocked = isGuideCategoryLocked(category, parentalControlLevel)
                        TvClickableSurface(
                            onClick = { onCategorySelected(category) },
                            modifier = if (category.id == filteredCategories.firstOrNull()?.id) {
                                Modifier.focusRequester(firstCategoryFocusRequester)
                            } else {
                                Modifier
                            },
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) Primary.copy(alpha = 0.18f) else SurfaceHighlight,
                                focusedContainerColor = if (isSelected) {
                                    Primary.copy(alpha = 0.22f)
                                } else {
                                    SurfaceHighlight
                                },
                                contentColor = if (isSelected) Primary else OnSurface,
                                focusedContentColor = if (isSelected) Primary else OnSurface
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(2.dp, FocusBorder),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) Primary else OnSurface
                                    )
                                    if (isLocked) {
                                        Text(
                                            text = stringResource(R.string.settings_parental_control),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceDim
                                        )
                                    } else if (category.count > 0) {
                                        Text(
                                            text = "${category.count} channels",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceDim
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Text(
                                        text = stringResource(R.string.epg_jump_now),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(filteredCategories, shouldFocusFirstCategory) {
        if (shouldFocusFirstCategory && filteredCategories.isNotEmpty()) {
            firstCategoryFocusRequester.requestFocus()
            shouldFocusFirstCategory = false
        }
    }
}

internal fun epgCategoryKey(category: Category, index: Int): String {
    return "category:${category.id}:${category.name.trim()}:$index"
}
