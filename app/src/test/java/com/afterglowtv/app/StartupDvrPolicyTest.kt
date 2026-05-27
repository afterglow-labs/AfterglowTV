package com.afterglowtv.app

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartupDvrPolicyTest {
    @Test
    fun `amazon review build enqueues recording maintenance only after developer mode unlock`() {
        assertThat(
            shouldEnqueueRecordingMaintenance(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).isFalse()
        assertThat(
            shouldEnqueueRecordingMaintenance(
                StorePolicySnapshot.amazon,
                developerModeEnabled = true
            )
        ).isTrue()
        assertThat(
            shouldEnqueueRecordingMaintenance(
                StorePolicySnapshot.standard,
                developerModeEnabled = false
            )
        ).isTrue()
    }
}
