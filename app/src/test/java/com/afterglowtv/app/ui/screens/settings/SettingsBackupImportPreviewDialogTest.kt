package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.store.StorePolicySnapshot
import com.afterglowtv.domain.manager.BackupImportPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsBackupImportPreviewDialogTest {
    @Test
    fun `amazon backup import hides recording schedules until developer mode unlock`() {
        assertThat(
            shouldShowBackupRecordingSchedules(
                StorePolicySnapshot.amazon,
                developerModeEnabled = false
            )
        ).isFalse()
        assertThat(
            shouldShowBackupRecordingSchedules(
                StorePolicySnapshot.amazon,
                developerModeEnabled = true
            )
        ).isTrue()
        assertThat(
            shouldShowBackupRecordingSchedules(
                StorePolicySnapshot.fullFeature,
                developerModeEnabled = false
            )
        ).isTrue()
    }

    @Test
    fun `hidden dvr backup import strips recording schedules from plan`() {
        val plan = BackupImportPlan(importRecordingSchedules = true)

        assertThat(sanitizedBackupImportPlanForDvr(plan, showRecordingSchedules = false).importRecordingSchedules)
            .isFalse()
        assertThat(sanitizedBackupImportPlanForDvr(plan, showRecordingSchedules = true).importRecordingSchedules)
            .isTrue()
    }
}
