package com.afterglowtv.app.ui.screens.vod

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdultVodGuideVisibilityTest {

    @Test
    fun `adult vod guide entry is hidden until developer mode is unlocked`() {
        assertThat(canShowAdultVodGuideEntry(developerModeEnabled = false)).isFalse()
        assertThat(canShowAdultVodGuideEntry(developerModeEnabled = true)).isTrue()
    }
}
