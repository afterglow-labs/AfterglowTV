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
            policy = StorePolicySnapshot.fullFeature
        )

        assertThat(tabs.map { it.id }).containsExactly(
            "home",
            "live_tv",
            "epg",
            "vod_container",
            "adult",
            "local_media",
            "search",
            "settings"
        ).inOrder()
        assertThat(tabs.map { it.label }).containsExactly(
            "Home",
            "Live TV",
            "TV Guide",
            "VOD",
            "Adult",
            "Library",
            "Search",
            "Settings"
        ).inOrder()
    }

    @Test
    fun `adult tab is hidden until developer mode is unlocked`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = false,
            showAdultTab = true,
            policy = StorePolicySnapshot.fullFeature
        )

        assertThat(tabs.map { it.id }).containsExactly(
            "home",
            "live_tv",
            "epg",
            "vod_container",
            "local_media",
            "search",
            "settings"
        ).inOrder()
    }

    @Test
    fun `developer mode still respects adult visibility preference`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultTab = false,
            policy = StorePolicySnapshot.fullFeature
        )

        assertThat(tabs.map { it.id }).containsExactly(
            "home",
            "live_tv",
            "epg",
            "vod_container",
            "local_media",
            "search",
            "settings"
        ).inOrder()
    }

    @Test
    fun `locked amazon review surface shows live tv guide and settings`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = false,
            showAdultTab = true,
            policy = StorePolicySnapshot.amazon
        )

        assertThat(tabs.map { it.id }).containsExactly("home", "live_tv", "epg", "settings").inOrder()
        assertThat(tabs.map { it.label }).containsExactly("Home", "Live TV", "TV Guide", "Settings").inOrder()
    }

    @Test
    fun `unlocked amazon review surface keeps vod label`() {
        val tabs = defaultTopTabs(
            developerModeEnabled = true,
            showAdultTab = true,
            policy = StorePolicySnapshot.amazon.effectiveFor(
                storedDeveloperModeEnabled = true,
                nowMs = 0L
            )
        )

        assertThat(tabs.single { it.id == "vod_container" }.label).isEqualTo("VOD")
        assertThat(tabs.map { it.label }).doesNotContain("Video")
    }
}
