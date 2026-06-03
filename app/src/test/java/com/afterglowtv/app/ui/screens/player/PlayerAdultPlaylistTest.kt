package com.afterglowtv.app.ui.screens.player

import com.afterglowtv.app.ui.model.adultGuideCategoryId
import com.afterglowtv.app.ui.model.adultGuideCachedChannelIdsForCategory
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.AdultGuideCachedCategory
import com.afterglowtv.domain.repository.AdultGuideCacheSnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerAdultGuidePlaylistTest {

    @Test
    fun `generated adult category playlist contains only matching category channels`() {
        val channels = listOf(
            Channel(id = 1L, name = "Blonde Live", isAdult = true),
            Channel(id = 2L, name = "Trans Live", isAdult = true),
            Channel(id = 3L, name = "Sports", isAdult = false)
        )

        val selected = adultGuideChannelsForPlaybackCategory(
            channels = channels,
            providerCategories = emptyList(),
            categoryId = adultGuideCategoryId("blondes")
        )

        assertThat(selected.map(Channel::id)).containsExactly(1L)
    }

    @Test
    fun `all adult playlist contains all adult channels`() {
        val channels = listOf(
            Channel(id = 1L, name = "Blonde Live", isAdult = true),
            Channel(id = 2L, name = "Trans Live", isAdult = true),
            Channel(id = 3L, name = "Sports", isAdult = false)
        )

        val selected = adultGuideChannelsForPlaybackCategory(
            channels = channels,
            providerCategories = emptyList(),
            categoryId = VirtualCategoryIds.ADULT_GUIDE
        )

        assertThat(selected.map(Channel::id)).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `cached adult category ids keep playback scoped to selected category`() {
        val cache = AdultGuideCacheSnapshot(
            providerId = 7L,
            playlistFingerprint = "provider:7:live:old",
            categorizedChannelCount = 3,
            categories = listOf(
                AdultGuideCachedCategory("blondes", "Blondes", listOf(1L, 2L)),
                AdultGuideCachedCategory("trans", "Trans", listOf(3L))
            )
        )

        val selectedIds = adultGuideCachedChannelIdsForCategory(
            cache = cache,
            categoryId = adultGuideCategoryId("blondes")
        )

        assertThat(selectedIds).containsExactly(1L, 2L).inOrder()
    }
}
