package com.afterglowtv.app.ui.screens.settings

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.app.tv.LauncherRecommendationsManager
import com.afterglowtv.app.tv.WatchNextManager
import com.afterglowtv.app.tvinput.TvInputChannelSyncManager
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.data.sync.SyncManager
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.CombinedM3uRepository
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.repository.SyncMetadataRepository
import com.afterglowtv.domain.usecase.SyncProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsProviderActionsTest {

    private val providerRepository: ProviderRepository = mock()
    private val combinedM3uRepository: CombinedM3uRepository = mock()
    private val preferencesRepository: PreferencesRepository = mock()
    private val syncProvider: SyncProvider = mock()
    private val syncManager: SyncManager = mock()
    private val syncMetadataRepository: SyncMetadataRepository = mock()
    private val watchNextManager: WatchNextManager = mock()
    private val launcherRecommendationsManager: LauncherRecommendationsManager = mock()
    private val tvInputChannelSyncManager: TvInputChannelSyncManager = mock()
    private val uiState = MutableStateFlow(SettingsUiState())

    private val actions = SettingsProviderActions(
        providerRepository = providerRepository,
        combinedM3uRepository = combinedM3uRepository,
        preferencesRepository = preferencesRepository,
        syncProvider = syncProvider,
        syncManager = syncManager,
        syncMetadataRepository = syncMetadataRepository,
        watchNextManager = watchNextManager,
        launcherRecommendationsManager = launcherRecommendationsManager,
        tvInputChannelSyncManager = tvInputChannelSyncManager,
        uiState = uiState
    )

    @Test
    fun setActiveProvider_refreshesProviderScopedTvSurfaces() = runTest(StandardTestDispatcher()) {
        val provider = Provider(
            id = 7L,
            name = "Provider Seven",
            type = ProviderType.M3U,
            serverUrl = "https://example.com",
            lastSyncedAt = System.currentTimeMillis()
        )
        whenever(providerRepository.getProvider(7L)).thenReturn(provider)

        actions.setActiveProvider(this, 7L)
        advanceUntilIdle()

        verify(preferencesRepository).setLastActiveProviderId(7L)
        verify(combinedM3uRepository).setActiveLiveSource(ActiveLiveSource.ProviderSource(7L))
        verify(providerRepository).setActiveProvider(7L)
        verify(watchNextManager).refreshWatchNext()
        verify(launcherRecommendationsManager).refreshRecommendations(force = true)
        verify(tvInputChannelSyncManager).refreshTvInputCatalog()
        verify(syncProvider, never()).invoke(any(), any())
    }

    @Test
    fun setActiveProvider_routesVodPlaylistToVodSourceSlot() = runTest(StandardTestDispatcher()) {
        val provider = Provider(
            id = 8L,
            name = "VOD Playlist",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u",
            m3uUrl = "https://example.com/vod.m3u",
            m3uPlaylistKind = ProviderM3uPlaylistKind.VOD,
            lastSyncedAt = System.currentTimeMillis()
        )
        whenever(providerRepository.getProvider(8L)).thenReturn(provider)

        actions.setActiveProvider(this, 8L)
        advanceUntilIdle()

        verify(preferencesRepository).setActiveSource(
            ProviderSourceSlot.VOD,
            ActiveLiveSource.ProviderSource(8L)
        )
        verify(providerRepository, never()).setActiveProvider(8L)
        verify(combinedM3uRepository, never()).setActiveLiveSource(any())
        assertThat(uiState.value.userMessage).isEqualTo("VOD source set to VOD Playlist")
    }

    @Test
    fun deleteProvider_refreshesProviderScopedTvSurfaces() = runTest(StandardTestDispatcher()) {
        whenever(providerRepository.deleteProvider(7L)).thenReturn(Result.success(Unit))

        actions.deleteProvider(this, 7L)
        advanceUntilIdle()

        verify(providerRepository).deleteProvider(7L)
        verify(watchNextManager).refreshWatchNext()
        verify(launcherRecommendationsManager).refreshRecommendations(force = true)
        verify(tvInputChannelSyncManager).refreshTvInputCatalog()
        assertThat(uiState.value.userMessage).isEqualTo("Provider deleted")
    }
}
