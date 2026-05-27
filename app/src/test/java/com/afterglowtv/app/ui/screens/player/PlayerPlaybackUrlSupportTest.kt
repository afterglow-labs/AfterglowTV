package com.afterglowtv.app.ui.screens.player

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.StreamInfo
import com.afterglowtv.player.PlaybackState
import org.junit.Test

class PlayerPlaybackUrlSupportTest {

    @Test
    fun `resolveTimeshiftStreamInfo prefers resolved metadata for restart`() {
        val resolved = StreamInfo(
            url = "https://resolved.example/live.m3u8",
            title = "Resolved title",
            headers = mapOf("Authorization" to "Bearer token"),
            userAgent = "AfterglowTVTest",
            expirationTime = 123_456L
        )

        val result = resolveTimeshiftStreamInfo(
            streamInfoOverride = null,
            currentResolvedStreamInfo = resolved,
            currentResolvedPlaybackUrl = resolved.url,
            currentStreamUrl = "stalker://logical",
            playbackTitle = "Playback title",
            currentTitle = "Fallback title"
        )

        assertThat(result).isEqualTo(resolved)
        assertThat(result?.headers).containsEntry("Authorization", "Bearer token")
        assertThat(result?.userAgent).isEqualTo("AfterglowTVTest")
        assertThat(result?.expirationTime).isEqualTo(123_456L)
    }

    @Test
    fun `resolveTimeshiftStreamInfo falls back to url when metadata is unavailable`() {
        val result = resolveTimeshiftStreamInfo(
            streamInfoOverride = null,
            currentResolvedStreamInfo = null,
            currentResolvedPlaybackUrl = " https://fallback.example/live.ts ",
            currentStreamUrl = "logical://stream",
            playbackTitle = "",
            currentTitle = "Fallback title"
        )

        assertThat(result).isEqualTo(
            StreamInfo(
                url = "https://fallback.example/live.ts",
                title = "Fallback title"
            )
        )
    }

    @Test
    fun `resolvePlaybackIdentityUrl tolerates missing playback urls`() {
        val result = resolvePlaybackIdentityUrl(
            currentResolvedPlaybackUrl = null,
            currentStreamUrl = null
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `matchesActivePlaybackSession tolerates missing playback urls`() {
        val result = matchesActivePlaybackSession(
            requestVersion = 9L,
            activeRequestVersion = 9L,
            expectedLogicalUrl = "playlist://demo/1",
            currentResolvedPlaybackUrl = null,
            currentStreamUrl = null
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `shouldReuseActivePlaybackForRoute rejects route reuse when playback urls are missing`() {
        val result = shouldReuseActivePlaybackForRoute(
            hasArchiveRequest = false,
            prepareRequestVersion = 3L,
            playbackState = PlaybackState.READY,
            currentContentType = ContentType.LIVE,
            requestedContentType = ContentType.LIVE.name,
            currentContentId = 230L,
            requestedContentId = 230L,
            currentProviderId = 7L,
            requestedProviderId = 7L,
            currentStreamUrl = null,
            currentResolvedPlaybackUrl = null,
            requestedStreamUrl = "playlist://demo/230"
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `shouldReuseActivePlaybackForRoute keeps active live player for same route`() {
        val result = shouldReuseActivePlaybackForRoute(
            hasArchiveRequest = false,
            prepareRequestVersion = 3L,
            playbackState = PlaybackState.READY,
            currentContentType = ContentType.LIVE,
            requestedContentType = ContentType.LIVE.name,
            currentContentId = 230L,
            requestedContentId = 230L,
            currentProviderId = 7L,
            requestedProviderId = 7L,
            currentStreamUrl = "xtream://live/230",
            currentResolvedPlaybackUrl = "https://cdn.example/live/230.m3u8",
            requestedStreamUrl = "xtream://live/230"
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `shouldReuseActivePlaybackForRoute rejects stale or different route`() {
        val result = shouldReuseActivePlaybackForRoute(
            hasArchiveRequest = false,
            prepareRequestVersion = 3L,
            playbackState = PlaybackState.READY,
            currentContentType = ContentType.LIVE,
            requestedContentType = ContentType.LIVE.name,
            currentContentId = 230L,
            requestedContentId = 231L,
            currentProviderId = 7L,
            requestedProviderId = 7L,
            currentStreamUrl = "xtream://live/230",
            currentResolvedPlaybackUrl = "https://cdn.example/live/230.m3u8",
            requestedStreamUrl = "xtream://live/231"
        )

        assertThat(result).isFalse()
    }
}
