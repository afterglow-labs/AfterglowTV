package com.afterglowtv.app.ui.screens.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderSetupLayoutTest {

    @Test
    fun `phone setup actions are placed inside scrollable form content`() {
        assertThat(
            shouldInlineProviderSetupActions(
                isTelevisionDevice = false,
                viewportHeightDp = 420
            )
        ).isTrue()
    }

    @Test
    fun `normal tv setup actions stay in the sticky action area`() {
        assertThat(
            shouldInlineProviderSetupActions(
                isTelevisionDevice = true,
                viewportHeightDp = 720
            )
        ).isFalse()
    }

    @Test
    fun `phone landscape keeps the compact source tabs instead of the tv side rail`() {
        assertThat(
            shouldUseProviderSetupSideRail(
                isTelevisionDevice = false,
                viewportWidthDp = 900
            )
        ).isFalse()
    }

    @Test
    fun `wide tv setup keeps the side rail source selector`() {
        assertThat(
            shouldUseProviderSetupSideRail(
                isTelevisionDevice = true,
                viewportWidthDp = 900
            )
        ).isTrue()
    }
}
