package com.afterglowtv.app.ui.screens.movies

import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoviesVodGuideSourceLoadingTest {
    @Test
    fun `vod guide auto loads an unsynced selected source`() {
        val provider = provider(lastSyncedAt = 0L)

        assertThat(
            shouldAutoLoadVodGuideSource(
                provider = provider,
                libraryCount = 4,
                alreadyAttempted = false
            )
        ).isTrue()
    }

    @Test
    fun `vod guide auto loads a synced source with an empty vod database`() {
        val provider = provider(lastSyncedAt = 123L)

        assertThat(
            shouldAutoLoadVodGuideSource(
                provider = provider,
                libraryCount = 0,
                alreadyAttempted = false
            )
        ).isTrue()
    }

    @Test
    fun `vod guide does not auto load a populated source again`() {
        val provider = provider(lastSyncedAt = 123L)

        assertThat(
            shouldAutoLoadVodGuideSource(
                provider = provider,
                libraryCount = 4,
                alreadyAttempted = false
            )
        ).isFalse()
    }

    @Test
    fun `vod guide does not repeat an automatic source load in the same session`() {
        val provider = provider(lastSyncedAt = 0L)

        assertThat(
            shouldAutoLoadVodGuideSource(
                provider = provider,
                libraryCount = 0,
                alreadyAttempted = true
            )
        ).isFalse()
    }

    private fun provider(lastSyncedAt: Long): Provider =
        Provider(
            id = 7L,
            name = "VOD Source",
            type = ProviderType.M3U,
            serverUrl = "https://example.test/vod.m3u8",
            m3uUrl = "https://example.test/vod.m3u8",
            lastSyncedAt = lastSyncedAt
        )
}
