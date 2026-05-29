package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.tv.LauncherRecommendationsManager
import com.afterglowtv.app.tv.WatchNextManager
import com.afterglowtv.app.tvinput.TvInputChannelSyncManager
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.BuiltInPlaylists
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.SyncMetadata
import com.afterglowtv.domain.repository.CombinedM3uRepository
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.repository.SyncMetadataRepository
import com.afterglowtv.domain.usecase.SyncProvider
import com.afterglowtv.domain.usecase.SyncProviderCommand
import com.afterglowtv.domain.usecase.SyncProviderResult
import com.afterglowtv.data.sync.SyncManager
import com.afterglowtv.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

internal class SettingsProviderActions(
    private val providerRepository: ProviderRepository,
    private val combinedM3uRepository: CombinedM3uRepository,
    private val preferencesRepository: PreferencesRepository,
    private val syncProvider: SyncProvider,
    private val syncManager: SyncManager,
    private val syncMetadataRepository: SyncMetadataRepository,
    private val watchNextManager: WatchNextManager,
    private val launcherRecommendationsManager: LauncherRecommendationsManager,
    private val tvInputChannelSyncManager: TvInputChannelSyncManager,
    private val uiState: MutableStateFlow<SettingsUiState>
) {
    private companion object {
        val AUTO_SWITCH_SYNC_STALE_AFTER_MS = 24.hours.inWholeMilliseconds
    }

    fun setActiveProvider(scope: CoroutineScope, providerId: Long) {
        scope.launch {
            // Validate the provider exists before writing any preferences.
            val provider = providerRepository.getProvider(providerId)
            if (provider == null) {
                uiState.update { it.copy(userMessage = "Could not activate provider: provider not found") }
                return@launch
            }
            // Write to the repository first; only persist UI-layer preferences on success.
            when (val result = providerRepository.setActiveProvider(providerId)) {
                is Result.Error -> {
                    uiState.update { it.copy(userMessage = "Could not activate provider: ${result.message}") }
                    return@launch
                }
                else -> Unit
            }
            // Repository write succeeded – now persist the UI-layer preferences and refresh.
            preferencesRepository.setLastActiveProviderId(providerId)
            combinedM3uRepository.setActiveLiveSource(ActiveLiveSource.ProviderSource(providerId))
            watchNextManager.refreshWatchNext()
            launcherRecommendationsManager.refreshRecommendations(force = true)
            tvInputChannelSyncManager.refreshTvInputCatalog()
            val lastSyncedAt = provider.lastSyncedAt
            val shouldAutoSync = lastSyncedAt <= 0L ||
                System.currentTimeMillis() - lastSyncedAt >= AUTO_SWITCH_SYNC_STALE_AFTER_MS
            if (shouldAutoSync) {
                refreshProvider(
                    scope = scope,
                    providerId = providerId,
                    syncMode = SettingsProviderSyncMode.SYNC_NOW,
                    progressPrefix = "Refreshing ${provider.name}..."
                )
            } else {
                uiState.update { it.copy(userMessage = "Connected to ${provider.name}") }
            }
        }
    }

    fun setActiveCombinedProfile(scope: CoroutineScope, profileId: Long) {
        scope.launch {
            // Validate the profile exists and has at least one enabled member before activating.
            val profile = combinedM3uRepository.getProfile(profileId)
            when {
                profile == null ->
                    uiState.update { it.copy(userMessage = "Could not activate combined source: profile not found") }
                profile.members.isEmpty() ->
                    uiState.update { it.copy(userMessage = "Add at least one playlist to this combined source before activating it") }
                profile.members.none { it.enabled } ->
                    uiState.update { it.copy(userMessage = "Enable at least one playlist in this combined source before activating it") }
                else -> when (combinedM3uRepository.setActiveLiveSource(ActiveLiveSource.CombinedM3uSource(profileId))) {
                    is Result.Success -> {
                        launcherRecommendationsManager.refreshRecommendations(force = true)
                        uiState.update { it.copy(userMessage = "Combined M3U source activated") }
                    }
                    is Result.Error -> uiState.update { it.copy(userMessage = "Could not activate combined source") }
                    Result.Loading -> Unit
                }
            }
        }
    }

    fun createCombinedProfile(scope: CoroutineScope, name: String, providerIds: List<Long>, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        scope.launch {
            when (val result = combinedM3uRepository.createProfile(name, providerIds)) {
                is Result.Success -> {
                    combinedM3uRepository.setActiveLiveSource(ActiveLiveSource.CombinedM3uSource(result.data.id))
                    uiState.update { it.copy(userMessage = "Combined M3U source created") }
                    onSuccess()
                }
                is Result.Error -> {
                    uiState.update { it.copy(userMessage = result.message) }
                    onError()
                }
                Result.Loading -> Unit
            }
        }
    }

    fun deleteCombinedProfile(scope: CoroutineScope, profileId: Long) {
        scope.launch {
            when (val result = combinedM3uRepository.deleteProfile(profileId)) {
                is Result.Success -> uiState.update { it.copy(userMessage = "Combined M3U source deleted") }
                is Result.Error -> uiState.update { it.copy(userMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun addProviderToCombinedProfile(scope: CoroutineScope, profileId: Long, providerId: Long, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        scope.launch {
            when (val result = combinedM3uRepository.addProvider(profileId, providerId)) {
                is Result.Success -> {
                    uiState.update { it.copy(userMessage = "Playlist added to combined source") }
                    onSuccess()
                }
                is Result.Error -> {
                    uiState.update { it.copy(userMessage = result.message) }
                    onError()
                }
                Result.Loading -> Unit
            }
        }
    }

    fun renameCombinedProfile(scope: CoroutineScope, profileId: Long, name: String, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        scope.launch {
            when (val result = combinedM3uRepository.updateProfileName(profileId, name)) {
                is Result.Success -> {
                    uiState.update { it.copy(userMessage = "Combined M3U source renamed") }
                    onSuccess()
                }
                is Result.Error -> {
                    uiState.update { it.copy(userMessage = result.message) }
                    onError()
                }
                Result.Loading -> Unit
            }
        }
    }

    fun removeProviderFromCombinedProfile(scope: CoroutineScope, profileId: Long, providerId: Long) {
        scope.launch {
            val profile = uiState.value.combinedProfiles.firstOrNull { it.id == profileId } ?: return@launch
            val activeSource = uiState.value.activeLiveSource
            if (activeSource is ActiveLiveSource.CombinedM3uSource && activeSource.profileId == profileId) {
                val memberToRemove = profile.members.firstOrNull { it.providerId == providerId }
                val removingEnabledMember = memberToRemove?.enabled == true
                val remainingEnabled = profile.members.count { it.providerId != providerId && it.enabled }
                if (removingEnabledMember && remainingEnabled == 0) {
                    uiState.update { it.copy(userMessage = "Cannot remove the last active playlist from the current live source") }
                    return@launch
                }
            }
            when (val result = combinedM3uRepository.removeProvider(profileId, providerId)) {
                is Result.Success -> uiState.update { it.copy(userMessage = "Playlist removed from combined source") }
                is Result.Error -> uiState.update { it.copy(userMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun moveCombinedProvider(scope: CoroutineScope, profileId: Long, providerId: Long, moveUp: Boolean) {
        scope.launch {
            val profile = uiState.value.combinedProfiles.firstOrNull { it.id == profileId } ?: return@launch
            val orderedProviderIds = profile.members.sortedBy { it.priority }.map { it.providerId }.toMutableList()
            val currentIndex = orderedProviderIds.indexOf(providerId)
            if (currentIndex == -1) return@launch
            val targetIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
            if (targetIndex !in orderedProviderIds.indices) return@launch
            java.util.Collections.swap(orderedProviderIds, currentIndex, targetIndex)
            when (val result = combinedM3uRepository.reorderMembers(profileId, orderedProviderIds)) {
                is Result.Success -> uiState.update { it.copy(userMessage = "Combined playlist order updated") }
                is Result.Error -> uiState.update { it.copy(userMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun setCombinedProviderEnabled(scope: CoroutineScope, profileId: Long, providerId: Long, enabled: Boolean) {
        scope.launch {
            if (!enabled) {
                val profile = uiState.value.combinedProfiles.firstOrNull { it.id == profileId }
                val activeSource = uiState.value.activeLiveSource
                if (profile != null &&
                    activeSource is ActiveLiveSource.CombinedM3uSource &&
                    activeSource.profileId == profileId
                ) {
                    val remainingEnabled = profile.members.count { it.providerId != providerId && it.enabled }
                    if (remainingEnabled == 0) {
                        uiState.update { it.copy(userMessage = "Cannot disable the last active playlist in the current live source") }
                        return@launch
                    }
                }
            }
            when (val result = combinedM3uRepository.setMemberEnabled(profileId, providerId, enabled)) {
                is Result.Success -> uiState.update {
                    it.copy(userMessage = if (enabled) "Playlist enabled in combined source" else "Playlist disabled in combined source")
                }
                is Result.Error -> uiState.update { it.copy(userMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun setBuiltInPlaylistsLoaded(scope: CoroutineScope, loaded: Boolean) {
        scope.launch {
            preferencesRepository.setShowBuiltInPlaylists(loaded)
            if (!loaded) {
                uiState.update {
                    it.copy(userMessage = "Free broadcasts unloaded")
                }
                return@launch
            }

            val existing = providerRepository.getProviders()
                .first()
                .firstOrNull(BuiltInPlaylists::isBuiltInProvider)
            if (existing != null) {
                uiState.update {
                    it.copy(userMessage = "Free broadcasts loaded")
                }
                return@launch
            }

            uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgress = "Scanning for legal and free broadcasts...",
                    syncingProviderName = BuiltInPlaylists.FREE_BROADCASTS_NAME
                )
            }
            when (val result = providerRepository.validateM3u(
                url = BuiltInPlaylists.FREE_BROADCASTS_URL,
                name = BuiltInPlaylists.FREE_BROADCASTS_NAME,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                onProgress = { progress ->
                    uiState.update { state ->
                        state.copy(
                            syncProgress = progress.ifBlank { "Scanning for legal and free broadcasts..." },
                            syncingProviderName = BuiltInPlaylists.FREE_BROADCASTS_NAME
                        )
                    }
                }
            )) {
                is Result.Success -> uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgress = null,
                        syncingProviderName = null,
                        userMessage = "Free broadcasts loaded"
                    )
                }
                is Result.Error -> {
                    preferencesRepository.setShowBuiltInPlaylists(false)
                    uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgress = null,
                            syncingProviderName = null,
                            userMessage = "Could not load free broadcasts: ${result.message}"
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun setM3uVodClassificationEnabled(scope: CoroutineScope, providerId: Long, enabled: Boolean) {
        scope.launch {
            val provider = providerRepository.getProvider(providerId) ?: return@launch
            if (provider.type != ProviderType.M3U) return@launch
            when (val result = providerRepository.updateProvider(provider.copy(m3uVodClassificationEnabled = enabled))) {
                is Result.Error -> uiState.update { it.copy(userMessage = "Could not save provider setting: ${result.message}") }
                else -> uiState.update {
                    it.copy(
                        userMessage = if (enabled) {
                            "M3U VOD classification enabled. Refresh the playlist to reclassify content."
                        } else {
                            "M3U VOD classification disabled. Refresh the playlist to reclassify content."
                        }
                    )
                }
            }
        }
    }

    fun refreshProvider(
        scope: CoroutineScope,
        providerId: Long,
        syncMode: SettingsProviderSyncMode = SettingsProviderSyncMode.SYNC_NOW,
        progressPrefix: String? = null
    ) {
        scope.launch {
            val providerName = providerRepository.getProvider(providerId)?.name
            uiState.update {
                it.copy(
                    isSyncing = true,
                    syncingProviderName = providerName,
                    syncProgress = progressPrefix ?: "Preparing sync..."
                )
            }
            try {
                when (syncMode) {
                    SettingsProviderSyncMode.SYNC_NOW -> runSyncNow(providerId, providerName)
                    SettingsProviderSyncMode.REBUILD_INDEX -> runRebuildIndex(providerId, providerName)
                }
            } catch (e: Exception) {
                uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgress = null,
                        syncingProviderName = null,
                        userMessage = "Sync failed: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun runSyncNow(providerId: Long, providerName: String?) {
        val provider = providerRepository.getProvider(providerId)
        val pendingXtreamTextRefreshGeneration = provider
            ?.takeIf { it.type == ProviderType.XTREAM_CODES }
            ?.let { currentProvider ->
                val currentGeneration = preferencesRepository.getXtreamTextImportGeneration()
                val appliedGeneration = preferencesRepository.getXtreamTextImportAppliedGeneration(currentProvider.id)
                currentGeneration.takeIf { it > appliedGeneration }
            }
        val beforeMetadata = syncMetadataRepository.getMetadata(providerId) ?: SyncMetadata(providerId)
        val result = syncProvider(
            SyncProviderCommand(
                providerId = providerId,
                force = pendingXtreamTextRefreshGeneration != null,
                movieFastSyncOverride = null,
                epgSyncModeOverride = ProviderEpgSyncMode.BACKGROUND
            ),
            onProgress = { message ->
                uiState.update { state ->
                    state.copy(
                        syncProgress = mapSyncNowProgress(message),
                        syncingProviderName = providerName
                    )
                }
            }
        )

        if (result !is SyncProviderResult.Error) {
            pendingXtreamTextRefreshGeneration?.let { generation ->
                preferencesRepository.markXtreamTextImportApplied(providerId, generation)
            }
            val afterMetadata = syncMetadataRepository.getMetadata(providerId) ?: beforeMetadata
            val liveRefreshed = afterMetadata.lastLiveSync > beforeMetadata.lastLiveSync ||
                afterMetadata.liveCount != beforeMetadata.liveCount
            val catalogRefreshed = liveRefreshed ||
                afterMetadata.lastMovieSync > beforeMetadata.lastMovieSync ||
                afterMetadata.lastSeriesSync > beforeMetadata.lastSeriesSync

            if (liveRefreshed) {
                uiState.update { state ->
                    state.copy(
                        syncProgress = "Updating TV integration...",
                        syncingProviderName = providerName
                    )
                }
                tvInputChannelSyncManager.refreshTvInputCatalog()
            } else if (!catalogRefreshed) {
                uiState.update { state ->
                    state.copy(
                        syncProgress = "Library already up to date.",
                        syncingProviderName = providerName
                    )
                }
            }

            uiState.update { state ->
                state.copy(
                    syncProgress = "Scheduling EPG refresh...",
                    syncingProviderName = providerName
                )
            }
            syncManager.scheduleBackgroundEpgSync(providerId)
        }

        uiState.update { state ->
            val partialWarnings = (result as? SyncProviderResult.Success)?.warnings.orEmpty()
            val warningsMessage = partialWarnings.take(3).joinToString(separator = ", ").ifBlank { "Some sections are incomplete." }
            val afterMetadata = syncMetadataRepository.getMetadata(providerId) ?: beforeMetadata
            val catalogRefreshed = afterMetadata.lastLiveSync > beforeMetadata.lastLiveSync ||
                afterMetadata.lastMovieSync > beforeMetadata.lastMovieSync ||
                afterMetadata.lastSeriesSync > beforeMetadata.lastSeriesSync ||
                afterMetadata.liveCount != beforeMetadata.liveCount
            state.copy(
                isSyncing = false,
                syncProgress = null,
                syncingProviderName = null,
                userMessage = when {
                    result is SyncProviderResult.Error -> "Sync failed: ${result.message}"
                    (result as? SyncProviderResult.Success)?.isPartial == true -> "Sync completed with warnings: $warningsMessage"
                    pendingXtreamTextRefreshGeneration != null -> "Sync completed and reapplied Xtream text decoding"
                    !catalogRefreshed -> "Library already up to date"
                    else -> "Sync completed"
                },
                syncWarningsByProvider = when {
                    result is SyncProviderResult.Error -> state.syncWarningsByProvider - providerId
                    (result as? SyncProviderResult.Success)?.isPartial == true -> state.syncWarningsByProvider + (providerId to partialWarnings)
                    else -> state.syncWarningsByProvider - providerId
                }
            )
        }
    }

    private suspend fun runRebuildIndex(providerId: Long, providerName: String?) {
        val provider = providerRepository.getProvider(providerId)
        val pendingXtreamTextRefreshGeneration = provider
            ?.takeIf { it.type == ProviderType.XTREAM_CODES }
            ?.let { currentProvider ->
                val currentGeneration = preferencesRepository.getXtreamTextImportGeneration()
                val appliedGeneration = preferencesRepository.getXtreamTextImportAppliedGeneration(currentProvider.id)
                currentGeneration.takeIf { it > appliedGeneration }
            }
        val result = if (provider?.type == ProviderType.XTREAM_CODES) {
            syncManager.rebuildXtreamIndex(providerId) { message ->
                uiState.update { state ->
                    state.copy(syncProgress = message, syncingProviderName = providerName)
                }
            }
        } else {
            val syncResult = syncProvider(
                SyncProviderCommand(
                    providerId = providerId,
                    force = true,
                    movieFastSyncOverride = null,
                    epgSyncModeOverride = null
                ),
                onProgress = { message ->
                    uiState.update { state ->
                        state.copy(syncProgress = message, syncingProviderName = providerName)
                    }
                }
            )
            when (syncResult) {
                is SyncProviderResult.Success -> Result.success(Unit)
                is SyncProviderResult.Error -> Result.error(syncResult.message, syncResult.exception)
            }
        }
        if (result !is Result.Error) {
            pendingXtreamTextRefreshGeneration?.let { generation ->
                preferencesRepository.markXtreamTextImportApplied(providerId, generation)
            }
        }
        uiState.update { state ->
            state.copy(
                isSyncing = false,
                syncProgress = null,
                syncingProviderName = null,
                userMessage = when {
                    result is Result.Error -> "Rebuild index failed: ${result.message}"
                    else -> "Index rebuild queued"
                },
                syncWarningsByProvider = when {
                    result is Result.Error -> state.syncWarningsByProvider - providerId
                    else -> state.syncWarningsByProvider - providerId
                }
            )
        }
    }

    private fun mapSyncNowProgress(message: String): String = when (message) {
        "Downloading Movies..." -> "Checking Movies..."
        "Downloading Series..." -> "Checking Series..."
        else -> message
    }

    fun deleteProvider(scope: CoroutineScope, providerId: Long, onSuccess: () -> Unit = {}) {
        scope.launch {
            uiState.update { it.copy(isDeletingProvider = true) }
            when (val result = providerRepository.deleteProvider(providerId)) {
                is Result.Success -> {
                    watchNextManager.refreshWatchNext()
                    launcherRecommendationsManager.refreshRecommendations(force = true)
                    tvInputChannelSyncManager.refreshTvInputCatalog()
                    uiState.update { it.copy(isDeletingProvider = false, userMessage = "Provider deleted") }
                    onSuccess()
                }
                is Result.Error -> uiState.update { it.copy(isDeletingProvider = false, userMessage = "Could not delete provider: ${result.message}") }
                Result.Loading -> Unit
            }
        }
    }
}

enum class SettingsProviderSyncMode {
    SYNC_NOW,
    REBUILD_INDEX
}
