package com.afterglowtv.app.ui.screens.vod

import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.Movie
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodSourceSelectionTest {
    @Test
    fun `active vod source wins over active live provider`() {
        val liveProvider = Provider(
            id = 1L,
            name = "Live",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/live.m3u"
        )
        val vodProvider = Provider(
            id = 2L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u",
            m3uPlaylistKind = ProviderM3uPlaylistKind.VOD
        )

        val selected = selectVodProvider(
            activeVodSource = ActiveLiveSource.ProviderSource(vodProvider.id),
            activeProvider = liveProvider,
            providers = listOf(liveProvider, vodProvider)
        )

        assertThat(selected).isEqualTo(vodProvider)
    }

    @Test
    fun `falls back to active provider when no vod source exists`() {
        val provider = Provider(
            id = 3L,
            name = "Portal",
            type = ProviderType.XTREAM_CODES,
            serverUrl = "https://example.com"
        )

        val selected = selectVodProvider(
            activeVodSource = null,
            activeProvider = provider,
            providers = listOf(provider)
        )

        assertThat(selected).isEqualTo(provider)
    }

    @Test
    fun `alphabet categories include all numbers and title letters`() {
        val categories = buildAlphabetCategories(
            listOf(
                Movie(id = 1L, name = "731"),
                Movie(id = 2L, name = "Scam City"),
                Movie(id = 3L, name = "Two Maladroits")
            )
        )

        assertThat(categories.map { it.label }).containsExactly("All", "#", "S", "T").inOrder()
        assertThat(categories.associate { it.label to it.count })
            .containsExactly("All", 3, "#", 1, "S", 1, "T", 1)
    }
}
