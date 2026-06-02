package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderActiveRolesTest {
    @Test
    fun `live vod and adult slots can mark different playlists active`() {
        val liveProvider = Provider(id = 1L, name = "Live", type = ProviderType.M3U, serverUrl = "live")
        val vodProvider = Provider(
            id = 2L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "vod",
            m3uPlaylistKind = ProviderM3uPlaylistKind.VOD
        )
        val adultProvider = Provider(id = 3L, name = "Adult", type = ProviderType.M3U, serverUrl = "adult")

        assertThat(
            providerActiveRoles(
                provider = liveProvider,
                activeProviderId = liveProvider.id,
                activeLiveSource = ActiveLiveSource.ProviderSource(liveProvider.id),
                activeVodSource = ActiveLiveSource.ProviderSource(vodProvider.id),
                activeAdultLiveSource = ActiveLiveSource.ProviderSource(adultProvider.id)
            )
        ).containsExactly(ProviderActiveRole.LIVE)

        assertThat(
            providerActiveRoles(
                provider = vodProvider,
                activeProviderId = liveProvider.id,
                activeLiveSource = ActiveLiveSource.ProviderSource(liveProvider.id),
                activeVodSource = ActiveLiveSource.ProviderSource(vodProvider.id),
                activeAdultLiveSource = ActiveLiveSource.ProviderSource(adultProvider.id)
            )
        ).containsExactly(ProviderActiveRole.VOD)

        assertThat(
            providerActiveRoles(
                provider = adultProvider,
                activeProviderId = liveProvider.id,
                activeLiveSource = ActiveLiveSource.ProviderSource(liveProvider.id),
                activeVodSource = ActiveLiveSource.ProviderSource(vodProvider.id),
                activeAdultLiveSource = ActiveLiveSource.ProviderSource(adultProvider.id)
            )
        ).containsExactly(ProviderActiveRole.ADULT)
    }
}
