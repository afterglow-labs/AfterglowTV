package com.afterglowtv.app.ui.components.shell

import com.afterglowtv.app.store.StorePolicy
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

        val expectedIds = buildList {
            add("epg")
            add("vod_guide")
            if (StorePolicy.current.showAdultSurfaces) add("adult_guide")
            add("local_media")
        }
        val expectedLabels = buildList {
            add(if (StorePolicy.current.amazonReviewBuild) "TV Guide" else "IPTV Guide")
            add(if (StorePolicy.current.amazonReviewBuild) "Video Guide" else "VOD Guide")
            if (StorePolicy.current.showAdultSurfaces) add("XXX Guide")
            add("Personal Guide")
        }

        assertThat(guideTabs.map { it.id }).containsExactlyElementsIn(expectedIds).inOrder()
        assertThat(guideTabs.map { it.label }).containsExactlyElementsIn(expectedLabels).inOrder()
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
