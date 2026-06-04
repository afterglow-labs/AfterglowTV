package com.afterglowtv.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.ui.model.orderedByRequestedRawIds
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.data.sync.SyncManager
import com.afterglowtv.app.update.AppUpdateInstaller
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Favorite
import com.afterglowtv.domain.model.PlaybackHistory
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderStatus
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.SyncState
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.ChannelRepository
import com.afterglowtv.domain.repository.CombinedM3uRepository
import com.afterglowtv.domain.repository.FavoriteRepository
import com.afterglowtv.domain.repository.PlaybackHistoryRepository
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.usecase.ContinueWatchingResult
import com.afterglowtv.domain.usecase.ContinueWatchingScope
import com.afterglowtv.domain.usecase.GetContinueWatching
import com.afterglowtv.domain.usecase.GetCustomCategories
import android.content.Context
import com.afterglowtv.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import com.afterglowtv.domain.util.AdultContentVisibilityPolicy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val providerRepository: ProviderRepository,
    private val combinedM3uRepository: CombinedM3uRepository,
    private val favoriteRepository: FavoriteRepository,
    private val channelRepository: ChannelRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getContinueWatching: GetContinueWatching,
    private val getCustomCategories: GetCustomCategories,
    private val syncManager: SyncManager,
    private val appUpdateInstaller: AppUpdateInstaller
) : ViewModel() {
    private companion object {
        const val FAVORITE_CHANNEL_LIMIT = 12
        const val RECENT_CHANNEL_LIMIT = 12
        const val CONTINUE_WATCHING_LIMIT = 12
        const val HOME_SHORTCUT_LIMIT = 4
        const val PROVIDER_SETTLE_DELAY_MS = 750L
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val startupDestination: StateFlow<StartupDestination> = preferencesRepository.startupDestination
        .map(StartupDestination::fromStorage)
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartupDestination.default)

    val developerModeEnabled: StateFlow<Boolean> = preferencesRepository.developerModeEnabled
        .map(StorePolicy::effectiveDeveloperModeEnabled)
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val remoteDpadChannelZapping: StateFlow<Boolean> = preferencesRepository.remoteDpadChannelZapping
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val remoteDpadInvertChannelZapping: StateFlow<Boolean> = preferencesRepository.remoteDpadInvertChannelZapping
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val remoteShowInfoOnZap: StateFlow<Boolean> = preferencesRepository.remoteShowInfoOnZap
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setStartupDestination(destination: StartupDestination) {
        viewModelScope.launch {
            preferencesRepository.setStartupDestination(destination.storageValue)
        }
    }

    fun setRemoteDpadChannelZapping(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRemoteDpadChannelZapping(enabled)
        }
    }

    fun setRemoteDpadInvertChannelZapping(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRemoteDpadInvertChannelZapping(enabled)
        }
    }

    fun setRemoteShowInfoOnZap(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRemoteShowInfoOnZap(enabled)
        }
    }

    init {
        viewModelScope.launch {
            combine(
                combinedM3uRepository.getActiveLiveSource(),
                providerRepository.getActiveProvider()
            ) { activeSource, activeProvider ->
                Pair(activeSource ?: activeProvider?.id?.let { ActiveLiveSource.ProviderSource(it) }, activeProvider)
            }
                .distinctUntilChanged { old, new ->
                    old.first == new.first && old.second?.id == new.second?.id
                }
                .flatMapLatest { (activeSource, activeProvider) ->
                    flow {
                        if (activeSource == null && activeProvider == null) {
                            delay(PROVIDER_SETTLE_DELAY_MS)
                            emit(DashboardUiState(isLoading = false))
                            return@flow
                        }

                        when (activeSource) {
                            is ActiveLiveSource.ProviderSource -> {
                                val provider = activeProvider?.takeIf { it.id == activeSource.providerId }
                                    ?: providerRepository.getProvider(activeSource.providerId)
                                    ?: run {
                                        emit(DashboardUiState(isLoading = false))
                                        return@flow
                                    }
                                emitAll(observeDashboard(provider, listOf(provider.id), combinedProfileId = null))
                            }

                            is ActiveLiveSource.CombinedM3uSource -> {
                                val liveProviderIds = combinedM3uRepository.getProfile(activeSource.profileId)
                                    ?.members
                                    .orEmpty()
                                    .filter { it.enabled }
                                    .map { it.providerId }
                                    .distinct()
                                val provider = activeProvider?.takeIf { it.id in liveProviderIds }
                                    ?: liveProviderIds.firstOrNull()?.let { providerRepository.getProvider(it) }
                                    ?: run {
                                        emit(DashboardUiState(isLoading = false, currentCombinedProfileId = activeSource.profileId))
                                        return@flow
                                    }
                                emitAll(observeDashboard(provider, liveProviderIds, combinedProfileId = activeSource.profileId))
                            }

                            null -> emit(DashboardUiState(isLoading = false))
                        }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun observeDashboard(
        provider: Provider,
        liveProviderIds: List<Long>,
        combinedProfileId: Long?
    ): Flow<DashboardUiState> {
        val contentShelves = flowOf(
            DashboardContentShelves(
                favoriteChannels = emptyList(),
                recentChannels = emptyList(),
                continueWatching = emptyList()
            )
        )
        val liveContext = flowOf(DashboardLiveContext(lastVisitedCategory = null, shortcuts = emptyList()))

        return combine(
            contentShelves,
            liveContext,
            observeLiveChannelCount(liveProviderIds).onStart { emit(0) }
        ) { shelves, liveContext, liveChannelCount ->
            DashboardSnapshot(
                shelves = shelves,
                liveContext = liveContext,
                liveChannelCount = liveChannelCount,
                updateNotice = null
            )
        }.combine(observeUpdateNotice().onStart { emit(null) }) { snapshot, updateNotice ->
            snapshot.copy(updateNotice = updateNotice)
        }.combine(syncManager.syncStateForProvider(provider.id).onStart { emit(SyncState.Idle) }) { snapshot, syncState ->
            val providerDisplayName = provider.name
            DashboardUiState(
                provider = provider,
                showProviderChrome = true,
                favoriteChannels = snapshot.shelves.favoriteChannels,
                recentChannels = snapshot.shelves.recentChannels,
                continueWatching = snapshot.shelves.continueWatching,
                continueWatchingDegraded = snapshot.shelves.continueWatchingDegraded,
                lastLiveCategory = snapshot.liveContext.lastVisitedCategory,
                liveShortcuts = snapshot.liveContext.shortcuts,
                currentCombinedProfileId = combinedProfileId,
                stats = DashboardStats(
                    liveChannelCount = snapshot.liveChannelCount,
                    favoriteChannelCount = snapshot.shelves.favoriteChannels.size,
                    recentChannelCount = snapshot.shelves.recentChannels.size,
                    continueWatchingCount = snapshot.shelves.continueWatching.size
                ),
                feature = buildFeature(
                    providerName = providerDisplayName,
                    recentChannels = snapshot.shelves.recentChannels,
                    continueWatching = snapshot.shelves.continueWatching,
                    continueWatchingDegraded = snapshot.shelves.continueWatchingDegraded
                ),
                providerHealth = provider.let {
                    DashboardProviderHealth(
                        status = it.status,
                        type = it.type,
                        lastSyncedAt = it.lastSyncedAt,
                        expirationDate = it.expirationDate,
                        maxConnections = it.maxConnections
                    )
                } ?: DashboardProviderHealth(),
                providerWarnings = when (syncState) {
                    is SyncState.Partial -> syncState.warnings
                    is SyncState.Error -> listOf(syncState.message)
                    else -> emptyList()
                },
                updateNotice = snapshot.updateNotice,
                isLoading = false
            )
        }
    }

    private fun observeFavoriteChannels(providerIds: List<Long>): Flow<List<Channel>> =
        observeLiveFavorites(providerIds)
            .map { favorites ->
                favorites
                    .filter { it.groupId == null }
                    .sortedBy { it.position }
                    .map { it.contentId }
                    .take(FAVORITE_CHANNEL_LIMIT)
            }
            .flatMapLatest(::loadChannelsByOrderedIds)

    private fun observeRecentChannels(providerIds: List<Long>): Flow<List<Channel>> =
        combine(
            preferencesRepository.showRecentChannelsCategory.flatMapLatest { show ->
                if (!show) flowOf(emptyList())
                else observeRecentLiveIds(providerIds, RECENT_CHANNEL_LIMIT)
                    .flatMapLatest(::loadChannelsByOrderedIds)
            },
            preferencesRepository.parentalControlLevel
        ) { channels, level ->
            AdultContentVisibilityPolicy.filterForAggregatedSurface(
                channels, level
            ) { isUserProtected }
        }

    private fun observeContinueWatching(providerIds: Set<Long>): Flow<ContinueWatchingShelf> =
        getContinueWatching(
            providerIds = providerIds,
            limit = CONTINUE_WATCHING_LIMIT,
            scope = ContinueWatchingScope.ALL_VOD
        ).map { result ->
            when (result) {
                is ContinueWatchingResult.Items -> ContinueWatchingShelf(items = result.items)
                ContinueWatchingResult.Degraded -> ContinueWatchingShelf(isDegraded = true)
            }
        }

    private fun buildLiveContext(
        providerIds: List<Long>,
        lastVisitedProviderId: Long?
    ): Flow<DashboardLiveContext> {
        if (providerIds.isEmpty()) {
            return flowOf(DashboardLiveContext(lastVisitedCategory = null, shortcuts = emptyList()))
        }

        return combine(
            getCustomCategories(providerIds, ContentType.LIVE),
            lastVisitedProviderId?.let(preferencesRepository::getLastLiveCategoryId) ?: flowOf(null),
            preferencesRepository.promotedLiveGroupIds
        ) { customCategories, lastVisitedCategoryId, promotedGroupIds ->
            val lastVisitedCategory = customCategories.firstOrNull { it.id == lastVisitedCategoryId }
            val shortcuts = buildList {
                lastVisitedCategory?.let {
                    add(
                        DashboardLiveShortcut(
                            label = appContext.getString(R.string.home_last_group),
                            detail = it.name,
                            categoryId = it.id,
                            type = DashboardShortcutType.LAST_GROUP
                        )
                    )
                }

                customCategories
                    .asSequence()
                    .filter { it.id != VirtualCategoryIds.FAVORITES }
                    .filter { it.count > 0 }
                    .sortedWith(
                        compareByDescending<Category> { category ->
                            val groupId = if (category.id < 0) -category.id else category.id
                            groupId in promotedGroupIds
                        }.thenByDescending { it.count }
                    )
                    .take(HOME_SHORTCUT_LIMIT)
                    .forEach { category ->
                        val groupId = if (category.id < 0) -category.id else category.id
                        add(
                            DashboardLiveShortcut(
                                label = category.name,
                                detail = if (groupId in promotedGroupIds) {
                                    appContext.getString(R.string.dashboard_pinned_channels_format, category.count)
                                } else {
                                    appContext.getString(R.string.dashboard_channels_format, category.count)
                                },
                                categoryId = category.id,
                                type = DashboardShortcutType.CUSTOM_GROUP
                            )
                        )
                    }
            }
                .distinctBy { it.categoryId ?: it.label }
                .take(HOME_SHORTCUT_LIMIT)

            DashboardLiveContext(
                lastVisitedCategory = lastVisitedCategory,
                shortcuts = shortcuts
            )
        }
    }

    private fun observeLiveChannelCount(providerIds: List<Long>): Flow<Int> = when (providerIds.size) {
        0 -> flowOf(0)
        1 -> channelRepository.getChannelCount(providerIds.first())
        else -> combine(providerIds.map { providerId ->
            channelRepository.getChannelCount(providerId)
        }) { counts ->
            counts.sum()
        }
    }

    private fun observeLiveFavorites(providerIds: List<Long>): Flow<List<Favorite>> = when (providerIds.size) {
        0 -> flowOf(emptyList())
        1 -> favoriteRepository.getFavorites(providerIds.first(), ContentType.LIVE)
        else -> favoriteRepository.getFavorites(providerIds, ContentType.LIVE)
    }

    private fun observeRecentLiveIds(providerIds: List<Long>, limit: Int): Flow<List<Long>> = when (providerIds.size) {
        0 -> flowOf(emptyList())
        1 -> playbackHistoryRepository.getRecentlyWatchedByProvider(providerIds.first(), limit)
            .map { history ->
                history
                    .filter { it.contentType == ContentType.LIVE }
                    .sortedByDescending { it.lastWatchedAt }
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .take(limit)
            }
        else -> combine(providerIds.map { providerId ->
            playbackHistoryRepository.getRecentlyWatchedByProvider(providerId, limit)
        }) { histories ->
            histories.toList()
                .flatMap { it }
                .asSequence()
                .filter { it.contentType == ContentType.LIVE }
                .sortedByDescending { it.lastWatchedAt }
                .distinctBy { it.providerId to it.contentId }
                .map { it.contentId }
                .take(limit)
                .toList()
        }
    }

    private fun loadChannelsByOrderedIds(ids: List<Long>): Flow<List<Channel>> {
        if (ids.isEmpty()) return flowOf(emptyList())

        return channelRepository.getChannelsByIds(ids).map { channels ->
            channels.orderedByRequestedRawIds(ids)
        }
    }

    private fun observeUpdateNotice(): Flow<DashboardUpdateNotice?> = combine(
        preferencesRepository.cachedAppUpdateVersionName,
        preferencesRepository.cachedAppUpdateVersionCode,
        preferencesRepository.downloadedAppUpdateVersionName
    ) { latestVersionName, latestVersionCode, downloadedVersionName ->
        if (latestVersionName.isNullOrBlank()) {
            return@combine null
        }

        val updateAvailable = if (latestVersionCode != null && latestVersionCode > BuildConfig.VERSION_CODE) {
            true
        } else {
            compareVersionNames(latestVersionName, BuildConfig.VERSION_NAME) > 0
        }

        if (!updateAvailable) {
            return@combine null
        }

        DashboardUpdateNotice(
            latestVersionName = latestVersionName,
            installReady = downloadedVersionName == latestVersionName
        )
    }

    private fun buildFeature(
        providerName: String,
        recentChannels: List<Channel>,
        continueWatching: List<PlaybackHistory>,
        continueWatchingDegraded: Boolean
    ): DashboardFeature {
        // When continue-watching is empty due to a transient IO failure, do not silently
        // fall through to an unrelated hero — surface an explicit degraded state instead.
        if (continueWatching.isEmpty() && continueWatchingDegraded) {
            return DashboardFeature(
                title = appContext.getString(R.string.dashboard_resume_unavailable),
                summary = appContext.getString(R.string.dashboard_resume_unavailable_detail),
                artworkUrl = null,
                actionLabel = appContext.getString(R.string.dashboard_continue_watching),
                actionType = DashboardFeatureAction.CONTINUE_WATCHING
            )
        }
        val resumeItem = continueWatching.firstOrNull()
        if (resumeItem != null) {
            val detail = when (resumeItem.contentType) {
                ContentType.MOVIE -> appContext.getString(R.string.dashboard_resume_movie)
                ContentType.SERIES -> appContext.getString(R.string.dashboard_resume_series)
                ContentType.SERIES_EPISODE -> {
                    if (resumeItem.seasonNumber != null && resumeItem.episodeNumber != null) {
                        appContext.getString(R.string.dashboard_resume_episode_format, resumeItem.seasonNumber, resumeItem.episodeNumber)
                    } else {
                        appContext.getString(R.string.dashboard_resume_episode)
                    }
                }
                ContentType.LIVE -> appContext.getString(R.string.dashboard_resume_live)
            }
            return DashboardFeature(
                title = resumeItem.title,
                summary = detail,
                artworkUrl = resumeItem.posterUrl,
                actionLabel = appContext.getString(R.string.dashboard_continue_watching),
                actionType = DashboardFeatureAction.CONTINUE_WATCHING
            )
        }

        recentChannels.firstOrNull()?.let { channel ->
            return DashboardFeature(
                title = channel.name,
                summary = appContext.getString(R.string.dashboard_jump_back_live),
                artworkUrl = channel.logoUrl,
                actionLabel = appContext.getString(R.string.dashboard_watch_live),
                actionType = DashboardFeatureAction.LIVE
            )
        }

        return DashboardFeature(
            title = providerName,
            summary = appContext.getString(R.string.dashboard_library_ready),
            artworkUrl = null,
            actionLabel = appContext.getString(R.string.dashboard_open_live_tv),
            actionType = DashboardFeatureAction.LIVE
        )
    }

    private fun compareVersionNames(left: String, right: String): Int {
        val leftParts = left.removePrefix("v").split('.')
        val rightParts = right.removePrefix("v").split('.')
        val length = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until length) {
            val leftValue = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
            val rightValue = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
            if (leftValue != rightValue) {
                return leftValue.compareTo(rightValue)
            }
        }
        return 0
    }

    fun installDownloadedUpdate() {
        viewModelScope.launch {
            when (val result = appUpdateInstaller.installDownloadedUpdate()) {
                is com.afterglowtv.domain.model.Result.Error -> {
                    _uiState.value = _uiState.value.copy(userMessage = result.message)
                }
                is com.afterglowtv.domain.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        userMessage = appContext.getString(R.string.settings_update_install_started)
                    )
                }
                else -> Unit
            }
        }
    }

    fun userMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}

