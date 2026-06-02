package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderSyncOptionsTest {
    @Test
    fun `vod m3u providers expose vod and epg sync sections`() {
        val provider = Provider(
            id = 42L,
            name = "VOD Playlist",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/IPTV_VOD",
            m3uUrl = "https://example.com/IPTV_VOD",
            epgUrl = "https://example.com/IPTV_VOD.xml",
            m3uPlaylistKind = ProviderM3uPlaylistKind.VOD
        )

        assertThat(provider.availableSyncSelections())
            .containsExactly(ProviderSyncSelection.VOD, ProviderSyncSelection.EPG)
            .inOrder()
        assertThat(provider.defaultCustomSyncSelections())
            .containsExactly(ProviderSyncSelection.VOD, ProviderSyncSelection.EPG)
    }

    @Test
    fun `live m3u providers do not expose old vod sections`() {
        val provider = Provider(
            id = 7L,
            name = "Live Playlist",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/live.m3u",
            m3uUrl = "https://example.com/live.m3u",
            m3uPlaylistKind = ProviderM3uPlaylistKind.LIVE
        )

        assertThat(provider.availableSyncSelections())
            .containsExactly(ProviderSyncSelection.TV, ProviderSyncSelection.EPG)
            .inOrder()
    }

    @Test
    fun `xtream providers still expose live vod series and epg sections`() {
        val provider = Provider(
            id = 9L,
            name = "Portal",
            type = ProviderType.XTREAM_CODES,
            serverUrl = "https://example.com",
            username = "user"
        )

        assertThat(provider.availableSyncSelections())
            .containsExactly(
                ProviderSyncSelection.TV,
                ProviderSyncSelection.VOD,
                ProviderSyncSelection.SERIES,
                ProviderSyncSelection.EPG
            )
            .inOrder()
    }
}
