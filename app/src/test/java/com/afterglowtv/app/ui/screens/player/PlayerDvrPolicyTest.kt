package com.afterglowtv.app.ui.screens.player

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerDvrPolicyTest {
    @Test
    fun `amazon review build hides player recording controls until developer mode unlock`() {
        assertThat(
            shouldShowPlayerRecordingControls(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).isFalse()
        assertThat(
            shouldShowPlayerRecordingControls(
                StorePolicySnapshot.amazon,
                developerModeEnabled = true
            )
        ).isTrue()
        assertThat(
            shouldShowPlayerRecordingControls(
                StorePolicySnapshot.standard,
                developerModeEnabled = false
            )
        ).isTrue()
    }
}
