package com.afterglowtv.app.ui.screens.player

import com.afterglowtv.domain.model.ContentType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerPlaybackHistorySupportTest {

    @Test
    fun `local media provider id is not written to provider playback history`() {
        val snapshot = buildPlaybackHistorySnapshot(
            positionMs = 5_000L,
            durationMs = 60_000L,
            currentContentId = 42L,
            currentProviderId = 0L,
            currentContentType = ContentType.MOVIE,
            currentTitle = "Local clip",
            currentArtworkUrl = null,
            currentStreamUrl = "smb://192.168.1.8/Plex/Local clip.mp4",
            currentSeriesId = null,
            currentEpisode = null,
            currentSeasonNumber = null,
            currentEpisodeNumber = null
        )

        assertThat(snapshot).isNull()
    }

    @Test
    fun `provider backed vod can still write playback history`() {
        val snapshot = buildPlaybackHistorySnapshot(
            positionMs = 5_000L,
            durationMs = 60_000L,
            currentContentId = 42L,
            currentProviderId = 7L,
            currentContentType = ContentType.MOVIE,
            currentTitle = "Provider movie",
            currentArtworkUrl = null,
            currentStreamUrl = "https://example.com/movie.mp4",
            currentSeriesId = null,
            currentEpisode = null,
            currentSeasonNumber = null,
            currentEpisodeNumber = null
        )

        assertThat(snapshot?.providerId).isEqualTo(7L)
    }
}
