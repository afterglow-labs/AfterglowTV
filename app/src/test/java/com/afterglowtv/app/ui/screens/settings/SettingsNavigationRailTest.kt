package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsNavigationRailTest {
    @Test
    fun `amazon settings navigation hides recording and keeps later section ids stable`() {
        assertThat(
            visibleSettingsCategoryIds(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).containsExactly(
            SETTINGS_CATEGORY_PROVIDERS,
            SETTINGS_CATEGORY_PROVIDERS_VOD,
            SETTINGS_CATEGORY_PLAYBACK,
            SETTINGS_CATEGORY_BROWSING,
            SETTINGS_CATEGORY_PRIVACY,
            SETTINGS_CATEGORY_LOCAL_MEDIA,
            SETTINGS_CATEGORY_BACKUP,
            SETTINGS_CATEGORY_EPG_SOURCES,
            SETTINGS_CATEGORY_ABOUT
        ).inOrder()
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
