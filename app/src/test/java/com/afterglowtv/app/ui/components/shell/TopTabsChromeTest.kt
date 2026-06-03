package com.afterglowtv.app.ui.components.shell

import com.afterglowtv.app.store.StorePolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopTabsChromeTest {

    @Test
    fun `program guide destinations are separate top level tabs`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultTab = true
        )

        val expectedIds = buildList {
            add("home")
            add("live_tv")
            add("epg")
            add("vod_container")
            if (StorePolicy.current.showAdultSurfaces) add("adult")
            add("local_media")
            add("favorites")
            add("search")
            add("settings")
        }
        val expectedLabels = buildList {
            add("Home")
            add("Live TV")
            add(if (StorePolicy.current.amazonReviewBuild) "TV Guide" else "IPTV Guide")
            add(if (StorePolicy.current.amazonReviewBuild) "Video" else "VOD")
            if (StorePolicy.current.showAdultSurfaces) add("Adult")
            add("Personal Library")
            add("Favorites")
            add("Search")
            add("Settings")
        }

        assertThat(tabs.map { it.id }).containsExactlyElementsIn(expectedIds).inOrder()
        assertThat(tabs.map { it.label }).containsExactlyElementsIn(expectedLabels).inOrder()
    }

    @Test
    fun `adult tab is hidden until developer mode is unlocked`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = false,
            showAdultTab = true
        )

        assertThat(tabs.map { it.id }).containsExactly(
            "home",
            "live_tv",
            "epg",
            "vod_container",
            "local_media",
            "favorites",
            "search",
            "settings"
        ).inOrder()
    }

    @Test
    fun `developer mode still respects adult visibility preference`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultTab = false
        )

        assertThat(tabs.map { it.id }).containsExactly(
            "home",
            "live_tv",
            "epg",
            "vod_container",
            "local_media",
            "favorites",
            "search",
            "settings"
        ).inOrder()
    }
}
