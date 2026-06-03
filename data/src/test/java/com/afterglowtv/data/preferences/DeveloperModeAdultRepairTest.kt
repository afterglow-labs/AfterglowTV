package com.afterglowtv.data.preferences

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeveloperModeAdultRepairTest {
    @Test
    fun `developer mode repairs missing or disabled adult tab setting`() {
        assertThat(shouldRepairAdultTabVisibility(developerModeEnabled = true, showAdultTab = null))
            .isTrue()
        assertThat(shouldRepairAdultTabVisibility(developerModeEnabled = true, showAdultTab = false))
            .isTrue()
    }

    @Test
    fun `developer mode does not rewrite an already visible adult tab`() {
        assertThat(shouldRepairAdultTabVisibility(developerModeEnabled = true, showAdultTab = true))
            .isFalse()
    }

    @Test
    fun `locked developer mode does not expose adult tab`() {
        assertThat(shouldRepairAdultTabVisibility(developerModeEnabled = false, showAdultTab = null))
            .isFalse()
        assertThat(shouldRepairAdultTabVisibility(developerModeEnabled = false, showAdultTab = false))
            .isFalse()
    }

    @Test
    fun `developer mode state controls adult tab visibility`() {
        assertThat(adultTabVisibilityForDeveloperMode(enabled = true)).isTrue()
        assertThat(adultTabVisibilityForDeveloperMode(enabled = false)).isFalse()
    }
}
