package com.afterglowtv.app.ui.screens.settings

import android.app.Application
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.update.AppUpdateInstaller
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.manager.RecordingManager
import com.afterglowtv.domain.model.RecordingItem
import com.afterglowtv.domain.repository.CategoryRepository
import com.afterglowtv.domain.repository.ChannelRepository
import com.afterglowtv.domain.repository.CombinedM3uRepository
import com.afterglowtv.domain.repository.EpgSourceRepository
import com.afterglowtv.domain.repository.MovieRepository
import com.afterglowtv.domain.repository.SeriesRepository
import com.afterglowtv.domain.repository.SyncMetadataRepository
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.usecase.GetCustomCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun registerSettingsAppUpdateObservers(
    scope: CoroutineScope,
    preferencesRepository: PreferencesRepository,
    appUpdateActions: SettingsAppUpdateActions,
    appUpdateInstaller: AppUpdateInstaller,
    uiState: MutableStateFlow<SettingsUiState>
) {
    if (!StorePolicy.current.enableSideloadUpdates) {
        return
    }

    scope.launch {
        combine(
            preferencesRepository.autoCheckAppUpdates,
            preferencesRepository.lastAppUpdateCheckTimestamp,
            preferencesRepository.autoDownloadAppUpdates
        ) { autoCheckEnabled, lastCheckedAt, autoDownload ->
            Triple(autoCheckEnabled, lastCheckedAt, autoDownload)
        }.distinctUntilChanged().collect { (autoCheckEnabled, lastCheckedAt, autoDownload) ->
            if (autoCheckEnabled && appUpdateActions.shouldAutoCheckForUpdates(lastCheckedAt)) {
                appUpdateActions.checkForAppUpdates(
                    scope = scope,
                    manual = false,
                    isRemoteVersionNewer = ::isRemoteVersionNewer,
                    autoDownload = autoDownload
                )
            }
        }
    }

    scope.launch {
        appUpdateInstaller.downloadState.collect { downloadState ->
            uiState.update {
                it.copy(appUpdate = it.appUpdate.withDownloadState(downloadState))
            }
        }
    }

    scope.launch {
        appUpdateInstaller.refreshState()
    }
}

internal fun registerCombinedProfileObservers(
    scope: CoroutineScope,
    combinedM3uRepository: CombinedM3uRepository,
    uiState: MutableStateFlow<SettingsUiState>
) {
    scope.launch {
        combinedM3uRepository.getProfiles().collect { profiles ->
            uiState.update { it.copy(combinedProfiles = profiles) }
        }
    }

    scope.launch {
        combinedM3uRepository.getAvailableM3uProviders().collect { providers ->
            uiState.update {
                it.copy(availableM3uProviders = providers.filter(StorePolicy.current::isUserVisibleProvider))
            }
        }
    }

    scope.launch {
        combinedM3uRepository.getActiveLiveSource().collect { activeSource ->
            uiState.update { it.copy(activeLiveSource = activeSource) }
        }
    }
}

internal fun registerRecordingObservers(
    scope: CoroutineScope,
    recordingManager: RecordingManager,
    preferencesRepository: PreferencesRepository,
    uiState: MutableStateFlow<SettingsUiState>
) {
    scope.launch {
        recordingManager.observeRecordingItems().collect { items ->
            uiState.update { it.copy(recordingItems = items.sortedByDescending(RecordingItem::scheduledStartMs)) }
        }
    }

    scope.launch {
        recordingManager.observeStorageState().collect { storage ->
            uiState.update { it.copy(recordingStorageState = storage) }
        }
    }

    scope.launch {
        preferencesRepository.recordingWifiOnly.collect { wifiOnly ->
            uiState.update { it.copy(wifiOnlyRecording = wifiOnly) }
        }
    }

    scope.launch {
        preferencesRepository.recordingPaddingBeforeMinutes.collect { minutes ->
            uiState.update { it.copy(recordingPaddingBeforeMinutes = minutes) }
        }
    }

    scope.launch {
        preferencesRepository.recordingPaddingAfterMinutes.collect { minutes ->
            uiState.update { it.copy(recordingPaddingAfterMinutes = minutes) }
        }
    }
}

internal fun registerEpgObservers(
    scope: CoroutineScope,
    epgSourceRepository: EpgSourceRepository,
    uiState: MutableStateFlow<SettingsUiState>
) {
    scope.launch {
        epgSourceRepository.getAllSources().collect { sources ->
            uiState.update { it.copy(epgSources = sources) }
        }
    }
}

internal fun registerDerivedStateObservers(
    scope: CoroutineScope,
    providerRepository: ProviderRepository,
    syncMetadataRepository: SyncMetadataRepository,
    movieRepository: MovieRepository,
    seriesRepository: SeriesRepository,
    application: Application,
    preferencesRepository: PreferencesRepository,
    activeProviderIdFlow: Flow<Long?>,
    categoryRepository: CategoryRepository,
    combinedM3uRepository: CombinedM3uRepository,
    channelRepository: ChannelRepository,
    getCustomCategories: GetCustomCategories,
    uiState: MutableStateFlow<SettingsUiState>
) {
    scope.launch {
        observeProviderDiagnostics(
            providerRepository = providerRepository,
            syncMetadataRepository = syncMetadataRepository,
            movieRepository = movieRepository,
            seriesRepository = seriesRepository,
            application = application
        ).collect { diagnosticsByProvider ->
            uiState.update { it.copy(diagnosticsByProvider = diagnosticsByProvider) }
        }
    }

    scope.launch {
        preferencesRepository.lastMaintenanceSnapshot.collect { snapshot ->
            uiState.update { it.copy(databaseMaintenance = snapshot?.toUiModel()) }
        }
    }

    scope.launch {
        observeCategoryManagement(
            activeProviderIdFlow = activeProviderIdFlow,
            preferencesRepository = preferencesRepository,
            categoryRepository = categoryRepository
        ).collect { snapshot ->
            uiState.update {
                it.copy(
                    categorySortModes = snapshot.categorySortModes,
                    hiddenCategories = snapshot.hiddenCategories
                )
            }
        }
    }

    scope.launch {
        observeGuideDefaultCategoryOptions(
            combinedM3uRepository = combinedM3uRepository,
            channelRepository = channelRepository,
            preferencesRepository = preferencesRepository,
            getCustomCategories = getCustomCategories
        ).collect { categories ->
            uiState.update { it.copy(guideDefaultCategoryOptions = categories) }
        }
    }
}
