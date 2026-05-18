package com.afterglowtv.app.ui.screens.epg

import com.afterglowtv.domain.repository.ChannelRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EpgScreenStateTest {

    @Test
    fun `filtered empty guide keeps toolbar reachable instead of showing retry only`() {
        val state = EpgUiState(
            channels = emptyList(),
            totalChannelCount = 42,
            showScheduledOnly = true,
            selectedChannelMode = GuideChannelMode.ARCHIVE_READY,
            error = null
        )

        assertThat(shouldRenderGuideChrome(state)).isTrue()
        assertThat(resolveGuideEmptyAction(state)).isEqualTo(GuideEmptyAction.ResetFilters)
    }

    @Test
    fun `true provider error still uses full screen retry state`() {
        val state = EpgUiState(
            channels = emptyList(),
            totalChannelCount = 0,
            error = "network failed"
        )

        assertThat(shouldRenderGuideChrome(state)).isFalse()
        assertThat(resolveGuideEmptyAction(state)).isEqualTo(GuideEmptyAction.Retry)
    }

    @Test
    fun `category empty guide keeps toolbar reachable so category can change`() {
        val state = EpgUiState(
            channels = emptyList(),
            totalChannelCount = 0,
            selectedCategoryId = 10L,
            categories = listOf(com.afterglowtv.domain.model.Category(id = ChannelRepository.ALL_CHANNELS_ID, name = "All"))
        )

        assertThat(shouldRenderGuideChrome(state)).isTrue()
        assertThat(resolveGuideEmptyAction(state)).isEqualTo(GuideEmptyAction.ResetFilters)
    }
}
