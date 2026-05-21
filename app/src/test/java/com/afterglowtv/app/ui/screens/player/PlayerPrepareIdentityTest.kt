package com.afterglowtv.app.ui.screens.player

import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.ChannelRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerPrepareIdentityTest {

    @Test
    fun `buildPlayerPrepareIdentity ignores display metadata changes`() {
        val baseline = buildPlayerPrepareIdentity(
            streamUrl = "https://example.com/stream",
            epgChannelId = "epg-1",
            internalChannelId = 100L,
            categoryId = 10L,
            providerId = 20L,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "SERIES_EPISODE",
            archiveStartMs = null,
            archiveEndMs = null
        )

        val sameIdentity = buildPlayerPrepareIdentity(
            streamUrl = "https://example.com/stream",
            epgChannelId = "epg-1",
            internalChannelId = 100L,
            categoryId = 10L,
            providerId = 20L,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "SERIES_EPISODE",
            archiveStartMs = null,
            archiveEndMs = null
        )

        assertThat(sameIdentity).isEqualTo(baseline)
    }

    @Test
    fun `buildPlayerPrepareIdentity changes when archive window changes`() {
        val baseline = buildPlayerPrepareIdentity(
            streamUrl = "https://example.com/stream",
            epgChannelId = "epg-1",
            internalChannelId = 100L,
            categoryId = 10L,
            providerId = 20L,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "LIVE",
            archiveStartMs = 1_000L,
            archiveEndMs = 2_000L
        )

        val changedWindow = buildPlayerPrepareIdentity(
            streamUrl = "https://example.com/stream",
            epgChannelId = "epg-1",
            internalChannelId = 100L,
            categoryId = 10L,
            providerId = 20L,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "LIVE",
            archiveStartMs = 2_000L,
            archiveEndMs = 3_000L
        )

        assertThat(changedWindow).isNotEqualTo(baseline)
    }

    @Test
    fun `buildPlayerPrepareIdentity changes when explicit start position changes`() {
        val baseline = buildPlayerPrepareIdentity(
            streamUrl = "content://media/local-episode",
            epgChannelId = null,
            internalChannelId = 300L,
            categoryId = null,
            providerId = null,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "MOVIE",
            archiveStartMs = null,
            archiveEndMs = null,
            startPositionMs = 0L
        )

        val changedPosition = buildPlayerPrepareIdentity(
            streamUrl = "content://media/local-episode",
            epgChannelId = null,
            internalChannelId = 300L,
            categoryId = null,
            providerId = null,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "MOVIE",
            archiveStartMs = null,
            archiveEndMs = null,
            startPositionMs = 37 * 60_000L
        )

        assertThat(changedPosition).isNotEqualTo(baseline)
    }

    @Test
    fun `resolveRouteDisplayTitle prefers archive title for live archive playback`() {
        assertThat(
            resolveRouteDisplayTitle(
                title = "Channel",
                contentType = "LIVE",
                archiveStartMs = 1_000L,
                archiveEndMs = 2_000L,
                archiveTitle = "Morning News"
            )
        ).isEqualTo("Morning News")
    }

    @Test
    fun `prepare identity preserves all-channels category id`() {
        val identity = buildPlayerPrepareIdentity(
            streamUrl = "https://example.com/live/230.m3u8",
            epgChannelId = null,
            internalChannelId = 230L,
            categoryId = ChannelRepository.ALL_CHANNELS_ID,
            providerId = 1L,
            isVirtual = false,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "LIVE",
            archiveStartMs = null,
            archiveEndMs = null
        )

        assertThat(identity.categoryId).isEqualTo(ChannelRepository.ALL_CHANNELS_ID)
    }

    @Test
    fun `prepare identity preserves virtual live category ids`() {
        val identity = buildPlayerPrepareIdentity(
            streamUrl = "https://example.com/live/recent.m3u8",
            epgChannelId = null,
            internalChannelId = 230L,
            categoryId = VirtualCategoryIds.RECENT,
            providerId = 1L,
            isVirtual = true,
            combinedProfileId = null,
            combinedSourceFilterProviderId = null,
            contentType = "LIVE",
            archiveStartMs = null,
            archiveEndMs = null
        )

        assertThat(identity.categoryId).isEqualTo(VirtualCategoryIds.RECENT)
    }
}
