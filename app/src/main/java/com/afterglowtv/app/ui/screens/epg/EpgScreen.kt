package com.afterglowtv.app.ui.screens.epg

import android.view.inputmethod.InputMethodManager
import com.afterglowtv.app.ui.model.isArchivePlayable
import com.afterglowtv.app.ui.model.guideLookupKey
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.device.rememberIsTelevisionDevice
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.ui.notifications.rememberNotificationPermissionGate
import com.afterglowtv.app.ui.components.SelectionChip
import com.afterglowtv.app.ui.components.SelectionChipRow
import kotlinx.coroutines.launch
import com.afterglowtv.app.ui.components.dialogs.PinDialog
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.theme.FocusBorder
import com.afterglowtv.app.ui.theme.OnBackground
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.app.ui.theme.TextPrimary
import com.afterglowtv.app.ui.theme.TextSecondary
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.EpgMatchType
import com.afterglowtv.domain.model.EpgOverrideCandidate
import com.afterglowtv.domain.model.EpgSourceType
import com.afterglowtv.domain.model.Program
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.ChannelRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.interaction.TvButton
import com.afterglowtv.app.ui.interaction.TvIconButton

private sealed interface LockedGuideAction {
    data class SelectCategory(val category: Category) : LockedGuideAction
    data class OpenProgram(val channel: Channel, val program: Program) : LockedGuideAction
    data class PlayChannel(val channel: Channel, val returnRoute: String) : LockedGuideAction
    data class PlayArchive(val channel: Channel, val program: Program, val returnRoute: String) : LockedGuideAction
}

internal enum class GuideEmptyAction {
    Retry,
    ClearSearch,
    ResetFilters
}

internal fun shouldRenderGuideChrome(state: EpgUiState): Boolean {
    if (state.error == EpgViewModel.NO_ACTIVE_PROVIDER) return false
    if (state.error != null && state.channels.isEmpty() && state.totalChannelCount == 0) return false

    return state.channels.isNotEmpty() ||
        state.totalChannelCount > 0 ||
        state.categories.isNotEmpty() ||
        hasRestrictiveGuideView(state)
}

internal fun resolveGuideEmptyAction(state: EpgUiState): GuideEmptyAction {
    if (state.programSearchQuery.isNotBlank()) return GuideEmptyAction.ClearSearch
    if (state.error != null && state.channels.isEmpty() && state.totalChannelCount == 0) {
        return GuideEmptyAction.Retry
    }
    return if (
        hasRestrictiveGuideView(state) ||
        state.totalChannelCount > 0 ||
        state.categories.isNotEmpty()
    ) {
        GuideEmptyAction.ResetFilters
    } else {
        GuideEmptyAction.Retry
    }
}

private fun hasRestrictiveGuideView(state: EpgUiState): Boolean =
    state.programSearchQuery.isNotBlank() ||
        state.showScheduledOnly ||
        state.selectedChannelMode != GuideChannelMode.ALL ||
        state.showFavoritesOnly ||
        state.selectedCategoryId != ChannelRepository.ALL_CHANNELS_ID

