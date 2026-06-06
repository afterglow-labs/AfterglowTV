package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsPrivacySectionTest {
    @Test
    fun `amazon build hides advanced text import settings`() {
        assertThat(showAdvancedTextImportSettings(StorePolicySnapshot.amazon)).isFalse()
    }

    @Test
    fun `standard build shows advanced text import settings`() {
        assertThat(showAdvancedTextImportSettings(StorePolicySnapshot.fullFeature)).isTrue()
    }
}
