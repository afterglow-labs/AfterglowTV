package com.afterglowtv.app.ui.screens.epg

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.VirtualCategoryIds
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

    @Test
    fun `active provider empty guide keeps toolbar reachable on no channel count`() {
        val state = EpgUiState(
            currentProviderName = "Provider",
            channels = emptyList(),
            totalChannelCount = 0,
            error = null
        )

        assertThat(shouldRenderGuideChrome(state)).isTrue()
        assertThat(resolveGuideEmptyAction(state)).isEqualTo(GuideEmptyAction.Retry)
    }

    @Test
    fun `explicit adult category uses adult surface`() {
        val state = EpgUiState(
            selectedCategoryId = VirtualCategoryIds.ADULT,
            categories = listOf(Category(id = VirtualCategoryIds.ADULT, name = "Adult", isAdult = true, isVirtual = true)),
            channels = listOf(Channel(id = 1L, name = "MILF TV", providerId = 1L, categoryId = 10L))
        )

        assertThat(shouldUseAdult(state)).isTrue()
    }

    @Test
    fun `selected provider adult category does not replace normal epg`() {
        val state = EpgUiState(
            selectedCategoryId = 10L,
            categories = listOf(Category(id = 10L, name = "XXX", isAdult = true)),
            channels = listOf(Channel(id = 1L, name = "MILF TV", providerId = 1L, categoryId = 10L))
        )

        assertThat(shouldUseAdult(state)).isFalse()
    }

    @Test
    fun `all-adult provider lineup does not replace normal epg`() {
        val state = EpgUiState(
            selectedCategoryId = ChannelRepository.ALL_CHANNELS_ID,
            categories = listOf(Category(id = 10L, name = "XXX", isAdult = true)),
            channels = listOf(
                Channel(id = 1L, name = "MILF TV", providerId = 1L, categoryId = 10L, categoryName = "XXX"),
                Channel(id = 2L, name = "Interracial Live", providerId = 1L, categoryId = 10L, categoryName = "XXX")
            )
        )

        assertThat(shouldUseAdult(state)).isFalse()
    }

    @Test
    fun `adult title keyword lineup does not replace normal epg`() {
        val state = EpgUiState(
            selectedCategoryId = ChannelRepository.ALL_CHANNELS_ID,
            categories = listOf(Category(id = 10L, name = "Live")),
            channels = listOf(
                Channel(id = 1L, name = "MILF Live", providerId = 1L, categoryId = 10L, categoryName = "Live"),
                Channel(id = 2L, name = "Interracial 24-7", providerId = 1L, categoryId = 10L, categoryName = "Live")
            )
        )

        assertThat(shouldUseAdult(state)).isFalse()
    }

    @Test
    fun `mixed normal provider does not use adult surface`() {
        val state = EpgUiState(
            selectedCategoryId = ChannelRepository.ALL_CHANNELS_ID,
            categories = listOf(Category(id = 10L, name = "Sports")),
            channels = listOf(
                Channel(id = 1L, name = "ESPN", providerId = 1L, categoryId = 10L, categoryName = "Sports"),
                Channel(id = 2L, name = "News Live", providerId = 1L, categoryId = 10L, categoryName = "News")
            )
        )

        assertThat(shouldUseAdult(state)).isFalse()
    }

}
