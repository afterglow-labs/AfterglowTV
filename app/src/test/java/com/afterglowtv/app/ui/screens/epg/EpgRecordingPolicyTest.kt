package com.afterglowtv.app.ui.screens.epg

import com.afterglowtv.app.store.StorePolicySnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EpgRecordingPolicyTest {
    @Test
    fun `amazon review build hides guide recording actions until developer mode unlock`() {
        assertThat(
            shouldShowGuideRecordingActions(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false,
                canScheduleRecording = true
            )
        ).isFalse()
        assertThat(
            shouldShowGuideRecordingActions(
                StorePolicySnapshot.amazon,
                developerModeEnabled = true,
                canScheduleRecording = true
            )
        ).isTrue()
        assertThat(
            shouldShowGuideRecordingActions(
                StorePolicySnapshot.fullFeature,
                developerModeEnabled = false,
                canScheduleRecording = true
            )
        )
            .isTrue()
    }

    @Test
    fun `guide recording actions still require schedulable guide data`() {
        assertThat(
            shouldShowGuideRecordingActions(
                StorePolicySnapshot.fullFeature,
                developerModeEnabled = false,
                canScheduleRecording = false
            )
        )
            .isFalse()
    }
}
