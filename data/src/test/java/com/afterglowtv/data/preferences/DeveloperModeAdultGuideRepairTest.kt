package com.afterglowtv.data.preferences

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeveloperModeAdultGuideRepairTest {
    @Test
    fun `developer mode repairs missing or disabled adult guide tab setting`() {
        assertThat(shouldRepairAdultGuideTabVisibility(developerModeEnabled = true, showAdultGuideTab = null))
            .isTrue()
        assertThat(shouldRepairAdultGuideTabVisibility(developerModeEnabled = true, showAdultGuideTab = false))
            .isTrue()
    }

    @Test
    fun `developer mode does not rewrite an already visible adult guide tab`() {
        assertThat(shouldRepairAdultGuideTabVisibility(developerModeEnabled = true, showAdultGuideTab = true))
            .isFalse()
    }

    @Test
    fun `locked developer mode does not expose adult guide tab`() {
        assertThat(shouldRepairAdultGuideTabVisibility(developerModeEnabled = false, showAdultGuideTab = null))
            .isFalse()
        assertThat(shouldRepairAdultGuideTabVisibility(developerModeEnabled = false, showAdultGuideTab = false))
            .isFalse()
    }
}
