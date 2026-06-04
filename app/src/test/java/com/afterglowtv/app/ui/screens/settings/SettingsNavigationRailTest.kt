package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsNavigationRailTest {
    @Test
    fun `locked amazon settings navigation exposes live tv providers`() {
        assertThat(
            visibleSettingsCategoryIds(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).containsExactly(SETTINGS_CATEGORY_PROVIDERS)
    }

    @Test
    fun `standard settings navigation includes recording`() {
        assertThat(visibleSettingsCategoryIds(StorePolicySnapshot.standard, developerModeEnabled = false))
            .contains(SETTINGS_CATEGORY_RECORDING)
    }

    @Test
    fun `amazon settings navigation restores recording after developer mode unlock`() {
        assertThat(visibleSettingsCategoryIds(StorePolicySnapshot.amazon, developerModeEnabled = true))
            .contains(SETTINGS_CATEGORY_RECORDING)
    }
}