@Composable
fun FullEpgScreen(
    currentRoute: String,
    initialCategoryId: Long? = null,
    initialAnchorTime: Long? = null,
    initialFavoritesOnly: Boolean = false,
    onPlayChannel: (Channel, Long, Boolean, Long?, String) -> Unit,
    onPlayArchive: (Channel, Program, Long, Boolean, Long?, String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: EpgViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overrideUiState by viewModel.overrideUiState.collectAsStateWithLifecycle()
    val programReminderUiState by viewModel.programReminderUiState.collectAsStateWithLifecycle()
    var selectedProgram by remember { mutableStateOf<Pair<Channel, Program>?>(null) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var focusedProgram by remember { mutableStateOf<Program?>(null) }
    // Snapshot captured at composition time — used once, by the grid's
    // first-resolve effect below, to restore the user's last spot in the
    // guide after they navigate away and come back.
    val savedPosition = remember { viewModel.lastPosition() }
    // Default to hidden so the guide fills the entire screen. The existing
    // onShowAppNavigation handler can flip this back to true on demand, and
    // onGuideInteract surfaces it briefly during interactions.
    var topNavVisible by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showGuideOptions by rememberSaveable { mutableStateOf(false) }
    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    var showPinDialog by rememberSaveable { mutableStateOf(false) }
    var pinError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLockedAction by remember { mutableStateOf<LockedGuideAction?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationPermissionGate = rememberNotificationPermissionGate(
        onNotificationsBlocked = { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        },
        reminderBlockedMessage = stringResource(R.string.notification_permission_reminder_required),
        recordingBlockedMessage = stringResource(R.string.notification_permission_recording_alert_required)
    )
    val guideToolbarFocusRequester = remember { FocusRequester() }
    val returnRoute = remember(uiState.selectedCategoryId, uiState.guideAnchorTime, uiState.showFavoritesOnly) {
        Routes.epg(
            categoryId = uiState.selectedCategoryId.takeIf { it != ChannelRepository.ALL_CHANNELS_ID },
            anchorTime = uiState.guideAnchorTime,
            favoritesOnly = uiState.showFavoritesOnly
        )
    }
    val categoriesById = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }
    val playerCategoryId = remember(uiState.selectedCategoryId, uiState.showFavoritesOnly) {
        if (uiState.showFavoritesOnly && uiState.selectedCategoryId == ChannelRepository.ALL_CHANNELS_ID) {
            VirtualCategoryIds.FAVORITES
        } else {
            uiState.selectedCategoryId
        }
    }
    val playerIsVirtualCategory = playerCategoryId == VirtualCategoryIds.FAVORITES ||
        playerCategoryId == VirtualCategoryIds.RECENT ||
        (playerCategoryId < 0L && playerCategoryId != ChannelRepository.ALL_CHANNELS_ID)

    fun executeLockedGuideAction(action: LockedGuideAction) {
        when (action) {
            is LockedGuideAction.SelectCategory -> viewModel.selectCategory(action.category.id)
            is LockedGuideAction.OpenProgram -> selectedProgram = action.channel to action.program
            is LockedGuideAction.PlayChannel ->
                onPlayChannel(
                    action.channel,
                    playerCategoryId,
                    playerIsVirtualCategory,
                    uiState.combinedProfileId,
                    action.returnRoute
                )
            is LockedGuideAction.PlayArchive ->
                onPlayArchive(
                    action.channel,
                    action.program,
                    playerCategoryId,
                    playerIsVirtualCategory,
                    uiState.combinedProfileId,
                    action.returnRoute
                )
        }
    }

    fun requestLockedGuideAction(action: LockedGuideAction) {
        pendingLockedAction = action
        pinError = null
        showPinDialog = true
    }

    LaunchedEffect(initialCategoryId, initialAnchorTime, initialFavoritesOnly) {
        viewModel.applyNavigationContext(
            categoryId = initialCategoryId,
            anchorTime = initialAnchorTime,
            favoritesOnly = initialFavoritesOnly
        )
    }

    uiState.recordingMessage?.let { message ->
        LaunchedEffect(message) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearRecordingMessage()
        }
    }

    uiState.pendingRecordingConflict?.let { conflict ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissRecordingConflict() },
            title = {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.epg_recording_conflict_title),
                    color = com.afterglowtv.app.ui.theme.OnSurface
                )
            },
            text = {
                val conflictNames = conflict.conflictingItems.joinToString(", ") {
                    it.programTitle ?: it.channelName
                }
                androidx.compose.material3.Text(
                    text = stringResource(R.string.epg_recording_conflict_body, conflict.programTitle, conflictNames),
                    color = com.afterglowtv.app.ui.theme.TextSecondary
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        notificationPermissionGate.runRecordingAction {
                            viewModel.forceScheduleRecording()
                        }
                    }
                ) {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.epg_recording_conflict_replace),
                        color = com.afterglowtv.app.ui.theme.Primary
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissRecordingConflict() }) {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.epg_recording_conflict_cancel),
                        color = com.afterglowtv.app.ui.theme.OnSurface
                    )
                }
            },
            containerColor = com.afterglowtv.app.ui.theme.SurfaceElevated,
            titleContentColor = com.afterglowtv.app.ui.theme.OnSurface,
            textContentColor = com.afterglowtv.app.ui.theme.TextSecondary
        )
    }

    LaunchedEffect(uiState.channels, uiState.programsByChannel) {
        if (uiState.channels.isEmpty()) {
            focusedChannel = null
            focusedProgram = null
            return@LaunchedEffect
        }
        // Resolve focused channel:
        //   1. If we already have one in local state (config change recomposition), keep it.
        //   2. Else if the user has a saved position from a prior visit, restore it.
        //   3. Else fall back to the first channel in the list.
        val resolvedChannel = focusedChannel?.let { current ->
            uiState.channels.firstOrNull { it.id == current.id }
        } ?: savedPosition?.let { saved ->
            uiState.channels.firstOrNull { it.id == saved.channelId }
        } ?: uiState.channels.firstOrNull()
        focusedChannel = resolvedChannel
        val resolvedPrograms = resolvedChannel?.let { channel ->
            channel.guideLookupKey()?.let { lookupKey ->
                uiState.programsByChannel[lookupKey].orEmpty()
            }.orEmpty()
        }.orEmpty()
        // Resolve focused program with the same precedence order — local
        // state, then saved snapshot, then the current-time program.
        focusedProgram = focusedProgram?.let { focused ->
            resolvedPrograms.firstOrNull {
                it.startTime == focused.startTime &&
                    it.endTime == focused.endTime &&
                    it.title == focused.title
            }
        } ?: savedPosition?.takeIf { it.programStartMs > 0L }?.let { saved ->
            // Sentinel 0L means "channel only, no specific program saved";
            // skip the exact-match lookup and fall through to the
            // current-time program below.
            resolvedPrograms.firstOrNull { it.startTime == saved.programStartMs }
        } ?: resolvedPrograms.firstOrNull {
            System.currentTimeMillis() in it.startTime until it.endTime
        }
    }

    AppScreenScaffold(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = stringResource(R.string.nav_epg),
        subtitle = stringResource(R.string.guide_shell_subtitle),
        navigationChrome = AppNavigationChrome.TopBar,
        topBarVisible = topNavVisible,
        compactHeader = true,
        showScreenHeader = false,
        fullBleed = true,   // EPG wants every pixel for the program grid
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val renderGuideChrome = shouldRenderGuideChrome(uiState)
            when {
                uiState.isInitialLoading && uiState.channels.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.epg_loading), color = OnBackground)
                    }
                }

                uiState.error != null && !renderGuideChrome -> {
                    GuideMessageState(
                        modifier = Modifier.weight(1f),
                        title = when (uiState.error) {
                            EpgViewModel.NO_ACTIVE_PROVIDER -> stringResource(R.string.epg_no_provider)
                            else -> stringResource(R.string.epg_error)
                        },
                        subtitle = when (uiState.error) {
                            EpgViewModel.NO_ACTIVE_PROVIDER -> null
                            else -> stringResource(R.string.epg_retry_hint)
                        },
                        actionLabel = if (uiState.error == EpgViewModel.NO_ACTIVE_PROVIDER) null else stringResource(R.string.epg_retry),
                        onAction = if (uiState.error == EpgViewModel.NO_ACTIVE_PROVIDER) null else viewModel::refresh
                    )
                }

                !renderGuideChrome -> {
                    GuideMessageState(
                        modifier = Modifier.weight(1f),
                        title = when {
                            uiState.programSearchQuery.isNotBlank() ->
                                stringResource(R.string.epg_no_search_results)
                            uiState.totalChannelCount == 0 && uiState.selectedCategoryId != ChannelRepository.ALL_CHANNELS_ID ->
                                stringResource(R.string.epg_no_channels_in_category)
                            uiState.totalChannelCount == 0 ->
                                stringResource(R.string.epg_no_data)
                            else ->
                                stringResource(R.string.epg_no_scheduled_channels)
                        },
                        subtitle = when {
                            uiState.programSearchQuery.isNotBlank() ->
                                stringResource(R.string.epg_search_empty_hint)
                            uiState.totalChannelCount == 0 ->
                                stringResource(R.string.epg_filter_hint)
                            uiState.showScheduledOnly ->
                                stringResource(R.string.epg_scheduled_only_hint)
                            else ->
                                stringResource(R.string.epg_stale_warning)
                        },
                        actionLabel = stringResource(R.string.epg_retry),
                        onAction = viewModel::refresh
                    )
                }

                else -> {
                    GuideNowProvider {
                        GuideHeroSection(
                            uiState = uiState,
                            focusedChannel = focusedChannel,
                            focusedProgram = focusedProgram,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                    GuideToolbarRow(
                        selectedCategoryName = uiState.categories
                            .firstOrNull { it.id == uiState.selectedCategoryId }
                            ?.name
                            ?: stringResource(R.string.epg_filter_short),
                        firstButtonFocusRequester = guideToolbarFocusRequester,
                        onOpenCategoryPicker = {
                            showCategoryPicker = true
                        },
                        onJumpToNow = {
                            viewModel.jumpToNow()
                        },
                        onOpenSearch = {
                            showSearchOverlay = true
                        },
                        onOpenOptions = {
                            showGuideOptions = true
                        },
                        onGuideInteract = { topNavVisible = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
                    if (uiState.isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .height(3.dp),
                            color = Primary,
                            trackColor = SurfaceHighlight
                        )
                    }
                    if (uiState.channels.isEmpty()) {
                        val emptyAction = resolveGuideEmptyAction(uiState)
                        GuideMessageState(
                            modifier = Modifier.weight(1f),
                            title = when (emptyAction) {
                                GuideEmptyAction.ClearSearch -> stringResource(R.string.epg_no_search_results)
                                GuideEmptyAction.ResetFilters -> stringResource(R.string.epg_no_visible_channels)
                                GuideEmptyAction.Retry -> when {
                                    uiState.totalChannelCount == 0 && uiState.selectedCategoryId != ChannelRepository.ALL_CHANNELS_ID ->
                                        stringResource(R.string.epg_no_channels_in_category)
                                    uiState.totalChannelCount == 0 ->
                                        stringResource(R.string.epg_no_data)
                                    else ->
                                        stringResource(R.string.epg_no_scheduled_channels)
                                }
                            },
                            subtitle = when (emptyAction) {
                                GuideEmptyAction.ClearSearch -> stringResource(R.string.epg_search_empty_hint)
                                GuideEmptyAction.ResetFilters -> stringResource(R.string.epg_filtered_empty_hint)
                                GuideEmptyAction.Retry -> stringResource(R.string.epg_filter_hint)
                            },
                            actionLabel = when (emptyAction) {
                                GuideEmptyAction.ClearSearch -> stringResource(R.string.epg_clear_search)
                                GuideEmptyAction.ResetFilters -> stringResource(R.string.epg_show_all_channels)
                                GuideEmptyAction.Retry -> stringResource(R.string.epg_retry)
                            },
                            onAction = when (emptyAction) {
                                GuideEmptyAction.ClearSearch -> viewModel::clearProgramSearch
                                GuideEmptyAction.ResetFilters -> viewModel::resetGuideFilters
                                GuideEmptyAction.Retry -> viewModel::refresh
                            }
                        )
                    } else {
                        GuideNowProvider {
                            EpgGrid(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                channels = uiState.channels,
                                favoriteChannelIds = uiState.favoriteChannelIds,
                                programsByChannel = uiState.programsByChannel,
                                guideWindowStart = uiState.guideWindowStart,
                                guideWindowEnd = uiState.guideWindowEnd,
                                density = uiState.selectedDensity,
                                onChannelClick = { channel ->
                                    if (isGuideChannelLocked(channel, categoriesById, uiState.parentalControlLevel)) {
                                        requestLockedGuideAction(LockedGuideAction.PlayChannel(channel, returnRoute))
                                    } else {
                                        onPlayChannel(
                                            channel,
                                            playerCategoryId,
                                            playerIsVirtualCategory,
                                            uiState.combinedProfileId,
                                            returnRoute
                                        )
                                    }
                                },
                                onProgramClick = { channel, program ->
                                    topNavVisible = false
                                    if (isGuideChannelLocked(channel, categoriesById, uiState.parentalControlLevel)) {
                                        requestLockedGuideAction(LockedGuideAction.OpenProgram(channel, program))
                                    } else {
                                        selectedProgram = channel to program
                                    }
                                },
                                onChannelFocused = { channel, currentProgram, isFirstRow ->
                                    topNavVisible = isFirstRow
                                    focusedChannel = channel
                                    focusedProgram = currentProgram
                                    // Always save the channel ID on focus, even when
                                    // there's no current program. Channels with no
                                    // EPG data previously had their focus dropped
                                    // from the position memo entirely, which is why
                                    // re-entering the EPG landed on the first
                                    // channel instead of the user's last spot.
                                    // programStartMs=0L is a sentinel for
                                    // "channel-only position"; the restore path
                                    // falls back to the current-time program.
                                    viewModel.rememberPosition(
                                        channelId = channel.id,
                                        programStartMs = currentProgram?.startTime ?: 0L,
                                        categoryId = uiState.selectedCategoryId,
                                    )
                                },
                                onProgramFocused = { channel, program, isFirstRow ->
                                    topNavVisible = isFirstRow
                                    focusedChannel = channel
                                    focusedProgram = program
                                    viewModel.rememberPosition(
                                        channelId = channel.id,
                                        programStartMs = program.startTime,
                                        categoryId = uiState.selectedCategoryId,
                                    )
                                },
                                onRequestGuideToolbarFocus = {
                                    topNavVisible = true
                                    guideToolbarFocusRequester.requestFocus()
                                },
                                onRequestMoreChannels = viewModel::requestMoreChannels
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        GuideCategoryPickerDialog(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            parentalControlLevel = uiState.parentalControlLevel,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = { category ->
                showCategoryPicker = false
                if (isGuideCategoryLocked(category, uiState.parentalControlLevel)) {
                    requestLockedGuideAction(LockedGuideAction.SelectCategory(category))
                } else {
                    viewModel.selectCategory(category.id)
                }
            }
        )
    }

    if (showSearchOverlay) {
        GuideSearchOverlay(
            query = uiState.programSearchQuery,
            onQueryChange = viewModel::updateProgramSearchQuery,
            onClear = viewModel::clearProgramSearch,
            onDismiss = {
                showSearchOverlay = false
            }
        )
    }

    if (showGuideOptions) {
        GuideOptionsOverlay(
            uiState = uiState,
            onDismiss = { showGuideOptions = false },
            onShowAppNavigation = {
                topNavVisible = true
                showGuideOptions = false
            },
            onJumpToPreviousDay = viewModel::jumpToPreviousDay,
            onPageBackward = viewModel::pageBackward,
            onJumpBackwardHalfHour = viewModel::jumpBackwardHalfHour,
            onJumpBackward = viewModel::jumpBackward,
            onJumpToNow = viewModel::jumpToNow,
            onJumpForwardHalfHour = viewModel::jumpForwardHalfHour,
            onJumpForward = viewModel::jumpForward,
            onPageForward = viewModel::pageForward,
            onJumpToPrimeTime = viewModel::jumpToPrimeTime,
            onJumpToTomorrow = viewModel::jumpToTomorrow,
            onJumpToNextDay = viewModel::jumpToNextDay,
            onDaySelected = viewModel::jumpToDay,
            onModeSelected = viewModel::selectChannelMode,
            onDensitySelected = viewModel::selectDensity,
            onToggleScheduledOnly = viewModel::toggleScheduledOnly,
            onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
            onRefresh = viewModel::refresh,
            onManageEpgMatch = focusedChannel?.takeIf { it.providerId > 0L }?.let { ch ->
                {
                    showGuideOptions = false
                    viewModel.openEpgOverride(ch)
                }
            }
        )
    }

    if (showPinDialog) {
        PinDialog(
            onDismissRequest = {
                showPinDialog = false
                pinError = null
                pendingLockedAction = null
            },
            onPinEntered = { pin ->
                scope.launch {
                    if (viewModel.verifyPin(pin)) {
                        val action = pendingLockedAction
                        val lockedCategoryId = when (action) {
                            is LockedGuideAction.SelectCategory -> action.category.id
                            is LockedGuideAction.OpenProgram -> action.channel.categoryId
                            is LockedGuideAction.PlayChannel -> action.channel.categoryId
                            is LockedGuideAction.PlayArchive -> action.channel.categoryId
                            null -> null
                        }
                        lockedCategoryId?.let(viewModel::unlockCategory)
                        showPinDialog = false
                        pinError = null
                        pendingLockedAction = null
                        action?.let(::executeLockedGuideAction)
                    } else {
                        pinError = context.getString(R.string.home_incorrect_pin)
                    }
                }
            },
            error = pinError
        )
    }

    val dialogState = selectedProgram
    if (dialogState != null) {
        GuideNowProvider {
            val (channel, program) = dialogState
            val isPlaceholderProgram = program.isPlaceholder
            val reminderProviderId = program.providerId.takeIf { it > 0L } ?: channel.providerId
            LaunchedEffect(channel.id, reminderProviderId, program.channelId, program.title, program.startTime, isPlaceholderProgram) {
                if (!isPlaceholderProgram) {
                    viewModel.loadProgramReminderState(channel, program)
                }
            }
            val reminderStateMatches = programReminderUiState.matches(
                providerId = reminderProviderId,
                channelId = program.channelId,
                programTitle = program.title,
                programStartTime = program.startTime
            )
            val reminderButtonLabel = if (
                !isPlaceholderProgram &&
                reminderProviderId > 0L &&
                program.channelId.isNotBlank() &&
                program.startTime > currentGuideNow() + 60_000L
            ) {
                when {
                    reminderStateMatches && programReminderUiState.isLoading ->
                        stringResource(R.string.epg_program_reminder_loading)
                    reminderStateMatches && programReminderUiState.isScheduled ->
                        stringResource(R.string.epg_program_reminder_cancel)
                    else -> stringResource(R.string.epg_program_reminder_set)
                }
            } else {
                null
            }
            val canWatchArchive = !isPlaceholderProgram && channel.isArchivePlayable(program, currentGuideNow())
            val canScheduleRecording = channel.streamUrl.isNotBlank() && program.endTime > currentGuideNow()
            CompactGuideProgramDialog(
                channel = channel,
                program = program,
                providerLabel = uiState.providerSourceLabel,
                now = currentGuideNow(),
                onDismiss = { selectedProgram = null },
                onWatchLive = {
                    selectedProgram = null
                    if (isGuideChannelLocked(channel, categoriesById, uiState.parentalControlLevel)) {
                        requestLockedGuideAction(LockedGuideAction.PlayChannel(channel, returnRoute))
                    } else {
                        onPlayChannel(
                            channel,
                            playerCategoryId,
                            playerIsVirtualCategory,
                            uiState.combinedProfileId,
                            returnRoute
                        )
                    }
                },
                onWatchArchive = if (canWatchArchive) {
                    {
                        selectedProgram = null
                        if (isGuideChannelLocked(channel, categoriesById, uiState.parentalControlLevel)) {
                            requestLockedGuideAction(LockedGuideAction.PlayArchive(channel, program, returnRoute))
                        } else {
                            onPlayArchive(
                                channel,
                                program,
                                playerCategoryId,
                                playerIsVirtualCategory,
                                uiState.combinedProfileId,
                                returnRoute
                            )
                        }
                    }
                } else {
                    null
                },
                reminderButtonLabel = reminderButtonLabel,
                onToggleReminder = reminderButtonLabel?.let {
                    {
                        if (reminderStateMatches && programReminderUiState.isScheduled) {
                            viewModel.toggleProgramReminder(channel, program)
                        } else {
                            notificationPermissionGate.runReminderAction {
                                viewModel.toggleProgramReminder(channel, program)
                            }
                        }
                    }
                },
                onScheduleRecording = if (canScheduleRecording) {
                    {
                        notificationPermissionGate.runRecordingAction {
                            viewModel.scheduleRecording(channel, program)
                        }
                    }
                } else {
                    null
                },
                onScheduleDailyRecording = if (canScheduleRecording) {
                    {
                        notificationPermissionGate.runRecordingAction {
                            viewModel.scheduleRecording(channel, program, com.afterglowtv.domain.model.RecordingRecurrence.DAILY)
                        }
                    }
                } else {
                    null
                },
                onScheduleWeeklyRecording = if (canScheduleRecording) {
                    {
                        notificationPermissionGate.runRecordingAction {
                            viewModel.scheduleRecording(channel, program, com.afterglowtv.domain.model.RecordingRecurrence.WEEKLY)
                        }
                    }
                } else {
                    null
                }
            )
        }
    }

    if (overrideUiState.channel != null) {
        EpgOverrideDialog(
            state = overrideUiState,
            onDismiss = viewModel::dismissEpgOverride,
            onQueryChange = viewModel::updateEpgOverrideSearch,
            onCandidateSelected = viewModel::applyEpgOverride,
            onClearOverride = viewModel::clearEpgOverride
        )
    }
}

@Composable
private fun GuideProgramMetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceDim
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GuideStatusCard(
    isArchiveReady: Boolean,
    providerCatchUpSupported: Boolean,
    isGuideStale: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.epg_program_status_title),
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface
            )
            Text(
                text = when {
                    isArchiveReady -> stringResource(R.string.epg_archive_ready_hint)
                    providerCatchUpSupported -> stringResource(R.string.epg_archive_provider_hint)
                    else -> stringResource(R.string.epg_archive_unavailable_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isArchiveReady) Primary else OnSurfaceDim
            )
            if (isGuideStale) {
                Text(
                    text = stringResource(R.string.epg_archive_stale_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }
        }
    }
}

@Composable
private fun GuideProviderTroubleshootingCard(
    summary: String,
    channel: Channel,
    program: Program,
    isGuideStale: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceHighlight.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.epg_provider_troubleshooting_title),
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface
            )
            val channelReason = when {
                !channel.catchUpSupported && !program.hasArchive ->
                    stringResource(R.string.epg_provider_troubleshooting_no_archive)
                channel.catchUpSupported && channel.catchUpSource.isNullOrBlank() ->
                    stringResource(R.string.epg_provider_troubleshooting_missing_template)
                channel.streamId <= 0L ->
                    stringResource(R.string.epg_provider_troubleshooting_missing_stream_id)
                else ->
                    stringResource(R.string.epg_provider_troubleshooting_ready)
            }
            Text(
                text = channelReason,
                style = MaterialTheme.typography.bodySmall,
                color = if (program.hasArchive) Primary else OnSurfaceDim
            )
            if (isGuideStale) {
                Text(
                    text = stringResource(R.string.epg_provider_troubleshooting_stale),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }
        }
    }
}

internal fun isGuideCategoryLocked(category: Category, parentalControlLevel: Int): Boolean =
    parentalControlLevel in 1..2 && category.isUserProtected

private fun isGuideChannelLocked(
    channel: Channel,
    categoriesById: Map<Long, Category>,
    parentalControlLevel: Int
): Boolean {
    if (parentalControlLevel !in 1..2) {
        return false
    }
    val categoryLocked = channel.categoryId?.let(categoriesById::get)?.let { category ->
        category.isUserProtected
    } ?: false
    return channel.isUserProtected || categoryLocked
}
