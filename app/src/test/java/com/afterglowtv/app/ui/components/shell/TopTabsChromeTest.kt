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
            "local_media"
        ).inOrder()
        assertThat(guideTabs.map { it.label }).containsExactly(
            "TV Guide",
            "Personal Guide"
        ).inOrder()
        assertThat(
            defaultTopTabs(
                developerModeEnabled = true,
                showAdultGuideTab = true
            ).first { it.id == "adult_guide" }.label
        ).isEqualTo("Adult")
    }

    @Test
    fun `adult guide tab uses custom label`() {
        val adultTab = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true,
            adultGuideLabel = "After Dark"
        ).first { it.id == "adult_guide" }

        assertThat(adultTab.label).isEqualTo("After Dark")
    }

    @Test
    fun `adult guide tab can be hidden`() {
        val guideTabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = false
        ).filter { tab ->
            tab.label.contains("Guide", ignoreCase = true)
        }

        assertThat(guideTabs.map { it.id }).containsExactly(
            "epg",
            "local_media"
        ).inOrder()
    }

    @Test
    fun `vod guide is not a top level tab`() {
        val tabIds = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true
        ).map { it.id }

        assertThat(tabIds).doesNotContain("vod_guide")
        assertThat(tabIds).contains("movies")
        assertThat(tabIds).contains("series")
    }
}
