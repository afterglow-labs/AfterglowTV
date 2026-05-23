package com.afterglowtv.app.navigation

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.Episode
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.repository.ChannelRepository
import org.junit.Test

class RoutesTest {

    @Test
    fun `liveTv route supports category deep links`() {
        assertThat(Routes.liveTv()).isEqualTo(Routes.LIVE_TV)
        assertThat(Routes.liveTv(42L)).isEqualTo("${Routes.LIVE_TV}?categoryId=42")
    }

    @Test
    fun `livePlayer preserves playback context`() {
        val request = Routes.livePlayer(
            channel = Channel(
                id = 42L,
                name = "News HD",
                streamUrl = "https://example.com/live.m3u8",
                epgChannelId = "news.hd",
                categoryId = 9L,
                providerId = 7L
            ),
            categoryId = 9L,
            providerId = 7L,
            isVirtual = false
        )

        assertThat(request.internalId).isEqualTo(42L)
        assertThat(request.categoryId).isEqualTo(9L)
        assertThat(request.providerId).isEqualTo(7L)
        assertThat(request.channelId).isEqualTo("news.hd")
        assertThat(request.contentType).isEqualTo("LIVE")
        assertThat(request.isVirtual).isFalse()
    }

    @Test
    fun `livePlayer falls back to all channels when category is missing`() {
        val request = Routes.livePlayer(
            channel = Channel(
                id = 8L,
                name = "Sports",
                streamUrl = "https://example.com/sports.m3u8",
                providerId = 3L
            ),
            categoryId = null,
            providerId = 3L
        )

        assertThat(request.categoryId).isEqualTo(ChannelRepository.ALL_CHANNELS_ID)
        assertThat(request.providerId).isEqualTo(3L)
    }

    @Test
    fun `moviePlayer preserves provider and category context`() {
        val request = Routes.moviePlayer(
            Movie(
                id = 15L,
                name = "Film",
                streamUrl = "https://example.com/movie.mp4",
                categoryId = 21L,
                providerId = 5L
            )
        )

        assertThat(request.internalId).isEqualTo(15L)
        assertThat(request.categoryId).isEqualTo(21L)
        assertThat(request.providerId).isEqualTo(5L)
        assertThat(request.contentType).isEqualTo("MOVIE")
    }

    @Test
    fun `episodePlayer preserves episode playback context`() {
        val request = Routes.episodePlayer(
            Episode(
                id = 33L,
                title = "Pilot",
                episodeNumber = 1,
                seasonNumber = 1,
                streamUrl = "https://example.com/episode.mp4",
                providerId = 11L
            )
        )

        assertThat(request.internalId).isEqualTo(33L)
        assertThat(request.providerId).isEqualTo(11L)
        assertThat(request.contentType).isEqualTo("SERIES_EPISODE")
    }

    @Test
    fun `localMediaPlayer preserves local file playback context`() {
        val request = Routes.localMediaPlayer(
            LocalMediaItem(
                id = 44L,
                libraryId = 2L,
                uri = "content://media/video/44",
                displayName = "Family.Video.mp4",
                title = "Family Video",
                mediaKind = LocalMediaKind.MOVIE,
                posterUri = "content://media/poster/44"
            )
        )

        assertThat(request.streamUrl).isEqualTo("content://media/video/44")
        assertThat(request.internalId).isEqualTo(44L)
        assertThat(request.providerId).isEqualTo(0L)
        assertThat(request.contentType).isEqualTo("MOVIE")
        assertThat(request.returnRoute).isEqualTo(Routes.LOCAL_MEDIA)
    }

    @Test
    fun `player route supports archive playback context`() {
        val request = Routes.player(
            streamUrl = "https://example.com/live.m3u8",
            title = "News HD",
            internalId = 42L,
            providerId = 7L,
            contentType = "LIVE",
            archiveStartMs = 1_700_000_000_000L,
            archiveEndMs = 1_700_000_360_000L,
            archiveTitle = "News HD: Morning Show"
        )

        assertThat(request.archiveStartMs).isEqualTo(1_700_000_000_000L)
        assertThat(request.archiveEndMs).isEqualTo(1_700_000_360_000L)
        assertThat(request.archiveTitle).isEqualTo("News HD: Morning Show")
    }

    @Test
    fun `epg route preserves guide context`() {
        val route = Routes.epg(
            categoryId = 21L,
            anchorTime = 1_700_000_000_000L,
            favoritesOnly = true
        )

        assertThat(route).isEqualTo(
            "epg?categoryId=21&anchorTime=1700000000000&favoritesOnly=true"
        )
    }

    @Test
    fun `player route preserves return route back to guide`() {
        val returnRoute = Routes.epg(
            categoryId = 9L,
            anchorTime = 1_700_000_360_000L,
            favoritesOnly = false
        )
        val request = Routes.livePlayer(
            channel = Channel(
                id = 42L,
                name = "News HD",
                streamUrl = "https://example.com/live.m3u8",
                epgChannelId = "news.hd",
                categoryId = 9L,
                providerId = 7L
            ),
            categoryId = 9L,
            providerId = 7L,
            returnRoute = returnRoute
        )

        assertThat(request.returnRoute).isEqualTo(returnRoute)
    }
}
