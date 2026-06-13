package com.afterglowtv.app.ui.screens.live

import com.afterglowtv.domain.model.Channel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiveTvFocusRestoreTest {

    @Test
    fun `restore loads another page when saved channel is not loaded yet`() {
        val loadedChannels = (1L..200L).map { id ->
            Channel(
                id = id,
                name = "Channel $id",
                streamUrl = "https://example.com/$id.m3u8"
            )
        }

        val plan = resolveChannelRestorePlan(
            savedChannelId = 230L,
            channels = loadedChannels,
            hasMoreChannels = true
        )

        assertThat(plan).isEqualTo(LiveTvChannelRestorePlan.LoadMoreForSavedChannel)
    }

    @Test
    fun `restore targets saved channel index when the channel is loaded`() {
        val loadedChannels = (225L..235L).map { id ->
            Channel(
                id = id,
                name = "Channel $id",
                streamUrl = "https://example.com/$id.m3u8"
            )
        }

        val plan = resolveChannelRestorePlan(
            savedChannelId = 230L,
            channels = loadedChannels,
            hasMoreChannels = false
        )

        assertThat(plan).isEqualTo(LiveTvChannelRestorePlan.FocusChannel(channelId = 230L, index = 5))
    }
}
