package com.afterglowtv.app.ui.screens.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `adult guide gives more width to preview than regular pro mode`() {
        val regularPro = liveContentPaneWeights(adultGuideMode = false, isProMode = true)
        val adultGuide = liveContentPaneWeights(adultGuideMode = true, isProMode = true)

        assertThat(adultGuide.channelList).isLessThan(regularPro.channelList)
        assertThat(adultGuide.preview).isGreaterThan(regularPro.preview)
        assertThat(adultGuide.channelList).isLessThan(0.7f)
        assertThat(adultGuide.preview).isGreaterThan(1.3f)
    }

    @Test
    fun `non pro mode keeps one full channel pane`() {
        val weights = liveContentPaneWeights(adultGuideMode = false, isProMode = false)

        assertThat(weights.channelList).isEqualTo(1f)
        assertThat(weights.preview).isEqualTo(0f)
    }
}
