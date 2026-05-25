package com.afterglowtv.app.ui.screens.epg

import com.afterglowtv.app.ui.model.AdultGuideCategory
import com.afterglowtv.app.ui.model.AdultGuideCategoryBuilder
import com.afterglowtv.app.ui.model.isArchivePlayable
import com.afterglowtv.app.ui.model.guideLookupKey
import com.afterglowtv.app.ui.model.isAdultGuideCategory
import com.afterglowtv.app.ui.model.isAdultGuideChannel
import com.afterglowtv.app.ui.model.isExplicitAdultGuideChannel
import com.afterglowtv.app.ui.model.isLikelyAdultGuideCategory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.ui.model.applyProviderCategoryDisplayPreferences
import com.afterglowtv.app.ui.model.orderedByRequestedRawIds
import com.afterglowtv.domain.manager.ParentalControlManager
import com.afterglowtv.domain.manager.ProgramReminderManager
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.ChannelEpgMapping
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.CombinedCategory
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.EpgOverrideCandidate
import com.afterglowtv.domain.model.Favorite
import com.afterglowtv.domain.model.Program
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.ChannelRepository
import com.afterglowtv.domain.repository.CombinedM3uRepository
import com.afterglowtv.domain.repository.EpgRepository
import com.afterglowtv.domain.repository.EpgSourceRepository
import com.afterglowtv.domain.repository.FavoriteRepository
import com.afterglowtv.domain.repository.LiveStreamProgramRequest
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.model.RecordingRecurrence
import com.afterglowtv.domain.model.RecordingItem
import com.afterglowtv.domain.model.RecordingRequest
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.manager.RecordingManager
import com.afterglowtv.domain.usecase.GetCustomCategories
import com.afterglowtv.domain.usecase.ScheduleRecording
import com.afterglowtv.domain.usecase.ScheduleRecordingCommand
import com.afterglowtv.domain.util.AdultContentVisibilityPolicy
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class RecordingConflictInfo(
    val conflictingItems: List<RecordingItem>,
    val pendingRequest: RecordingRequest,
    val programTitle: String
)

