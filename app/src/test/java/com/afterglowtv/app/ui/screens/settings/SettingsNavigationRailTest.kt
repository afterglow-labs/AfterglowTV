package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsNavigationRailTest {
    @Test
    fun `locked amazon settings navigation keeps normal app settings`() {
        assertThat(
            visibleSettingsCategoryIds(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).containsExactly(
            SETTINGS_CATEGORY_PROVIDERS,
            SETTINGS_CATEGORY_PLAYBACK,
            SETTINGS_CATEGORY_BROWSING,
            SETTINGS_CATEGORY_PRIVACY,
            SETTINGS_CATEGORY_BACKUP,
            SETTINGS_CATEGORY_EPG_SOURCES,
            SETTINGS_CATEGORY_ABOUT
        ).inOrder()
    }

    @Test
    fun `locked amazon settings navigation still hides gated feature settings`() {
        assertThat(
            visibleSettingsCategoryIds(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).containsNoneOf(
            SETTINGS_CATEGORY_PROVIDERS_VOD,
            SETTINGS_CATEGORY_RECORDING,
            SETTINGS_CATEGORY_LOCAL_MEDIA
        )
    }

    @Test
    fun `standard settings navigation includes recording`() {
        assertThat(visibleSettingsCategoryIds(StorePolicySnapshot.fullFeature, developerModeEnabled = false))
            .contains(SETTINGS_CATEGORY_RECORDING)
    }

    @Test
    fun `amazon settings navigation restores recording after developer mode unlock`() {
        assertThat(visibleSettingsCategoryIds(StorePolicySnapshot.amazon, developerModeEnabled = true))
            .contains(SETTINGS_CATEGORY_RECORDING)
    }
}
