package com.afterglowtv.app.ui.screens.vod

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdultVodVisibilityTest {

    @Test
    fun `adult vod entry is hidden until developer mode is unlocked`() {
        assertThat(canShowAdultVodEntry(developerModeEnabled = false)).isFalse()
        assertThat(canShowAdultVodEntry(developerModeEnabled = true)).isTrue()
    }
}
