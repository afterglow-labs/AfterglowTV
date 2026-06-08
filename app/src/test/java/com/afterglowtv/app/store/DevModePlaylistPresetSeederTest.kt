package com.afterglowtv.app.store

import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DevModePlaylistPresetSeederTest {

    @Test
    fun `parser reads live and vod dev preset specs`() {
        val presets = parseDevModePlaylistPresetSpecs(
            "Live One::LIVE::LIVE::https://example.com/live.m3u8::https://example.com/live.xml" +
                "|Adult VOD::VOD::ADULT_VOD::https://example.com/vod.m3u8::https://example.com/vod.xml"
        )

        assertThat(presets).hasSize(2)
        assertThat(presets[0].name).isEqualTo("Live One")
        assertThat(presets[0].playlistKind).isEqualTo(ProviderM3uPlaylistKind.LIVE)
        assertThat(presets[0].sourceSlot).isEqualTo(ProviderSourceSlot.LIVE)
        assertThat(presets[0].playlistUrl).isEqualTo("https://example.com/live.m3u8")
        assertThat(presets[0].epgUrl).isEqualTo("https://example.com/live.xml")
        assertThat(presets[1].playlistKind).isEqualTo(ProviderM3uPlaylistKind.VOD)
        assertThat(presets[1].sourceSlot).isEqualTo(ProviderSourceSlot.ADULT_VOD)
    }

    @Test
    fun `parser skips malformed dev preset specs`() {
        val presets = parseDevModePlaylistPresetSpecs(
            "Missing Parts::LIVE" +
                "|Bad Kind::MOVIES::VOD::https://example.com/list.m3u8" +
                "|Good::VOD::VOD::https://example.com/vod.m3u8"
        )

        assertThat(presets.map { it.name }).containsExactly("Good")
    }
}
