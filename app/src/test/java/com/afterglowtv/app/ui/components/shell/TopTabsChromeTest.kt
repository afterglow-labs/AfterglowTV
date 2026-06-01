package com.afterglowtv.app.ui.components.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopTabsChromeTest {

    @Test
    fun `program guide and personal library are separate top level tabs`() {
        val guideTabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true
        ).filter { tab ->
            tab.label.contains("Guide", ignoreCase = true)
        }
        val localMediaTab = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true
        ).first { it.id == "local_media" }

        assertThat(guideTabs.map { it.id }).containsExactly("epg").inOrder()
        assertThat(guideTabs.map { it.label }).containsExactly("TV Guide").inOrder()
        assertThat(localMediaTab.label).isEqualTo("Personal Library")
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

        assertThat(guideTabs.map { it.id }).containsExactly("epg").inOrder()
    }

    @Test
    fun `vod replaces movies and series as a top level tab`() {
        val tabIds = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true
        ).map { it.id }
        val vodTab = defaultTopTabs(
            developerModeEnabled = true,
            showAdultGuideTab = true
        ).first { it.id == "vod_container" }

        assertThat(tabIds).contains("vod_container")
        assertThat(tabIds).doesNotContain("movies")
        assertThat(tabIds).doesNotContain("series")
        assertThat(vodTab.label).isEqualTo("VOD")
    }
}
