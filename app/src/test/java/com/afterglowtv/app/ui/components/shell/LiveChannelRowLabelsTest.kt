package com.afterglowtv.app.ui.components.shell

import com.afterglowtv.app.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiveChannelRowLabelsTest {

    @Test
    fun `adult channel rows omit fallback program text`() {
        assertThat(liveChannelFallbackProgramLabelRes(adultChannelMode = true))
            .isNull()
    }

    @Test
    fun `regular channel rows keep schedule fallback`() {
        assertThat(liveChannelFallbackProgramLabelRes(adultChannelMode = false))
            .isEqualTo(R.string.label_no_schedule)
    }

    @Test
    fun `focused channel rows use configured glow specs`() {
        assertThat(liveChannelRowGlowSpecs(focused = true)).isNotEmpty()
        assertThat(liveChannelRowGlowSpecs(focused = false)).isEmpty()
    }

    @Test
    fun `adult channel rows separate number from title`() {
        assertThat(liveChannelNumberBadgeLabel(channelNumber = 7, separateChannelNumber = true))
            .isEqualTo("#7")
        assertThat(
            liveChannelTitleText(
                channelNumber = 7,
                channelName = "Example Channel",
                separateChannelNumber = true
            )
        ).isEqualTo("Example Channel")
    }

    @Test
    fun `regular channel rows keep inline number formatting`() {
        assertThat(liveChannelNumberBadgeLabel(channelNumber = 7, separateChannelNumber = false))
            .isNull()
        assertThat(
            liveChannelTitleText(
                channelNumber = 7,
                channelName = "Example Channel",
                separateChannelNumber = false
            )
        ).isEqualTo("07  Example Channel")
    }
}
