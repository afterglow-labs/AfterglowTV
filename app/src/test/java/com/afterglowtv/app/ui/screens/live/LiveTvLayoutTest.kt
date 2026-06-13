package com.afterglowtv.app.ui.screens.live

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiveTvLayoutTest {

    @Test
    fun `adult gives more width to preview than regular pro mode`() {
        val regularPro = liveContentPaneWeights(adultMode = false, isProMode = true)
        val adult = liveContentPaneWeights(adultMode = true, isProMode = true)

        assertThat(adult.channelList).isLessThan(regularPro.channelList)
        assertThat(adult.preview).isGreaterThan(regularPro.preview)
        assertThat(adult.channelList).isLessThan(0.7f)
        assertThat(adult.preview).isGreaterThan(1.3f)
    }

    @Test
    fun `non pro mode keeps one full channel pane`() {
        val weights = liveContentPaneWeights(adultMode = false, isProMode = false)

        assertThat(weights.channelList).isEqualTo(1f)
        assertThat(weights.preview).isEqualTo(0f)
    }
}