private data class DashboardLiveContext(
    val lastVisitedCategory: Category?,
    val shortcuts: List<DashboardLiveShortcut>
)

private data class ContinueWatchingShelf(
    val items: List<PlaybackHistory> = emptyList(),
    val isDegraded: Boolean = false
)

private data class DashboardContentShelves(
    val favoriteChannels: List<Channel>,
    val recentChannels: List<Channel>,
    val continueWatching: List<PlaybackHistory>,
    val continueWatchingDegraded: Boolean = false
)

private data class DashboardSnapshot(
    val shelves: DashboardContentShelves,
    val liveContext: DashboardLiveContext,
    val liveChannelCount: Int,
    val updateNotice: DashboardUpdateNotice?
)

data class DashboardUiState(
    val provider: Provider? = null,
    val showProviderChrome: Boolean = true,
    val favoriteChannels: List<Channel> = emptyList(),
    val recentChannels: List<Channel> = emptyList(),
    val continueWatching: List<PlaybackHistory> = emptyList(),
    val continueWatchingDegraded: Boolean = false,
    val lastLiveCategory: Category? = null,
    val liveShortcuts: List<DashboardLiveShortcut> = emptyList(),
    val feature: DashboardFeature = DashboardFeature(),
    val providerHealth: DashboardProviderHealth = DashboardProviderHealth(),
    val providerWarnings: List<String> = emptyList(),
    val currentCombinedProfileId: Long? = null,
    val updateNotice: DashboardUpdateNotice? = null,
    val stats: DashboardStats = DashboardStats(),
    val userMessage: String? = null,
    val isLoading: Boolean = true
)

data class DashboardUpdateNotice(
    val latestVersionName: String,
    val installReady: Boolean
)

data class DashboardProviderHealth(
    val status: ProviderStatus = ProviderStatus.UNKNOWN,
    val type: ProviderType = ProviderType.M3U,
    val lastSyncedAt: Long = 0L,
    val expirationDate: Long? = null,
    val maxConnections: Int = 1
)

data class DashboardStats(
    val liveChannelCount: Int = 0,
    val favoriteChannelCount: Int = 0,
    val recentChannelCount: Int = 0,
    val continueWatchingCount: Int = 0
)

data class DashboardFeature(
    val title: String = "",
    val summary: String = "",
    val artworkUrl: String? = null,
    val actionLabel: String = "",
    val actionType: DashboardFeatureAction = DashboardFeatureAction.LIVE
)

enum class DashboardFeatureAction {
    LIVE,
    CONTINUE_WATCHING
}

data class DashboardLiveShortcut(
    val label: String,
    val detail: String,
    val categoryId: Long?,
    val type: DashboardShortcutType
)

enum class DashboardShortcutType {
    FAVORITES,
    RECENT,
    LAST_GROUP,
    CUSTOM_GROUP
}
