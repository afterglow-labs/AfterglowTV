package com.afterglowtv.app.ui.components.shell

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopTabsChromeTest {

    @Test
    fun `program guide destinations are separate top level tabs`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultTab = true,
            policy = StorePolicySnapshot.standard
        )

        assertThat(tabs.map { it.id }).containsExactly(
            "home",
            "live_tv",
            "epg",
            "vod_container",
            "adult",
            "local_media",
            "favorites",
            "search",
            "settings"
        ).inOrder()
        assertThat(tabs.map { it.label }).containsExactly(
            "Home",
            "Live TV",
            "IPTV Guide",
            "VOD",
            "Adult",
            "Personal Library",
            "Favorites",
            "Search",
            "Settings"
        ).inOrder()
    }

    @Test
    fun `adult tab is hidden until developer mode is unlocked`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = false,
            showAdultTab = true,
            policy = StorePolicySnapshot.standard
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
            showAdultTab = false,
            policy = StorePolicySnapshot.standard
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
    fun `locked amazon review surface shows tv guide and settings`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = false,
            showAdultTab = true,
            policy = StorePolicySnapshot.amazon
        )

        assertThat(tabs.map { it.id }).containsExactly("epg", "settings").inOrder()
        assertThat(tabs.map { it.label }).containsExactly("TV Guide", "Settings").inOrder()
    }
}
