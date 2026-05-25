package com.afterglowtv.app.ui.components.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopTabsChromeTest {

    @Test
    fun `program guide destinations are separate top level tabs`() {
        val guideTabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true
        ).filter { tab ->
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
    fun `adult guide tab is hidden until developer mode is unlocked`() {
        val guideTabs = defaultTopTabs(
            developerModeEnabled = false,
            showAdultGuideTab = true
        ).filter { tab ->
            tab.label.contains("Guide", ignoreCase = true)
        }

        assertThat(guideTabs.map { it.id }).containsExactly(
            "epg",
            "vod_guide",
            "local_media"
        ).inOrder()
    }

    @Test
    fun `developer mode still respects adult guide visibility preference`() {
        val guideTabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = false
        ).filter { tab ->
            tab.label.contains("Guide", ignoreCase = true)
        }

        assertThat(guideTabs.map { it.id }).containsExactly(
            "epg",
            "vod_guide",
            "local_media"
        ).inOrder()
    }
}
