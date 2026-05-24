package com.afterglowtv.app.ui.components.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopTabsChromeTest {

    @Test
    fun `program guide destinations are separate top level tabs`() {
        val guideTabs = DefaultTopTabs.filter { tab ->
            tab.label.contains("Guide", ignoreCase = true)
        }

        assertThat(guideTabs.map { it.id }).containsExactly(
            "epg",
            "vod_guide",
            "adult_guide",
            "local_media"
        ).inOrder()
        assertThat(guideTabs.map { it.label }).containsExactly(
            "IPTV Guide",
            "VOD Guide",
            "XXX Guide",
            "Personal Guide"
        ).inOrder()
    }

    @Test
    fun `adult guide tab can be hidden`() {
        val guideTabs = defaultTopTabs(showAdultGuideTab = false).filter { tab ->
            tab.label.contains("Guide", ignoreCase = true)
        }

        assertThat(guideTabs.map { it.id }).containsExactly(
            "epg",
            "vod_guide",
            "local_media"
        ).inOrder()
    }
}