data class EpgUiState(
    val currentProviderName: String? = null,
    val providerSourceLabel: String = "",
    val providerArchiveSummary: String = "",
    val combinedProfileId: Long? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long = ChannelRepository.ALL_CHANNELS_ID,
    val programSearchQuery: String = "",
    val showScheduledOnly: Boolean = false,
    val selectedChannelMode: GuideChannelMode = GuideChannelMode.ALL,
    val selectedDensity: GuideDensity = GuideDensity.COMPACT,
    val parentalControlLevel: Int = 0,
    val showFavoritesOnly: Boolean = false,
    val favoriteChannelIds: Set<Long> = emptySet(),
    val channels: List<Channel> = emptyList(),
    val programsByChannel: Map<String, List<Program>> = emptyMap(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val totalChannelCount: Int = 0,
    val channelsWithSchedule: Int = 0,
    val failedScheduleCount: Int = 0,
    val lastUpdatedAt: Long? = null,
    val isGuideStale: Boolean = false,
    val guideAnchorTime: Long = DEFAULT_NOW,
    val guideWindowStart: Long = DEFAULT_NOW - EpgViewModel.LOOKBACK_MS,
    val guideWindowEnd: Long = DEFAULT_NOW + EpgViewModel.LOOKAHEAD_MS,
    val loadedChannelCount: Int = 0,
    val recordingMessage: String? = null,
    val pendingRecordingConflict: RecordingConflictInfo? = null,
    val hasMoreChannels: Boolean = false,
    val adultGuideCategories: List<AdultGuideCategory> = emptyList(),
    val adultGuideCategorizedChannelCount: Int = 0,
    val isAdultGuideCategorizing: Boolean = false
) {
    companion object {
        private val DEFAULT_NOW = System.currentTimeMillis()
    }
}

data class EpgOverrideUiState(
    val channel: Channel? = null,
    val currentMapping: ChannelEpgMapping? = null,
    val searchQuery: String = "",
    val candidates: List<EpgOverrideCandidate> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

data class ProgramReminderUiState(
    val providerId: Long = 0L,
    val channelId: String = "",
    val programTitle: String = "",
    val programStartTime: Long = 0L,
    val isScheduled: Boolean = false,
    val isLoading: Boolean = false
) {
    fun matches(providerId: Long, channelId: String, programTitle: String, programStartTime: Long): Boolean =
        this.providerId == providerId &&
            this.channelId == channelId &&
            this.programTitle == programTitle &&
            this.programStartTime == programStartTime
}

enum class GuideChannelMode {
    ALL,
    ANCHORED,
    ARCHIVE_READY
}

enum class GuideDensity {
    COMPACT,
    COMFORTABLE,
    CINEMATIC
}

private data class GuideProgramsResult(
    val programsByChannel: Map<String, List<Program>>,
    val failedCount: Int
)

private data class GuideChannelSelection(
    val channels: List<Channel>,
    val favoriteChannelIds: Set<Long>
)

private enum class GuideSurface {
    STANDARD_EPG,
    ADULT_GUIDE
}

private data class GuideBaseRequest(
    val categories: List<Category>,
    val hiddenCategoryIds: Set<Long>,
    val adultCategoryIds: Set<Long>,
    val surface: GuideSurface,
    val resolvedCategoryId: Long,
    val parentalControlLevel: Int,
    val anchorTime: Long,
    val favoritesOnly: Boolean,
    val windowStart: Long,
    val windowEnd: Long,
    val estimatedChannelCount: Int
)

private data class GuideBaseSnapshot(
    val providerId: Long,
    val currentProviderName: String,
    val providerSourceLabel: String,
    val providerArchiveSummary: String,
    val categories: List<Category>,
    val selectedCategoryId: Long,
    val parentalControlLevel: Int,
    val showFavoritesOnly: Boolean,
    val favoriteChannelIds: Set<Long>,
    val allChannels: List<Channel>,
    val visibleChannels: List<Channel>,
    val baseProgramsByChannel: Map<String, List<Program>>,
    val failedScheduleCount: Int,
    val lastUpdatedAt: Long,
    val baseChannelsWithSchedule: Int,
    val baseGuideStale: Boolean,
    val guideAnchorTime: Long,
    val guideWindowStart: Long,
    val guideWindowEnd: Long,
    val hiddenCategoryIds: Set<Long> = emptySet(),
    val hasMoreChannels: Boolean = false
)

private data class GuideDisplaySnapshot(
    val channels: List<Channel>,
    val programsByChannel: Map<String, List<Program>>,
    val totalChannelCount: Int,
    val channelsWithSchedule: Int,
    val isGuideStale: Boolean
)

private data class GuideSelectionRequest(
    val requestedCategoryId: Long,
    val anchorTime: Long,
    val favoritesOnly: Boolean,
    val parentalControlLevel: Int,
    val unlockedCategoryIds: Set<Long>,
    val isStartupSelection: Boolean
)

private data class CombinedGuideDependencies(
    val combinedCategories: List<CombinedCategory>,
    val providerIds: List<Long>,
    val customCategories: List<Category>,
    val selection: Pair<GuideSelectionSeed, Int>
)

private data class CombinedGuideRequest(
    val request: GuideBaseRequest,
    val providerIds: List<Long>
)

private data class GuideFallbackContext(
    val providerId: Long,
    val selectedCategoryId: Long,
    val guideAnchorTime: Long,
    val guideWindowStart: Long,
    val guideWindowEnd: Long,
    val visibleChannelIds: List<Long>
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EpgViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val combinedM3uRepository: CombinedM3uRepository,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val epgSourceRepository: EpgSourceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val preferencesRepository: PreferencesRepository,
    private val parentalControlManager: ParentalControlManager,
    private val programReminderManager: ProgramReminderManager,
    private val getCustomCategories: GetCustomCategories,
    private val scheduleRecording: ScheduleRecording,
    private val recordingManager: RecordingManager,
    private val positionMemo: EpgPositionMemo,
) : ViewModel() {

    /** Read by [FullEpgScreen] at first composition to restore the user's last
     *  spot in the guide. Returns null on first launch / after [positionMemo.clear]. */
    fun lastPosition(): EpgPositionMemo.Snapshot? = positionMemo.snapshot()

    /** Called by [FullEpgScreen] from `onChannelFocused` / `onProgramFocused`. */
    fun rememberPosition(channelId: Long, programStartMs: Long, categoryId: Long?) {
        positionMemo.remember(channelId, programStartMs, categoryId)
    }

    companion object {
        private const val MAX_XTREAM_GUIDE_FALLBACK_PROGRAMS = 6
        const val LOOKBACK_MS = 60 * 60 * 1000L
        const val LOOKAHEAD_MS = 6 * 60 * 60 * 1000L
        const val HALF_HOUR_SHIFT_MS = 30 * 60 * 1000L
        const val WINDOW_SHIFT_MS = 3 * 60 * 60 * 1000L
        const val PAGE_SHIFT_MS = LOOKBACK_MS + LOOKAHEAD_MS
        const val DAY_SHIFT_MS = 24 * 60 * 60 * 1000L
        const val PRIME_TIME_HOUR = 20
        const val NO_ACTIVE_PROVIDER = "NO_ACTIVE_PROVIDER"
        private const val MINUTE_MS = 60 * 1000L
        private const val MIN_PLACEHOLDER_RECORDING_MS = 60 * MINUTE_MS
        private const val ADULT_GUIDE_RENDER_PAGE_SIZE = 240
        private const val ADULT_GUIDE_CATEGORY_CHUNK_SIZE = 200
    }

    private val _uiState = MutableStateFlow(EpgUiState())
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    private val selectedCategoryId = MutableStateFlow(ChannelRepository.ALL_CHANNELS_ID)
    private val guideAnchorTime = MutableStateFlow(System.currentTimeMillis())
    private val showScheduledOnly = MutableStateFlow(false)
    private val selectedChannelMode = MutableStateFlow(GuideChannelMode.ALL)
    private val selectedDensity = MutableStateFlow(GuideDensity.COMPACT)
    private val showFavoritesOnly = MutableStateFlow(false)
    private val programSearchQuery = MutableStateFlow("")
    private val startupCategoryId = MutableStateFlow<Long?>(null)
    private val fixedCategoryId = MutableStateFlow<Long?>(null)
    private val refreshNonce = MutableStateFlow(0)
    private val baseGuideSnapshot = MutableStateFlow<GuideBaseSnapshot?>(null)
    private val _overrideUiState = MutableStateFlow(EpgOverrideUiState())
    val overrideUiState: StateFlow<EpgOverrideUiState> = _overrideUiState.asStateFlow()
    private val _programReminderUiState = MutableStateFlow(ProgramReminderUiState())
    val programReminderUiState: StateFlow<ProgramReminderUiState> = _programReminderUiState.asStateFlow()
    private var overrideSearchJob: Job? = null
    private var guideFallbackJob: Job? = null
    private var adultGuideCategorizationJob: Job? = null
    private var combinedCategoriesById: Map<Long, CombinedCategory> = emptyMap()
    private var adultGuideRenderLimit: Int = ADULT_GUIDE_RENDER_PAGE_SIZE

    init {
        restoreGuidePreferences()
        observeGuideBase()
        observeGuidePresentation()
    }

    fun selectCategory(categoryId: Long) {
        fixedCategoryId.value?.let { lockedCategoryId ->
            if (categoryId != lockedCategoryId) return
        }
        startupCategoryId.value = null
        if (categoryId != VirtualCategoryIds.FAVORITES && showFavoritesOnly.value) {
            showFavoritesOnly.value = false
            viewModelScope.launch {
                preferencesRepository.setGuideFavoritesOnly(false)
            }
        }
        if (selectedCategoryId.value == categoryId) return
        baseGuideSnapshot.value?.providerId?.takeIf { it > 0L }?.let { providerId ->
            parentalControlManager.retainUnlockedCategory(
                providerId = providerId,
                categoryId = categoryId.takeIf { it > 0L && it != ChannelRepository.ALL_CHANNELS_ID }
            )
        }
        selectedCategoryId.value = categoryId
    }

    fun updateProgramSearchQuery(query: String) {
        programSearchQuery.value = query
    }

    fun clearProgramSearch() {
        if (programSearchQuery.value.isBlank()) return
        programSearchQuery.value = ""
    }

    suspend fun verifyPin(pin: String): Boolean =
        preferencesRepository.verifyParentalPin(pin)

    fun unlockCategory(categoryId: Long) {
        val providerId = baseGuideSnapshot.value?.providerId?.takeIf { it > 0L } ?: return
        parentalControlManager.unlockCategory(providerId, categoryId)
    }

    fun refresh() {
        refreshNonce.update { it + 1 }
    }

    fun requestMoreChannels() {
        val snapshot = baseGuideSnapshot.value ?: return
        if (snapshot.selectedCategoryId != VirtualCategoryIds.ADULT_GUIDE || !snapshot.hasMoreChannels) return
        val nextLimit = (snapshot.visibleChannels.size + ADULT_GUIDE_RENDER_PAGE_SIZE)
            .coerceAtMost(snapshot.allChannels.size)
        adultGuideRenderLimit = nextLimit
        baseGuideSnapshot.value = snapshot.copy(
            visibleChannels = snapshot.allChannels.take(nextLimit),
            hasMoreChannels = nextLimit < snapshot.allChannels.size
        )
    }

    fun categorizeNextAdultGuideChunk() {
        val snapshot = baseGuideSnapshot.value ?: return
        if (snapshot.selectedCategoryId != VirtualCategoryIds.ADULT_GUIDE) return
        if (adultGuideCategorizationJob?.isActive == true || _uiState.value.isAdultGuideCategorizing) return
        val channels = snapshot.allChannels
        if (channels.isEmpty()) return

        val nextCount = (_uiState.value.adultGuideCategorizedChannelCount + ADULT_GUIDE_CATEGORY_CHUNK_SIZE)
            .coerceAtMost(channels.size)
        val isComplete = nextCount >= channels.size
        val chunkChannels = channels.subList(0, nextCount).toList()
        val providerCategories = snapshot.categories
        adultGuideCategorizationJob?.cancel()
        adultGuideCategorizationJob = viewModelScope.launch {
            _uiState.update { current ->
                if (current.selectedCategoryId == VirtualCategoryIds.ADULT_GUIDE) {
                    current.copy(isAdultGuideCategorizing = true)
                } else {
                    current
                }
            }
            val categories = withContext(Dispatchers.Default) {
                AdultGuideCategoryBuilder.build(
                    channels = chunkChannels,
                    providerCategories = providerCategories,
                    includeAllCategory = isComplete
                )
            }
            _uiState.update { current ->
                if (current.selectedCategoryId != VirtualCategoryIds.ADULT_GUIDE) {
                    current
                } else {
                    current.copy(
                        adultGuideCategories = categories,
                        adultGuideCategorizedChannelCount = nextCount,
                        isAdultGuideCategorizing = false
                    )
                }
            }
            preferencesRepository.setAdultGuideCategorizedChannelCount(snapshot.providerId, nextCount)
        }
    }

    fun openEpgOverride(channel: Channel) {
        _overrideUiState.value = EpgOverrideUiState(
            channel = channel,
            isLoading = true
        )
        loadEpgOverrideCandidates(channel = channel, query = "", refreshMapping = true)
    }

    fun dismissEpgOverride() {
        overrideSearchJob?.cancel()
        _overrideUiState.value = EpgOverrideUiState()
    }

    fun loadProgramReminderState(channel: Channel, program: Program) {
        if (program.isPlaceholder) {
            _programReminderUiState.value = ProgramReminderUiState()
            return
        }
        val providerId = program.providerId.takeIf { it > 0L } ?: channel.providerId
        val channelId = program.channelId
        if (providerId <= 0L || channelId.isBlank()) {
            _programReminderUiState.value = ProgramReminderUiState()
            return
        }
        _programReminderUiState.update {
            it.copy(
                providerId = providerId,
                channelId = channelId,
                programTitle = program.title,
                programStartTime = program.startTime,
                isLoading = true
            )
        }
        viewModelScope.launch {
            val scheduled = programReminderManager.isReminderScheduled(
                providerId = providerId,
                channelId = channelId,
                programTitle = program.title,
                programStartTime = program.startTime
            )
            _programReminderUiState.value = ProgramReminderUiState(
                providerId = providerId,
                channelId = channelId,
                programTitle = program.title,
                programStartTime = program.startTime,
                isScheduled = scheduled,
                isLoading = false
            )
        }
    }

    fun toggleProgramReminder(channel: Channel, program: Program) {
        if (program.isPlaceholder) return
        val providerId = program.providerId.takeIf { it > 0L } ?: channel.providerId
        val channelId = program.channelId
        if (providerId <= 0L || channelId.isBlank()) return
        val currentState = _programReminderUiState.value
        val isScheduled = currentState.matches(providerId, channelId, program.title, program.startTime) &&
            currentState.isScheduled
        _programReminderUiState.update {
            it.copy(
                providerId = providerId,
                channelId = channelId,
                programTitle = program.title,
                programStartTime = program.startTime,
                isLoading = true
            )
        }
        viewModelScope.launch {
            if (isScheduled) {
                programReminderManager.cancelReminder(
                    providerId = providerId,
                    channelId = channelId,
                    programTitle = program.title,
                    programStartTime = program.startTime
                )
            } else {
                programReminderManager.scheduleReminder(
                    providerId = providerId,
                    channelId = channelId,
                    channelName = channel.name,
                    program = program
                )
            }
            loadProgramReminderState(channel, program)
        }
    }

    fun scheduleRecording(channel: Channel, program: Program, recurrence: RecordingRecurrence = RecordingRecurrence.NONE) {
        viewModelScope.launch {
            val recordingProgram = if (program.title.isBlank()) {
                program.copy(title = channel.name)
            } else {
                program
            }
            val nowMs = System.currentTimeMillis()
            val scheduledStartMs = maxOf(nowMs, recordingProgram.startTime)
            val scheduledEndMs = resolveGuideRecordingEndMs(recordingProgram, scheduledStartMs)
            val normalizedRecordingProgram = recordingProgram.copy(endTime = scheduledEndMs)
            val command = ScheduleRecordingCommand(
                contentType = ContentType.LIVE,
                providerId = channel.providerId,
                channel = channel,
                streamUrl = channel.streamUrl,
                currentProgram = normalizedRecordingProgram,
                nextProgram = null,
                recurrence = recurrence
            )
            val result = scheduleRecording(command)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(recordingMessage = "Recording scheduled: ${normalizedRecordingProgram.title}") }
                }
                is Result.Error -> {
                    val msg = result.message.orEmpty()
                    if (msg.contains("conflicts", ignoreCase = true)) {
                        val conflicts = recordingManager.getConflictingRecordings(
                            scheduledStartMs, scheduledEndMs, channel.providerId
                        )
                        if (conflicts.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    pendingRecordingConflict = RecordingConflictInfo(
                                        conflictingItems = conflicts,
                                        pendingRequest = RecordingRequest(
                                            providerId = channel.providerId,
                                            channelId = channel.id,
                                            channelName = channel.name,
                                            streamUrl = channel.streamUrl,
                                            scheduledStartMs = scheduledStartMs,
                                            scheduledEndMs = scheduledEndMs,
                                            programTitle = normalizedRecordingProgram.title,
                                            recurrence = recurrence
                                        ),
                                        programTitle = normalizedRecordingProgram.title.ifBlank { channel.name }
                                    )
                                )
                            }
                            return@launch
                        }
                    }
                    _uiState.update { it.copy(recordingMessage = msg.ifBlank { "Failed to schedule recording" }) }
                }
                else -> {}
            }
        }
    }

    private fun resolveGuideRecordingEndMs(program: Program, scheduledStartMs: Long): Long {
        if (!program.isPlaceholder) return program.endTime
        return maxOf(program.endTime, scheduledStartMs + MIN_PLACEHOLDER_RECORDING_MS)
    }

    fun forceScheduleRecording() {
        val conflict = _uiState.value.pendingRecordingConflict ?: return
        viewModelScope.launch {
            val result = recordingManager.forceScheduleRecording(conflict.pendingRequest)
            val message = when (result) {
                is Result.Success -> "Recording scheduled: ${conflict.programTitle}"
                is Result.Error -> result.message ?: "Failed to schedule recording"
                else -> return@launch
            }
            _uiState.update { it.copy(recordingMessage = message, pendingRecordingConflict = null) }
        }
    }

    fun dismissRecordingConflict() {
        _uiState.update { it.copy(pendingRecordingConflict = null) }
    }

    fun clearRecordingMessage() {
        _uiState.update { it.copy(recordingMessage = null) }
    }

    fun updateEpgOverrideSearch(query: String) {
        val channel = _overrideUiState.value.channel ?: return
        _overrideUiState.update {
            it.copy(
                searchQuery = query,
                isLoading = true,
                error = null
            )
        }
        loadEpgOverrideCandidates(channel = channel, query = query, refreshMapping = false)
    }

    fun applyEpgOverride(candidate: EpgOverrideCandidate) {
        val channel = _overrideUiState.value.channel ?: return
        viewModelScope.launch {
            _overrideUiState.update { it.copy(isSaving = true, error = null) }
            when (val result = epgSourceRepository.applyManualOverride(
                providerId = channel.providerId,
                channelId = channel.id,
                epgSourceId = candidate.epgSourceId,
                xmltvChannelId = candidate.xmltvChannelId
            )) {
                is com.afterglowtv.domain.model.Result.Error -> {
                    _overrideUiState.update { it.copy(isSaving = false, error = result.message) }
                }
                else -> {
                    dismissEpgOverride()
                    refresh()
                }
            }
        }
    }

    fun clearEpgOverride() {
        val channel = _overrideUiState.value.channel ?: return
        viewModelScope.launch {
            _overrideUiState.update { it.copy(isSaving = true, error = null) }
            when (val result = epgSourceRepository.clearManualOverride(channel.providerId, channel.id)) {
                is com.afterglowtv.domain.model.Result.Error -> {
                    _overrideUiState.update { it.copy(isSaving = false, error = result.message) }
                }
                else -> {
                    dismissEpgOverride()
                    refresh()
                }
            }
        }
    }

    fun jumpToNow() {
        updateGuideAnchorTime(System.currentTimeMillis())
    }

    fun jumpForwardHalfHour() {
        updateGuideAnchorTime(guideAnchorTime.value + HALF_HOUR_SHIFT_MS)
    }

    fun jumpBackwardHalfHour() {
        updateGuideAnchorTime((guideAnchorTime.value - HALF_HOUR_SHIFT_MS).coerceAtLeast(0L))
    }

    fun jumpForward() {
        updateGuideAnchorTime(guideAnchorTime.value + WINDOW_SHIFT_MS)
    }

    fun jumpBackward() {
        updateGuideAnchorTime((guideAnchorTime.value - WINDOW_SHIFT_MS).coerceAtLeast(0L))
    }

    fun pageBackward() {
        updateGuideAnchorTime((guideAnchorTime.value - PAGE_SHIFT_MS).coerceAtLeast(0L))
    }

    fun pageForward() {
        updateGuideAnchorTime(guideAnchorTime.value + PAGE_SHIFT_MS)
    }

    fun jumpToTomorrow() {
        updateGuideAnchorTime(shiftGuideAnchorByDays(System.currentTimeMillis(), 1))
    }

    fun jumpToPrimeTime() {
        updateGuideAnchorTime(guidePrimeTimeAnchor(guideAnchorTime.value, PRIME_TIME_HOUR))
    }

    fun jumpToPreviousDay() {
        updateGuideAnchorTime(shiftGuideAnchorByDays(guideAnchorTime.value, -1).coerceAtLeast(0L))
    }

    fun jumpToNextDay() {
        updateGuideAnchorTime(shiftGuideAnchorByDays(guideAnchorTime.value, 1))
    }

    fun jumpToDay(dayStartMillis: Long) {
        updateGuideAnchorTime(jumpGuideAnchorToDay(guideAnchorTime.value, dayStartMillis))
    }

    fun toggleScheduledOnly() {
        val enabled = !showScheduledOnly.value
        showScheduledOnly.value = enabled
        viewModelScope.launch {
            preferencesRepository.setGuideScheduledOnly(enabled)
        }
    }

    fun selectChannelMode(mode: GuideChannelMode) {
        selectedChannelMode.value = mode
        viewModelScope.launch {
            preferencesRepository.setGuideChannelMode(mode.name)
        }
    }

    fun selectDensity(density: GuideDensity) {
        selectedDensity.value = density
        viewModelScope.launch {
            preferencesRepository.setGuideDensity(density.name)
        }
    }

    fun toggleFavoritesOnly() {
        val enabled = !showFavoritesOnly.value
        showFavoritesOnly.value = enabled
        viewModelScope.launch {
            preferencesRepository.setGuideFavoritesOnly(enabled)
        }
    }

    fun resetGuideFilters() {
        startupCategoryId.value = null
        selectedCategoryId.value = fixedCategoryId.value ?: ChannelRepository.ALL_CHANNELS_ID
        programSearchQuery.value = ""
        showScheduledOnly.value = false
        selectedChannelMode.value = GuideChannelMode.ALL
        showFavoritesOnly.value = false
        viewModelScope.launch {
            preferencesRepository.setGuideDefaultCategoryId(ChannelRepository.ALL_CHANNELS_ID)
            preferencesRepository.setShowAllChannelsCategory(true)
            preferencesRepository.setGuideScheduledOnly(false)
            preferencesRepository.setGuideChannelMode(GuideChannelMode.ALL.name)
            preferencesRepository.setGuideFavoritesOnly(false)
        }
    }

    fun applyNavigationContext(
        categoryId: Long?,
        anchorTime: Long?,
        favoritesOnly: Boolean?,
        lockCategory: Boolean = false
    ) {
        fixedCategoryId.value = if (lockCategory) categoryId else null
        categoryId?.let { requested ->
            startupCategoryId.value = null
            selectedCategoryId.value = requested
        }
        anchorTime?.takeIf { it > 0L }?.let { requested ->
            guideAnchorTime.value = requested
        }
        if (lockCategory) {
            showFavoritesOnly.value = false
            showScheduledOnly.value = false
            selectedChannelMode.value = GuideChannelMode.ALL
        } else {
            favoritesOnly?.let { requested ->
                showFavoritesOnly.value = requested
            }
        }
    }

    private fun observeGuideBase() {
        viewModelScope.launch {
            combine(
                combinedM3uRepository.getActiveLiveSource(),
                providerRepository.getActiveProvider()
            ) { activeSource, activeProvider ->
                Pair(activeSource ?: activeProvider?.id?.let { ActiveLiveSource.ProviderSource(it) }, activeProvider)
            }.distinctUntilChanged().collectLatest { (activeSource, activeProvider) ->
                if (activeSource == null && activeProvider == null) {
                    guideFallbackJob?.cancel()
                    baseGuideSnapshot.value = null
                    _uiState.update {
                        it.copy(
                            currentProviderName = null,
                            providerSourceLabel = "",
                            providerArchiveSummary = "",
                            combinedProfileId = null,
                            categories = emptyList(),
                            channels = emptyList(),
                            programsByChannel = emptyMap(),
                            isInitialLoading = false,
                            isRefreshing = false,
                            error = NO_ACTIVE_PROVIDER,
                            totalChannelCount = 0,
                            channelsWithSchedule = 0,
                            failedScheduleCount = 0,
                            lastUpdatedAt = null,
                            isGuideStale = false,
                            guideAnchorTime = System.currentTimeMillis(),
                            guideWindowStart = System.currentTimeMillis() - LOOKBACK_MS,
                            guideWindowEnd = System.currentTimeMillis() + LOOKAHEAD_MS
                        )
                    }
                    return@collectLatest
                }

                when (activeSource) {
                    is ActiveLiveSource.ProviderSource -> {
                        _uiState.update { it.copy(combinedProfileId = null) }
                        val provider = activeProvider?.takeIf { it.id == activeSource.providerId }
                            ?: providerRepository.getProvider(activeSource.providerId)
                            ?: return@collectLatest
                        observeSingleProviderGuide(provider)
                    }
                    is ActiveLiveSource.CombinedM3uSource -> {
                        _uiState.update { it.copy(combinedProfileId = activeSource.profileId) }
                        observeCombinedGuide(activeSource.profileId)
                    }
                    null -> Unit
                }
            }
        }
    }

    private suspend fun observeSingleProviderGuide(provider: com.afterglowtv.domain.model.Provider) {
        combine(
            channelRepository.getCategories(provider.id),
            getCustomCategories(provider.id, ContentType.LIVE),
            preferencesRepository.getHiddenCategoryIds(provider.id, ContentType.LIVE),
            preferencesRepository.getCategorySortMode(provider.id, ContentType.LIVE)
        ) { providerCategories, customCategories, hiddenCategoryIds, sortMode ->
            GuideCategoryData(
                providerCategories = providerCategories,
                customCategories = customCategories,
                hiddenCategoryIds = hiddenCategoryIds,
                sortMode = sortMode
            )
        }.combine(preferencesRepository.showAllChannelsCategory) { data, showAll ->
            data.copy(showAllChannels = showAll)
        }
            .combine(
                guideSelectionStateFlow().combine(
                    parentalControlManager.unlockedCategoriesForProvider(provider.id)
                ) { (selection, parentalControlLevel), unlockedCategoryIds ->
                    GuideSelectionRequest(
                        requestedCategoryId = selection.requestedCategoryId,
                        anchorTime = selection.anchorTime,
                        favoritesOnly = selection.favoritesOnly,
                        parentalControlLevel = parentalControlLevel,
                        unlockedCategoryIds = unlockedCategoryIds,
                        isStartupSelection = selection.isStartupSelection
                    )
                }
            ) { categoryData, selection ->
                val visibleProviderCategories = applyProviderCategoryDisplayPreferences(
                    categories = categoryData.providerCategories.filter { it.id != ChannelRepository.ALL_CHANNELS_ID },
                    hiddenCategoryIds = categoryData.hiddenCategoryIds,
                    sortMode = categoryData.sortMode
                )
                val adultCategoryIds = visibleProviderCategories
                    .filter(::isLikelyAdultGuideCategory)
                    .map(Category::id)
                    .toSet()
                val orderedCategories = buildGuideCategoryList(
                    providerCategories = visibleProviderCategories,
                    customCategories = categoryData.customCategories,
                    showAllChannels = categoryData.showAllChannels
                )
                val resolvedCategoryId = resolveGuideCategorySelection(
                    requestedCategoryId = selection.requestedCategoryId,
                    categories = orderedCategories,
                    parentalControlLevel = selection.parentalControlLevel,
                    unlockedCategoryIds = selection.unlockedCategoryIds,
                    fallbackFromEmptyFavorites = selection.isStartupSelection
                )
                GuideBaseRequest(
                    categories = orderedCategories,
                    hiddenCategoryIds = categoryData.hiddenCategoryIds,
                    adultCategoryIds = adultCategoryIds,
                    surface = resolvedCategoryId.toGuideSurface(),
                    resolvedCategoryId = resolvedCategoryId,
                    parentalControlLevel = selection.parentalControlLevel,
                    anchorTime = selection.anchorTime,
                    favoritesOnly = selection.favoritesOnly,
                    windowStart = selection.anchorTime - LOOKBACK_MS,
                    windowEnd = selection.anchorTime + LOOKAHEAD_MS,
                    estimatedChannelCount = estimateGuideChannelCount(
                        resolvedCategoryId = resolvedCategoryId,
                        categories = orderedCategories
                    )
                )
            }.collectLatest { request ->
                val categories = request.categories
                val hasVisibleGuide = _uiState.value.channels.isNotEmpty() || _uiState.value.programsByChannel.isNotEmpty()
                val providerSourceLabel = buildProviderSourceLabel(provider)
                val providerArchiveSummary = buildProviderArchiveSummary(provider)
                if (request.isAdultGuideRequest()) {
                    adultGuideRenderLimit = ADULT_GUIDE_RENDER_PAGE_SIZE
                } else {
                    clearAdultGuideCategorization()
                }
                _uiState.update {
                    it.copy(
                        currentProviderName = provider.name,
                        providerSourceLabel = providerSourceLabel,
                        providerArchiveSummary = providerArchiveSummary,
                        categories = categories,
                        parentalControlLevel = request.parentalControlLevel,
                        showFavoritesOnly = request.favoritesOnly,
                        selectedCategoryId = request.resolvedCategoryId,
                        guideAnchorTime = request.anchorTime,
                        guideWindowStart = request.windowStart,
                        guideWindowEnd = request.windowEnd,
                        isInitialLoading = !hasVisibleGuide,
                        isRefreshing = hasVisibleGuide,
                        loadedChannelCount = 0,
                        totalChannelCount = request.estimatedChannelCount,
                        adultGuideCategories = it.adultGuideCategories,
                        adultGuideCategorizedChannelCount = it.adultGuideCategorizedChannelCount,
                        isAdultGuideCategorizing = false,
                        error = null
                    )
                }

                if (request.shouldSkipEmptyStandardGuideScan()) {
                    val emptySelection = GuideChannelSelection(
                        channels = emptyList(),
                        favoriteChannelIds = emptySet()
                    )
                    publishGuideChannelLoadProgress(
                        providerName = provider.name,
                        providerSourceLabel = providerSourceLabel,
                        providerArchiveSummary = providerArchiveSummary,
                        categories = categories,
                        request = request,
                        channelSelection = emptySelection,
                        visibleChannels = emptyList()
                    )
                    publishGuideSnapshot(
                        providerId = provider.id,
                        providerName = provider.name,
                        providerSourceLabel = providerSourceLabel,
                        providerArchiveSummary = providerArchiveSummary,
                        categories = categories,
                        request = request,
                        channelSelection = emptySelection,
                        guideResult = GuideProgramsResult(emptyMap(), failedCount = 0)
                    )
                    finalizeStartupCategory(request.resolvedCategoryId)
                    return@collectLatest
                }

                combine(loadGuideChannelsForProvider(provider.id, request), favoriteRepository.getFavorites(provider.id, ContentType.LIVE)) { preferredChannels, favorites ->
                    val favoriteIds = favorites.map { it.contentId }.toSet()
                    val visibleChannels = preferredChannels.filterNot { channel -> channel.categoryId in request.hiddenCategoryIds }
                    val guideChannels = if (request.isAdultGuideRequest()) {
                        visibleChannels
                    } else {
                        visibleChannels.filterNot { channel -> channel.isAdultForStandardGuide(request) }
                    }
                    GuideChannelSelection(
                        channels = guideChannels,
                        favoriteChannelIds = favoriteIds
                    )
                }.collectLatest { channelSelection ->
                    val visibleChannels = request.visibleInitialChannels(channelSelection.channels)
                    publishGuideChannelLoadProgress(
                        providerName = provider.name,
                        providerSourceLabel = providerSourceLabel,
                        providerArchiveSummary = providerArchiveSummary,
                        categories = categories,
                        request = request,
                        channelSelection = channelSelection,
                        visibleChannels = visibleChannels
                    )
                    val guideResult = if (request.isAdultGuideRequest()) {
                        GuideProgramsResult(emptyMap(), failedCount = 0)
                    } else {
                        loadGuidePrograms(
                            providerId = provider.id,
                            channels = visibleChannels,
                            windowStart = request.windowStart,
                            windowEnd = request.windowEnd
                        )
                    }
                    publishGuideSnapshot(
                        providerId = provider.id,
                        providerName = provider.name,
                        providerSourceLabel = providerSourceLabel,
                        providerArchiveSummary = providerArchiveSummary,
                        categories = categories,
                        request = request,
                        channelSelection = channelSelection,
                        guideResult = guideResult
                    )
                    if (!request.isAdultGuideRequest()) {
                        scheduleGuideFallbackEnrichment(
                            snapshotContext = GuideFallbackContext(
                                providerId = provider.id,
                                selectedCategoryId = request.resolvedCategoryId,
                                guideAnchorTime = request.anchorTime,
                                guideWindowStart = request.windowStart,
                                guideWindowEnd = request.windowEnd,
                                visibleChannelIds = visibleChannels.map(Channel::id)
                            ),
                            channels = visibleChannels,
                            existingProgramsByChannel = guideResult.programsByChannel
                        )
                    }
                    finalizeStartupCategory(request.resolvedCategoryId)
                }
            }
    }

    private suspend fun observeCombinedGuide(profileId: Long) {
        val providerIdsFlow = combinedProviderIdsFlow(profileId)
        combine(
            combinedM3uRepository.getCombinedCategories(profileId),
            providerIdsFlow,
            providerIdsFlow.flatMapLatest { providerIds ->
                getCustomCategories(providerIds, ContentType.LIVE)
            },
            guideSelectionStateFlow()
        ) { combinedCategories, providerIds, customCategories, selection ->
            combinedCategoriesById = combinedCategories.associateBy { it.category.id }
            CombinedGuideDependencies(
                combinedCategories = combinedCategories,
                providerIds = providerIds,
                customCategories = customCategories,
                selection = selection
            )
        }.combine(preferencesRepository.showAllChannelsCategory) { data, showAllChannels ->
            val providerCategories = data.combinedCategories.map { it.category }
            val adultCategoryIds = providerCategories
                .filter(::isLikelyAdultGuideCategory)
                .map(Category::id)
                .toSet()
            val categories = buildGuideCategoryList(
                providerCategories = providerCategories,
                customCategories = data.customCategories,
                showAllChannels = showAllChannels
            )
            val resolvedCategoryId = resolveGuideCategorySelection(
                requestedCategoryId = data.selection.first.requestedCategoryId,
                categories = categories,
                parentalControlLevel = data.selection.second,
                unlockedCategoryIds = emptySet(),
                fallbackFromEmptyFavorites = data.selection.first.isStartupSelection
            )
            CombinedGuideRequest(
                request = GuideBaseRequest(
                    categories = categories,
                    hiddenCategoryIds = emptySet(),
                    adultCategoryIds = adultCategoryIds,
                    surface = resolvedCategoryId.toGuideSurface(),
                    resolvedCategoryId = resolvedCategoryId,
                    parentalControlLevel = data.selection.second,
                    anchorTime = data.selection.first.anchorTime,
                    favoritesOnly = data.selection.first.favoritesOnly,
                    windowStart = data.selection.first.anchorTime - LOOKBACK_MS,
                    windowEnd = data.selection.first.anchorTime + LOOKAHEAD_MS,
                    estimatedChannelCount = estimateGuideChannelCount(
                        resolvedCategoryId = resolvedCategoryId,
                        categories = categories
                    )
                ),
                providerIds = data.providerIds
            )
        }.collectLatest { combinedRequest ->
            val request = combinedRequest.request
            val providerIds = combinedRequest.providerIds
            val categories = request.categories
            val profile = combinedM3uRepository.getProfile(profileId)
            val profileName = profile?.name ?: "Combined M3U"
            val hasVisibleGuide = _uiState.value.channels.isNotEmpty() || _uiState.value.programsByChannel.isNotEmpty()
            if (request.isAdultGuideRequest()) {
                adultGuideRenderLimit = ADULT_GUIDE_RENDER_PAGE_SIZE
            } else {
                clearAdultGuideCategorization()
            }
            _uiState.update {
                it.copy(
                    currentProviderName = profileName,
                    providerSourceLabel = "Combined M3U",
                    providerArchiveSummary = "Guide data is merged from each member playlist's own EPG sources.",
                    categories = categories,
                    parentalControlLevel = request.parentalControlLevel,
                    showFavoritesOnly = request.favoritesOnly,
                    selectedCategoryId = request.resolvedCategoryId,
                    guideAnchorTime = request.anchorTime,
                    guideWindowStart = request.windowStart,
                    guideWindowEnd = request.windowEnd,
                    isInitialLoading = !hasVisibleGuide,
                    isRefreshing = hasVisibleGuide,
                    loadedChannelCount = 0,
                    totalChannelCount = request.estimatedChannelCount,
                    adultGuideCategories = it.adultGuideCategories,
                    adultGuideCategorizedChannelCount = it.adultGuideCategorizedChannelCount,
                    isAdultGuideCategorizing = false,
                    error = null
                )
            }

            if (request.shouldSkipEmptyStandardGuideScan()) {
                val emptySelection = GuideChannelSelection(
                    channels = emptyList(),
                    favoriteChannelIds = emptySet()
                )
                publishGuideChannelLoadProgress(
                    providerName = profileName,
                    providerSourceLabel = "Combined M3U",
                    providerArchiveSummary = "Guide data is merged from each member playlist's own EPG sources.",
                    categories = categories,
                    request = request,
                    channelSelection = emptySelection,
                    visibleChannels = emptyList()
                )
                publishGuideSnapshot(
                    providerId = 0L,
                    providerName = profileName,
                    providerSourceLabel = "Combined M3U",
                    providerArchiveSummary = "Guide data is merged from each member playlist's own EPG sources.",
                    categories = categories,
                    request = request,
                    channelSelection = emptySelection,
                    guideResult = GuideProgramsResult(emptyMap(), failedCount = 0)
                )
                finalizeStartupCategory(request.resolvedCategoryId)
                return@collectLatest
            }

            combine(
                combinedGuideChannels(profileId, request.resolvedCategoryId, providerIds),
                observeLiveFavorites(providerIds)
            ) { channels, favorites ->
                val favoriteIds = favorites.map { it.contentId }.toSet()
                val standardFilteredChannels = if (request.isAdultGuideRequest()) {
                    channels
                } else {
                    channels.filterNot { channel -> channel.isAdultForStandardGuide(request) }
                }
                val preferredChannels = if (request.favoritesOnly) {
                    standardFilteredChannels.filter { it.id in favoriteIds }
                } else {
                    standardFilteredChannels
                }
                GuideChannelSelection(
                    channels = preferredChannels,
                    favoriteChannelIds = favoriteIds
                )
            }.collectLatest { channelSelection ->
                val visibleChannels = request.visibleInitialChannels(channelSelection.channels)
                publishGuideChannelLoadProgress(
                    providerName = profileName,
                    providerSourceLabel = "Combined M3U",
                    providerArchiveSummary = "Guide data is merged from each member playlist's own EPG sources.",
                    categories = categories,
                    request = request,
                    channelSelection = channelSelection,
                    visibleChannels = visibleChannels
                )
                val guideResult = if (request.isAdultGuideRequest()) {
                    GuideProgramsResult(emptyMap(), failedCount = 0)
                } else {
                    loadCombinedGuidePrograms(
                        channels = visibleChannels,
                        windowStart = request.windowStart,
                        windowEnd = request.windowEnd
                    )
                }
                publishGuideSnapshot(
                    providerId = 0L,
                    providerName = profileName,
                    providerSourceLabel = "Combined M3U",
                    providerArchiveSummary = "Guide data is merged from each member playlist's own EPG sources.",
                    categories = categories,
                    request = request,
                    channelSelection = channelSelection,
                    guideResult = guideResult
                )
                if (!request.isAdultGuideRequest()) {
                    scheduleGuideFallbackEnrichment(
                        snapshotContext = GuideFallbackContext(
                            providerId = 0L,
                            selectedCategoryId = request.resolvedCategoryId,
                            guideAnchorTime = request.anchorTime,
                            guideWindowStart = request.windowStart,
                            guideWindowEnd = request.windowEnd,
                            visibleChannelIds = visibleChannels.map(Channel::id)
                        ),
                        channels = visibleChannels,
                        existingProgramsByChannel = guideResult.programsByChannel
                    )
                }
                finalizeStartupCategory(request.resolvedCategoryId)
            }
        }
    }

    private fun combinedGuideChannels(
        profileId: Long,
        categoryId: Long,
        providerIds: List<Long>
    ): Flow<List<Channel>> {
        return if (categoryId == ChannelRepository.ALL_CHANNELS_ID) {
            val flows = combinedCategoriesById.values.map { combinedM3uRepository.getCombinedChannels(profileId, it) }
            if (flows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList<Channel>())
            } else {
                combine(flows) { arrays: Array<List<Channel>> -> arrays.toList().flatMap { it } }
            }
        } else if (categoryId == VirtualCategoryIds.ADULT_GUIDE) {
            val providerCategoriesById = combinedCategoriesById.values.associate { combined ->
                combined.category.id to combined.category
            }
            val flows = combinedCategoriesById.values.map { combinedM3uRepository.getCombinedChannels(profileId, it) }
            if (flows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList<Channel>())
            } else {
                combine(flows) { arrays: Array<List<Channel>> ->
                    arrays.toList()
                        .flatMap { it }
                        .distinctBy(Channel::id)
                        .filter { channel ->
                            isAdultGuideChannel(channel, channel.categoryId?.let(providerCategoriesById::get))
                        }
                }
            }
        } else if (categoryId == VirtualCategoryIds.FAVORITES) {
            observeLiveFavorites(providerIds)
                .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
                .flatMapLatest(::loadGuideChannelsByOrderedIds)
        } else if (categoryId < 0L) {
            favoriteRepository.getFavoritesByGroup(-categoryId)
                .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
                .flatMapLatest(::loadGuideChannelsByOrderedIds)
        } else {
            val combinedCategory = combinedCategoriesById[categoryId]
            if (combinedCategory == null) {
                kotlinx.coroutines.flow.flowOf(emptyList<Channel>())
            } else {
                combinedM3uRepository.getCombinedChannels(profileId, combinedCategory)
            }
        }
    }

    private fun guideSelectionSeedFlow(): Flow<GuideSelectionSeed> =
        combine(
            selectedCategoryId,
            startupCategoryId,
            fixedCategoryId,
            guideAnchorTime,
            showFavoritesOnly
        ) { requestedCategoryId, startupSelectionId, fixedSelectionId, anchorTime, favoritesOnly ->
            val effectiveCategoryId = fixedSelectionId ?: startupSelectionId ?: requestedCategoryId
            val lockedGuide = fixedSelectionId != null
            GuideSelectionSeed(
                requestedCategoryId = effectiveCategoryId,
                anchorTime = anchorTime,
                favoritesOnly = if (lockedGuide) false else favoritesOnly,
                isStartupSelection = !lockedGuide && startupSelectionId != null
            )
        }.combine(
            refreshNonce
        ) { seed, _ ->
            seed
        }

    private fun guideSelectionStateFlow(): Flow<Pair<GuideSelectionSeed, Int>> =
        guideSelectionSeedFlow().combine(preferencesRepository.parentalControlLevel) { selection, parentalControlLevel ->
            selection to parentalControlLevel
        }

    private suspend fun publishGuideSnapshot(
        providerId: Long,
        providerName: String,
        providerSourceLabel: String,
        providerArchiveSummary: String,
        categories: List<Category>,
        request: GuideBaseRequest,
        channelSelection: GuideChannelSelection,
        guideResult: GuideProgramsResult
    ) {
        val visibleChannels = request.visibleInitialChannels(channelSelection.channels)
        val computedNow = System.currentTimeMillis()
        val computedChannelsWithSchedule = visibleChannels.count { channel ->
            channel.guideLookupKey()
                ?.let { lookupKey -> guideResult.programsByChannel[lookupKey].orEmpty().isNotEmpty() }
                ?: false
        }
        val computedHasUpcomingData = guideResult.programsByChannel.values.any { programs ->
            programs.any { program -> program.endTime > request.windowStart }
        }
        val snapshot = GuideBaseSnapshot(
            providerId = providerId,
            currentProviderName = providerName,
            providerSourceLabel = providerSourceLabel,
            providerArchiveSummary = providerArchiveSummary,
            categories = categories,
            selectedCategoryId = request.resolvedCategoryId,
            parentalControlLevel = request.parentalControlLevel,
            showFavoritesOnly = request.favoritesOnly,
            favoriteChannelIds = channelSelection.favoriteChannelIds,
            allChannels = channelSelection.channels,
            visibleChannels = visibleChannels,
            baseProgramsByChannel = guideResult.programsByChannel,
            failedScheduleCount = guideResult.failedCount,
            lastUpdatedAt = computedNow,
            baseChannelsWithSchedule = computedChannelsWithSchedule,
            baseGuideStale = visibleChannels.isNotEmpty() && (computedChannelsWithSchedule == 0 || !computedHasUpcomingData),
            guideAnchorTime = request.anchorTime,
            guideWindowStart = request.windowStart,
            guideWindowEnd = request.windowEnd,
            hiddenCategoryIds = request.hiddenCategoryIds,
            hasMoreChannels = request.hasMoreChannels(
                visibleChannels = visibleChannels,
                allChannels = channelSelection.channels
            )
        )
        baseGuideSnapshot.value = snapshot
        restoreAdultGuideCategorizationFromDisk(snapshot)
    }

    private fun restoreAdultGuideCategorizationFromDisk(snapshot: GuideBaseSnapshot) {
        if (snapshot.selectedCategoryId != VirtualCategoryIds.ADULT_GUIDE) return
        if (snapshot.allChannels.isEmpty()) return
        if (adultGuideCategorizationJob?.isActive == true) return

        adultGuideCategorizationJob = viewModelScope.launch {
            val savedCount = preferencesRepository
                .adultGuideCategorizedChannelCount(snapshot.providerId)
                .first()
                .coerceIn(0, snapshot.allChannels.size)
            val currentCount = _uiState.value.adultGuideCategorizedChannelCount
            if (savedCount <= currentCount) return@launch

            _uiState.update { current ->
                if (current.selectedCategoryId == VirtualCategoryIds.ADULT_GUIDE) {
                    current.copy(isAdultGuideCategorizing = true)
                } else {
                    current
                }
            }

            val categories = withContext(Dispatchers.Default) {
                AdultGuideCategoryBuilder.build(
                    channels = snapshot.allChannels.take(savedCount),
                    providerCategories = snapshot.categories,
                    includeAllCategory = savedCount >= snapshot.allChannels.size
                )
            }

            _uiState.update { current ->
                if (current.selectedCategoryId != VirtualCategoryIds.ADULT_GUIDE) {
                    current
                } else {
                    current.copy(
                        adultGuideCategories = categories,
                        adultGuideCategorizedChannelCount = savedCount,
                        isAdultGuideCategorizing = false
                    )
                }
            }
        }
    }

    private fun clearAdultGuideCategorization() {
        adultGuideCategorizationJob?.cancel()
        adultGuideCategorizationJob = null
        _uiState.update {
            it.copy(
                adultGuideCategories = emptyList(),
                adultGuideCategorizedChannelCount = 0,
                isAdultGuideCategorizing = false
            )
        }
    }

    private suspend fun publishGuideChannelLoadProgress(
        providerName: String,
        providerSourceLabel: String,
        providerArchiveSummary: String,
        categories: List<Category>,
        request: GuideBaseRequest,
        channelSelection: GuideChannelSelection,
        visibleChannels: List<Channel>
    ) {
        val total = channelSelection.channels.size
        if (total <= 0) {
            publishGuideChannelLoadState(
                providerName = providerName,
                providerSourceLabel = providerSourceLabel,
                providerArchiveSummary = providerArchiveSummary,
                categories = categories,
                request = request,
                channelSelection = channelSelection,
                visibleChannels = emptyList()
            )
            return
        }
        val chunkSize = when {
            total <= 80 -> 10
            total <= 400 -> 25
            total <= 1_000 -> 50
            else -> 100
        }
        var loaded = 0
        while (loaded < total) {
            loaded = (loaded + chunkSize).coerceAtMost(total)
            publishGuideChannelProgressState(
                providerName = providerName,
                providerSourceLabel = providerSourceLabel,
                providerArchiveSummary = providerArchiveSummary,
                categories = categories,
                request = request,
                channelSelection = channelSelection,
                loaded = loaded,
                total = total
            )
            delay(20L)
        }
        publishGuideChannelLoadState(
            providerName = providerName,
            providerSourceLabel = providerSourceLabel,
            providerArchiveSummary = providerArchiveSummary,
            categories = categories,
            request = request,
            channelSelection = channelSelection,
            visibleChannels = visibleChannels
        )
    }

    private fun publishGuideChannelProgressState(
        providerName: String,
        providerSourceLabel: String,
        providerArchiveSummary: String,
        categories: List<Category>,
        request: GuideBaseRequest,
        channelSelection: GuideChannelSelection,
        loaded: Int,
        total: Int
    ) {
        _uiState.update { current ->
            val keepCurrentGuide = current.channels.isNotEmpty()
            current.copy(
                currentProviderName = providerName,
                providerSourceLabel = providerSourceLabel,
                providerArchiveSummary = providerArchiveSummary,
                categories = categories,
                selectedCategoryId = request.resolvedCategoryId,
                parentalControlLevel = request.parentalControlLevel,
                showFavoritesOnly = request.favoritesOnly,
                favoriteChannelIds = channelSelection.favoriteChannelIds,
                channels = if (keepCurrentGuide) current.channels else emptyList(),
                programsByChannel = if (keepCurrentGuide) current.programsByChannel else emptyMap(),
                isInitialLoading = !keepCurrentGuide,
                isRefreshing = keepCurrentGuide,
                error = null,
                loadedChannelCount = loaded,
                totalChannelCount = total,
                guideAnchorTime = request.anchorTime,
                guideWindowStart = request.windowStart,
                guideWindowEnd = request.windowEnd
            )
        }
    }

    private fun publishGuideChannelLoadState(
        providerName: String,
        providerSourceLabel: String,
        providerArchiveSummary: String,
        categories: List<Category>,
        request: GuideBaseRequest,
        channelSelection: GuideChannelSelection,
        visibleChannels: List<Channel>
    ) {
        _uiState.update {
            it.copy(
                currentProviderName = providerName,
                providerSourceLabel = providerSourceLabel,
                providerArchiveSummary = providerArchiveSummary,
                categories = categories,
                selectedCategoryId = request.resolvedCategoryId,
                parentalControlLevel = request.parentalControlLevel,
                showFavoritesOnly = request.favoritesOnly,
                favoriteChannelIds = channelSelection.favoriteChannelIds,
                channels = visibleChannels,
                programsByChannel = emptyMap(),
                isInitialLoading = false,
                isRefreshing = true,
                error = null,
                loadedChannelCount = channelSelection.channels.size,
                totalChannelCount = channelSelection.channels.size,
                channelsWithSchedule = 0,
                failedScheduleCount = 0,
                guideAnchorTime = request.anchorTime,
                guideWindowStart = request.windowStart,
                guideWindowEnd = request.windowEnd,
                hasMoreChannels = request.hasMoreChannels(
                    visibleChannels = visibleChannels,
                    allChannels = channelSelection.channels
                )
            )
        }
    }

    private fun loadEpgOverrideCandidates(channel: Channel, query: String, refreshMapping: Boolean) {
        overrideSearchJob?.cancel()
        overrideSearchJob = viewModelScope.launch {
            val mapping = if (refreshMapping) {
                epgSourceRepository.getChannelMapping(channel.providerId, channel.id)
            } else {
                _overrideUiState.value.currentMapping
            }
            val candidates = epgSourceRepository.getOverrideCandidates(
                providerId = channel.providerId,
                query = query
            )
            _overrideUiState.update {
                it.copy(
                    channel = channel,
                    currentMapping = mapping,
                    searchQuery = query,
                    candidates = candidates,
                    isLoading = false,
                    isSaving = false,
                    error = null
                )
            }
        }
    }

    private fun observeGuidePresentation() {
        viewModelScope.launch {
            combine(
                baseGuideSnapshot,
                programSearchQuery.debounce(150L),
                showScheduledOnly,
                selectedChannelMode,
                selectedDensity
            ) { baseSnapshot, searchQuery, scheduledOnly, channelMode, density ->
                GuidePresentationState(
                    baseSnapshot = baseSnapshot,
                    searchQuery = searchQuery.trim(),
                    scheduledOnly = scheduledOnly,
                    channelMode = channelMode,
                    density = density
                )
            }.collectLatest { presentation ->
                val baseSnapshot = presentation.baseSnapshot ?: run {
                    _uiState.update {
                        it.copy(
                            programSearchQuery = presentation.searchQuery,
                            showScheduledOnly = presentation.scheduledOnly,
                            selectedChannelMode = presentation.channelMode,
                            selectedDensity = presentation.density
                        )
                    }
                    return@collectLatest
                }

                val displaySnapshot = buildGuideDisplaySnapshot(
                    baseSnapshot = baseSnapshot,
                    searchQuery = presentation.searchQuery,
                    scheduledOnly = presentation.scheduledOnly,
                    channelMode = presentation.channelMode
                )

                _uiState.update {
                    it.copy(
                        currentProviderName = baseSnapshot.currentProviderName,
                        providerSourceLabel = baseSnapshot.providerSourceLabel,
                        providerArchiveSummary = baseSnapshot.providerArchiveSummary,
                        categories = baseSnapshot.categories,
                        selectedCategoryId = baseSnapshot.selectedCategoryId,
                        parentalControlLevel = baseSnapshot.parentalControlLevel,
                        programSearchQuery = presentation.searchQuery,
                        showScheduledOnly = presentation.scheduledOnly,
                        selectedChannelMode = presentation.channelMode,
                        selectedDensity = presentation.density,
                        showFavoritesOnly = baseSnapshot.showFavoritesOnly,
                        favoriteChannelIds = baseSnapshot.favoriteChannelIds,
                        channels = displaySnapshot.channels,
                        programsByChannel = displaySnapshot.programsByChannel,
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = null,
                        totalChannelCount = displaySnapshot.totalChannelCount,
                        channelsWithSchedule = displaySnapshot.channelsWithSchedule,
                        failedScheduleCount = baseSnapshot.failedScheduleCount,
                        lastUpdatedAt = baseSnapshot.lastUpdatedAt,
                        isGuideStale = displaySnapshot.isGuideStale,
                        guideAnchorTime = baseSnapshot.guideAnchorTime,
                        guideWindowStart = baseSnapshot.guideWindowStart,
                        guideWindowEnd = baseSnapshot.guideWindowEnd,
                        hasMoreChannels = baseSnapshot.hasMoreChannels
                    )
                }
            }
        }
    }

    private fun restoreGuidePreferences() {
        viewModelScope.launch {
            preferencesRepository.guideDensity.first()
                ?.let { saved ->
                    GuideDensity.entries.firstOrNull { it.name == saved }?.let { density ->
                        selectedDensity.value = density
                    }
                }
            preferencesRepository.guideChannelMode.first()
                ?.let { saved ->
                    GuideChannelMode.entries.firstOrNull { it.name == saved }?.let { mode ->
                        selectedChannelMode.value = mode
                    }
                }
            if (fixedCategoryId.value == null) {
                startupCategoryId.value = preferencesRepository.guideDefaultCategoryId.first() ?: VirtualCategoryIds.FAVORITES
                showFavoritesOnly.value = preferencesRepository.guideFavoritesOnly.first()
                showScheduledOnly.value = preferencesRepository.guideScheduledOnly.first()
                preferencesRepository.guideAnchorTime.first()
                    ?.takeIf { it > 0L }
                    ?.let { guideAnchorTime.value = it }
            } else {
                startupCategoryId.value = null
                showFavoritesOnly.value = false
                showScheduledOnly.value = false
                selectedChannelMode.value = GuideChannelMode.ALL
            }
        }
    }

    private fun buildProviderSourceLabel(provider: com.afterglowtv.domain.model.Provider): String {
        return when (provider.type) {
            com.afterglowtv.domain.model.ProviderType.XTREAM_CODES -> "Xtream Codes"
            com.afterglowtv.domain.model.ProviderType.M3U -> "M3U Playlist"
            com.afterglowtv.domain.model.ProviderType.STALKER_PORTAL -> "Portal/MAG Login"
        }
    }

    private fun buildProviderArchiveSummary(provider: com.afterglowtv.domain.model.Provider): String {
        return when (provider.type) {
            com.afterglowtv.domain.model.ProviderType.XTREAM_CODES ->
                "Xtream replay depends on archive-enabled channels and valid replay stream ids from the provider."
            com.afterglowtv.domain.model.ProviderType.M3U ->
                if (provider.epgUrl.isBlank()) {
                    "M3U replay is limited: archive depends on provider templates and guide coverage is weaker without XMLTV."
                } else {
                    "M3U replay depends on the provider catch-up template and matching guide data."
                }
            com.afterglowtv.domain.model.ProviderType.STALKER_PORTAL ->
                if (provider.epgUrl.isBlank()) {
                    "Portal guide falls back to on-demand Stalker data when XMLTV is unavailable."
                } else {
                    "Guide combines optional XMLTV with on-demand Stalker portal data."
                }
        }
    }

    private fun updateGuideAnchorTime(anchorTimeMs: Long) {
        guideAnchorTime.value = anchorTimeMs
        viewModelScope.launch {
            preferencesRepository.setGuideAnchorTime(anchorTimeMs)
        }
    }

    private fun buildGuideCategoryList(
        providerCategories: List<Category>,
        customCategories: List<Category>,
        showAllChannels: Boolean = true
    ): List<Category> {
        val favoritesCategory = customCategories.find { it.id == VirtualCategoryIds.FAVORITES }
        val adultGuideCount = providerCategories
            .filter(::isLikelyAdultGuideCategory)
            .sumOf(Category::count)
        val standardProviderCategories = providerCategories.filterNot(::isLikelyAdultGuideCategory)
        return buildList {
            if (favoritesCategory != null) {
                add(favoritesCategory)
            }
            addAll(customCategories.filter { it.id != VirtualCategoryIds.FAVORITES })
            add(
                Category(
                    id = VirtualCategoryIds.ADULT_GUIDE,
                    name = "XXX Guide",
                    type = ContentType.LIVE,
                    isVirtual = true,
                    count = adultGuideCount,
                    isAdult = true
                )
            )
            if (showAllChannels) {
                add(
                    Category(
                        id = ChannelRepository.ALL_CHANNELS_ID,
                        name = "All Channels",
                        type = ContentType.LIVE,
                        count = standardProviderCategories.sumOf(Category::count)
                    )
                )
            }
            addAll(standardProviderCategories)
        }
    }

    private fun estimateGuideChannelCount(
        resolvedCategoryId: Long,
        categories: List<Category>
    ): Int {
        val selectedCount = categories.firstOrNull { it.id == resolvedCategoryId }?.count ?: 0
        if (selectedCount > 0) return selectedCount
        return categories.firstOrNull { it.id == ChannelRepository.ALL_CHANNELS_ID }?.count ?: 0
    }

    private fun loadGuideChannelsForProvider(
        providerId: Long,
        request: GuideBaseRequest
    ) = when (request.resolvedCategoryId) {
        ChannelRepository.ALL_CHANNELS_ID -> loadPreferredGuideChannels(
            providerId = providerId,
            categoryId = request.resolvedCategoryId,
            favoritesOnly = request.favoritesOnly
        )

        VirtualCategoryIds.FAVORITES -> favoriteRepository.getFavorites(providerId, ContentType.LIVE)
            .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
            .flatMapLatest { ids -> loadGuideChannelsByOrderedIds(ids, providerId) }

        VirtualCategoryIds.ADULT_GUIDE -> {
            val categoriesById = request.categories.associateBy(Category::id)
            channelRepository.getChannels(providerId)
                .map { channels ->
                    channels.filter { channel ->
                        channel.categoryId in request.adultCategoryIds ||
                            isAdultGuideChannel(channel, channel.categoryId?.let(categoriesById::get))
                    }
                }
        }

        in Long.MIN_VALUE..<0L -> favoriteRepository.getFavoritesByGroup(-request.resolvedCategoryId)
            .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
            .flatMapLatest { ids -> loadGuideChannelsByOrderedIds(ids, providerId) }

        else -> loadPreferredGuideChannels(
            providerId = providerId,
            categoryId = request.resolvedCategoryId,
            favoritesOnly = request.favoritesOnly
        )
    }

    private fun loadPreferredGuideChannels(
        providerId: Long,
        categoryId: Long,
        favoritesOnly: Boolean
    ): Flow<List<Channel>> = if (favoritesOnly) {
        favoriteRepository.getFavorites(providerId, ContentType.LIVE)
            .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
            .flatMapLatest { favoriteIds ->
                val favoriteChannels = loadGuideChannelsByOrderedIds(favoriteIds, providerId)
                if (categoryId == ChannelRepository.ALL_CHANNELS_ID) {
                    favoriteChannels
                } else {
                    combine(
                        favoriteChannels,
                        channelRepository.getChannelsByCategory(providerId, categoryId)
                    ) { orderedFavorites, categoryChannels ->
                        val categoryChannelIds = categoryChannels.map(Channel::id).toSet()
                        orderedFavorites.filter { it.id in categoryChannelIds }
                    }
                }
            }
    } else {
        combine(
            channelRepository.getChannelsByCategory(providerId, categoryId),
            channelRepository.getChannelsWithoutErrors(providerId, categoryId),
            favoriteRepository.getFavorites(providerId, ContentType.LIVE)
        ) { channelsByNumber, healthyChannels, _ ->
            val healthyIds = healthyChannels.map(Channel::id).toSet()
            when {
                channelsByNumber.isEmpty() -> healthyChannels
                healthyIds.isEmpty() -> channelsByNumber
                healthyChannels.size >= channelsByNumber.size -> healthyChannels
                else -> channelsByNumber.filter { channel ->
                    channel.errorCount <= 0 || channel.id !in healthyIds
                }.ifEmpty { channelsByNumber }
            }
        }
    }

    private fun GuideBaseRequest.isAdultGuideRequest(): Boolean =
        surface == GuideSurface.ADULT_GUIDE

    private fun Long.toGuideSurface(): GuideSurface =
        if (this == VirtualCategoryIds.ADULT_GUIDE) GuideSurface.ADULT_GUIDE else GuideSurface.STANDARD_EPG

    private fun Channel.isAdultForStandardGuide(request: GuideBaseRequest): Boolean =
        categoryId in request.adultCategoryIds || isExplicitAdultGuideChannel(this)

    private fun GuideBaseRequest.visibleInitialChannels(channels: List<Channel>): List<Channel> =
        if (isAdultGuideRequest()) channels.take(adultGuideRenderLimit) else channels

    private fun GuideBaseRequest.hasMoreChannels(
        visibleChannels: List<Channel>,
        allChannels: List<Channel>
    ): Boolean =
        isAdultGuideRequest() && visibleChannels.size < allChannels.size

    private fun GuideBaseRequest.shouldSkipEmptyStandardGuideScan(): Boolean {
        if (isAdultGuideRequest()) return false
        if (resolvedCategoryId != ChannelRepository.ALL_CHANNELS_ID) return false
        if (estimatedChannelCount != 0) return false
        if (adultCategoryIds.isEmpty()) return false
        return categories.none { category ->
            category.id > 0L &&
                category.id != ChannelRepository.ALL_CHANNELS_ID &&
                category.id != VirtualCategoryIds.ADULT_GUIDE &&
                category.id !in adultCategoryIds &&
                !category.isVirtual
        }
    }

    private fun combinedProviderIdsFlow(profileId: Long): kotlinx.coroutines.flow.Flow<List<Long>> = kotlinx.coroutines.flow.flow {
        emit(combinedM3uRepository.getProfile(profileId)?.members.orEmpty())
    }.map { members ->
        members.filter { it.enabled }.map { it.providerId }
    }

    private fun observeLiveFavorites(providerIds: List<Long>): kotlinx.coroutines.flow.Flow<List<Favorite>> = when (providerIds.size) {
        0 -> kotlinx.coroutines.flow.flowOf(emptyList())
        1 -> favoriteRepository.getFavorites(providerIds.first(), ContentType.LIVE)
        else -> favoriteRepository.getFavorites(providerIds, ContentType.LIVE)
    }

    private fun loadGuideChannelsByOrderedIds(ids: List<Long>, providerId: Long? = null) =
        if (ids.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            channelRepository.getChannelsByIds(ids).map { unsorted ->
                val filtered = providerId?.let { requiredProviderId ->
                    unsorted.filter { it.providerId == requiredProviderId }
                } ?: unsorted
                filtered.orderedByRequestedRawIds(ids)
            }
        }

    private fun finalizeStartupCategory(resolvedCategoryId: Long) {
        fixedCategoryId.value?.let { lockedCategoryId ->
            startupCategoryId.value = null
            selectedCategoryId.value = lockedCategoryId
            return
        }
        if (startupCategoryId.value == null) return
        startupCategoryId.value = null
        selectedCategoryId.value = resolvedCategoryId
    }

    private suspend fun loadGuidePrograms(
        providerId: Long,
        channels: List<Channel>,
        windowStart: Long,
        windowEnd: Long
    ): GuideProgramsResult {
        if (channels.isEmpty()) {
            return GuideProgramsResult(emptyMap(), failedCount = 0)
        }

        // 1. Try the multi-source resolved path first.
        val channelIds = channels.map { it.id }
        val resolvedPrograms: Map<String, List<Program>> = runCatching {
            epgRepository.getResolvedProgramsForChannels(providerId, channelIds, windowStart, windowEnd)
        }.getOrElse { emptyMap() }

        // 2. For channels not covered by resolution, fall back to legacy provider-native query.
        val unresolvedChannels = channels.filter { channel ->
            val key = channel.guideLookupKey()
            key == null || resolvedPrograms[key].isNullOrEmpty()
        }
        val legacyPrograms: Map<String, List<Program>> = if (unresolvedChannels.isNotEmpty()) {
            val fallbackGuideKeys = unresolvedChannels.mapNotNull(Channel::guideLookupKey).distinct()
            runCatching {
                if (fallbackGuideKeys.isEmpty()) emptyMap()
                else epgRepository.getProgramsForChannelsSnapshot(providerId, fallbackGuideKeys, windowStart, windowEnd)
            }.getOrElse { emptyMap() }
        } else {
            emptyMap()
        }

        val programsByChannel = resolvedPrograms + legacyPrograms
        return GuideProgramsResult(
            programsByChannel = programsByChannel,
            failedCount = countMissingGuideEntries(channels, programsByChannel)
        )
    }

    private suspend fun loadCombinedGuidePrograms(
        channels: List<Channel>,
        windowStart: Long,
        windowEnd: Long
    ): GuideProgramsResult {
        if (channels.isEmpty()) {
            return GuideProgramsResult(emptyMap(), failedCount = 0)
        }
        val groupedPrograms = channels.groupBy { it.providerId }
            .mapValues { (_, providerChannels) ->
                providerChannels
            }

        val mergedProgramsByChannel = buildMap<String, List<Program>> {
            groupedPrograms.forEach { (providerId, providerChannels) ->
                val result = loadGuidePrograms(
                    providerId = providerId,
                    channels = providerChannels,
                    windowStart = windowStart,
                    windowEnd = windowEnd
                )
                putAll(result.programsByChannel)
            }
        }
        val guideKeys = channels.mapNotNull(Channel::guideLookupKey).distinct()
        return GuideProgramsResult(
            programsByChannel = mergedProgramsByChannel,
            failedCount = countMissingGuideEntries(channels, mergedProgramsByChannel)
        )
    }

    private fun scheduleGuideFallbackEnrichment(
        snapshotContext: GuideFallbackContext,
        channels: List<Channel>,
        existingProgramsByChannel: Map<String, List<Program>>
    ) {
        guideFallbackJob?.cancel()
        if (channels.isEmpty()) {
            return
        }

        guideFallbackJob = viewModelScope.launch {
            val fallbackProgramsByChannel = buildMap {
                channels.groupBy(Channel::providerId).forEach { (providerId, providerChannels) ->
                    val provider = providerRepository.getProvider(providerId) ?: return@forEach
                    val providerPrograms = fetchXtreamGuideFallback(
                        provider = provider,
                        providerId = providerId,
                        channels = providerChannels,
                        existingProgramsByChannel = existingProgramsByChannel + this,
                        windowStart = snapshotContext.guideWindowStart,
                        windowEnd = snapshotContext.guideWindowEnd
                    )
                    putAll(providerPrograms)
                }
            }
            if (fallbackProgramsByChannel.isEmpty()) {
                return@launch
            }

            baseGuideSnapshot.update { currentSnapshot ->
                if (currentSnapshot == null || !currentSnapshot.matches(snapshotContext)) {
                    return@update currentSnapshot
                }

                val mergedProgramsByChannel = currentSnapshot.baseProgramsByChannel + fallbackProgramsByChannel
                val visibleChannels = currentSnapshot.visibleChannels
                val channelsWithSchedule = countChannelsWithSchedule(visibleChannels, mergedProgramsByChannel)
                val hasUpcomingData = hasUpcomingGuideData(mergedProgramsByChannel, currentSnapshot.guideWindowStart)

                currentSnapshot.copy(
                    baseProgramsByChannel = mergedProgramsByChannel,
                    failedScheduleCount = countMissingGuideEntries(visibleChannels, mergedProgramsByChannel),
                    lastUpdatedAt = System.currentTimeMillis(),
                    baseChannelsWithSchedule = channelsWithSchedule,
                    baseGuideStale = visibleChannels.isNotEmpty() && (channelsWithSchedule == 0 || !hasUpcomingData)
                )
            }
        }
    }

    private suspend fun fetchXtreamGuideFallback(
        provider: com.afterglowtv.domain.model.Provider,
        providerId: Long,
        channels: List<Channel>,
        existingProgramsByChannel: Map<String, List<Program>>,
        windowStart: Long,
        windowEnd: Long
    ): Map<String, List<Program>> {
        if (
            provider.type != com.afterglowtv.domain.model.ProviderType.XTREAM_CODES &&
            provider.type != com.afterglowtv.domain.model.ProviderType.STALKER_PORTAL
        ) {
            return emptyMap()
        }

        val missingChannels = channels.filter { channel ->
            val lookupKey = channel.guideLookupKey()
            lookupKey != null &&
                channel.streamId > 0L &&
                existingProgramsByChannel[lookupKey].isNullOrEmpty()
        }
        if (missingChannels.isEmpty()) {
            return emptyMap()
        }

        val fallbackChannels = missingChannels
        val programsByRequest = providerRepository.getProgramsForLiveStreams(
            providerId = providerId,
            requests = fallbackChannels.map { channel ->
                LiveStreamProgramRequest(
                    streamId = channel.streamId,
                    epgChannelId = channel.epgChannelId
                )
            },
            limit = MAX_XTREAM_GUIDE_FALLBACK_PROGRAMS
        )

        return fallbackChannels.mapNotNull { channel ->
            val programs = (programsByRequest[
                LiveStreamProgramRequest(
                    streamId = channel.streamId,
                    epgChannelId = channel.epgChannelId
                )
            ] as? com.afterglowtv.domain.model.Result.Success)?.data
                .orEmpty()
                .filter { program -> program.endTime > windowStart && program.startTime < windowEnd }
                .sortedBy { program -> program.startTime }
            val lookupKey = channel.guideLookupKey() ?: return@mapNotNull null
            if (programs.isEmpty()) null else lookupKey to programs
        }.toMap()
    }

    private fun GuideBaseSnapshot.matches(context: GuideFallbackContext): Boolean =
        providerId == context.providerId &&
            selectedCategoryId == context.selectedCategoryId &&
            guideAnchorTime == context.guideAnchorTime &&
            guideWindowStart == context.guideWindowStart &&
            guideWindowEnd == context.guideWindowEnd &&
            visibleChannels.map(Channel::id) == context.visibleChannelIds

    private fun countMissingGuideEntries(
        channels: List<Channel>,
        programsByChannel: Map<String, List<Program>>
    ): Int =
        channels.mapNotNull(Channel::guideLookupKey)
            .distinct()
            .count { lookupKey -> !programsByChannel[lookupKey].orEmpty().hasRealGuidePrograms() }

    private fun countChannelsWithSchedule(
        channels: List<Channel>,
        programsByChannel: Map<String, List<Program>>
    ): Int =
        channels.count { channel ->
            channel.guideLookupKey()
                ?.let { lookupKey -> programsByChannel[lookupKey].orEmpty().hasRealGuidePrograms() }
                ?: false
        }

    private fun hasUpcomingGuideData(
        programsByChannel: Map<String, List<Program>>,
        windowStart: Long
    ): Boolean =
        programsByChannel.values.any { programs ->
            programs.any { program -> !program.isPlaceholder && program.endTime > windowStart }
        }

    private suspend fun buildGuideDisplaySnapshot(
        baseSnapshot: GuideBaseSnapshot,
        searchQuery: String,
        scheduledOnly: Boolean,
        channelMode: GuideChannelMode
    ): GuideDisplaySnapshot {
        val normalizedQuery = searchQuery.trim()
        val (candidateChannels, candidateProgramsByChannel) = if (normalizedQuery.isBlank()) {
            baseSnapshot.visibleChannels to baseSnapshot.baseProgramsByChannel
        } else {
            buildSearchGuideSnapshot(baseSnapshot, normalizedQuery)
        }
        val displayChannels = candidateChannels.filter { channel ->
            val programs = channel.guideLookupKey()
                ?.let { lookupKey -> candidateProgramsByChannel[lookupKey].orEmpty() }
                .orEmpty()
            val matchesScheduled = !scheduledOnly || programs.hasRealGuidePrograms()
            val matchesMode = when (channelMode) {
                GuideChannelMode.ALL -> true
                GuideChannelMode.ANCHORED -> programs.any { program ->
                    baseSnapshot.guideAnchorTime in program.startTime until program.endTime
                }
                GuideChannelMode.ARCHIVE_READY -> programs.any { program ->
                    !program.isPlaceholder && channel.isArchivePlayable(program, baseSnapshot.guideAnchorTime)
                }
            }
            matchesScheduled && matchesMode
        }
        val channelsWithSchedule = candidateChannels.count { channel ->
            channel.guideLookupKey()
                ?.let { lookupKey -> candidateProgramsByChannel[lookupKey].orEmpty().hasRealGuidePrograms() }
                ?: false
        }
        val hasUpcomingData = candidateProgramsByChannel.values.any { programs ->
            programs.any { program -> !program.isPlaceholder && program.endTime > baseSnapshot.guideWindowStart }
        }

        return GuideDisplaySnapshot(
            channels = displayChannels,
            programsByChannel = candidateProgramsByChannel,
            totalChannelCount = if (baseSnapshot.selectedCategoryId == VirtualCategoryIds.ADULT_GUIDE) {
                baseSnapshot.allChannels.size
            } else {
                candidateChannels.size
            },
            channelsWithSchedule = channelsWithSchedule,
            isGuideStale = candidateChannels.isNotEmpty() && (channelsWithSchedule == 0 || !hasUpcomingData)
        )
    }

    private fun List<Program>.hasRealGuidePrograms(): Boolean =
        any { !it.isPlaceholder }

    private suspend fun buildSearchGuideSnapshot(
        baseSnapshot: GuideBaseSnapshot,
        searchQuery: String
    ): Pair<List<Channel>, Map<String, List<Program>>> {
        val scopedChannels = loadGuideSearchScopeChannels(baseSnapshot)
        val scopedChannelsByLookup = scopedChannels.mapNotNull { channel ->
            channel.guideLookupKey()?.let { lookupKey -> lookupKey to channel }
        }.toMap()

        val metadataMatchedLookupKeys = scopedChannels.filter { channel ->
            channel.matchesGuideMetadataSearch(searchQuery)
        }.mapNotNull(Channel::guideLookupKey).toSet()

        val repositoryMatchedPrograms = epgRepository.searchPrograms(
            providerId = baseSnapshot.providerId,
            query = searchQuery,
            startTime = baseSnapshot.guideWindowStart,
            endTime = baseSnapshot.guideWindowEnd,
            categoryId = baseSnapshot.selectedCategoryId.takeIf { it >= 0L }
        ).first().groupBy { it.channelId }

        val repositoryMatchedLookupKeys = repositoryMatchedPrograms.keys
        val matchedLookupKeys = buildList {
            scopedChannels.forEach { channel ->
                val lookupKey = channel.guideLookupKey() ?: return@forEach
                if (lookupKey in metadataMatchedLookupKeys || lookupKey in repositoryMatchedLookupKeys) {
                    add(lookupKey)
                }
            }
        }

        val missingMetadataLookupKeys = matchedLookupKeys.filter { lookupKey ->
            lookupKey in metadataMatchedLookupKeys && lookupKey !in baseSnapshot.baseProgramsByChannel
        }
        val missingMetadataPrograms = if (missingMetadataLookupKeys.isEmpty()) {
            emptyMap()
        } else {
            epgRepository.getProgramsForChannelsSnapshot(
                providerId = baseSnapshot.providerId,
                channelIds = missingMetadataLookupKeys,
                startTime = baseSnapshot.guideWindowStart,
                endTime = baseSnapshot.guideWindowEnd
            )
        }

        val matchedChannels = matchedLookupKeys.mapNotNull(scopedChannelsByLookup::get)
        val matchedPrograms = matchedLookupKeys.associateWith { lookupKey ->
            if (lookupKey in metadataMatchedLookupKeys) {
                baseSnapshot.baseProgramsByChannel[lookupKey]
                    ?: missingMetadataPrograms[lookupKey]
                    ?: emptyList()
            } else {
                repositoryMatchedPrograms[lookupKey].orEmpty()
            }
        }
        return matchedChannels to matchedPrograms
    }

    private suspend fun loadGuideSearchScopeChannels(baseSnapshot: GuideBaseSnapshot): List<Channel> {
        val rawChannels = when {
            baseSnapshot.showFavoritesOnly || baseSnapshot.selectedCategoryId < 0L -> baseSnapshot.allChannels
            baseSnapshot.selectedCategoryId == ChannelRepository.ALL_CHANNELS_ID ->
                channelRepository.getChannels(baseSnapshot.providerId).first()
            else -> channelRepository.getChannelsByCategory(baseSnapshot.providerId, baseSnapshot.selectedCategoryId).first()
        }
        if (baseSnapshot.selectedCategoryId != ChannelRepository.ALL_CHANNELS_ID) {
            return rawChannels.filterNot { channel ->
                channel.categoryId != null && channel.categoryId in baseSnapshot.hiddenCategoryIds
            }
        }

        val accessibleCategoryIds = baseSnapshot.categories.filter { category ->
            isGuideCategoryAccessible(
                category = category,
                parentalControlLevel = baseSnapshot.parentalControlLevel,
                unlockedCategoryIds = emptySet()
            )
        }.map(Category::id).toSet()

        return rawChannels.filterNot { channel ->
            channel.categoryId != null && channel.categoryId in baseSnapshot.hiddenCategoryIds
        }.filter { channel ->
            channel.categoryId == null || channel.categoryId in accessibleCategoryIds
        }
    }

    private fun Channel.matchesGuideMetadataSearch(searchQuery: String): Boolean {
        return name.contains(searchQuery, ignoreCase = true) ||
            categoryName?.contains(searchQuery, ignoreCase = true) == true
    }

    private fun resolveGuideCategorySelection(
        requestedCategoryId: Long,
        categories: List<Category>,
        parentalControlLevel: Int,
        unlockedCategoryIds: Set<Long>,
        fallbackFromEmptyFavorites: Boolean = false
    ): Long {
        val requestedExists = categories.any { it.id == requestedCategoryId }
        if (requestedCategoryId == ChannelRepository.ALL_CHANNELS_ID && requestedExists) {
            return ChannelRepository.ALL_CHANNELS_ID
        }

        val requestedCategory = categories.firstOrNull { it.id == requestedCategoryId }
        if (requestedCategory != null && isGuideCategoryAccessible(requestedCategory, parentalControlLevel, unlockedCategoryIds)) {
            if (fallbackFromEmptyFavorites && requestedCategory.id == VirtualCategoryIds.FAVORITES && requestedCategory.count <= 0) {
                return categories.find { it.id == ChannelRepository.ALL_CHANNELS_ID }?.id
                    ?: categories.firstOrNull {
                        !(it.id == VirtualCategoryIds.FAVORITES && it.count <= 0) &&
                            isGuideCategoryAccessible(it, parentalControlLevel, unlockedCategoryIds)
                    }?.id
                    ?: categories.firstOrNull()?.id
                    ?: ChannelRepository.ALL_CHANNELS_ID
            }
            return requestedCategory.id
        }

        return categories.firstOrNull { category ->
            if (fallbackFromEmptyFavorites && category.id == VirtualCategoryIds.FAVORITES && category.count <= 0) {
                false
            } else {
                isGuideCategoryAccessible(category, parentalControlLevel, unlockedCategoryIds)
            }
        }?.id ?: categories.firstOrNull()?.id ?: ChannelRepository.ALL_CHANNELS_ID
    }

    private fun isGuideCategoryAccessible(
        category: Category,
        parentalControlLevel: Int,
        unlockedCategoryIds: Set<Long>
    ): Boolean {
        if (!category.isUserProtected) return true
        return AdultContentVisibilityPolicy.showInAggregatedSurfaces(parentalControlLevel) ||
            unlockedCategoryIds.contains(category.id)
    }
}

private data class GuidePresentationState(
    val baseSnapshot: GuideBaseSnapshot?,
    val searchQuery: String,
    val scheduledOnly: Boolean,
    val channelMode: GuideChannelMode,
    val density: GuideDensity
)

private data class GuideSelectionSeed(
    val requestedCategoryId: Long,
    val anchorTime: Long,
    val favoritesOnly: Boolean,
    val isStartupSelection: Boolean
)

private data class GuideCategoryData(
    val providerCategories: List<Category>,
    val customCategories: List<Category>,
    val hiddenCategoryIds: Set<Long>,
    val sortMode: com.afterglowtv.domain.model.CategorySortMode,
    val showAllChannels: Boolean = true
)
