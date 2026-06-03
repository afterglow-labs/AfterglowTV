package com.afterglowtv.app.ui.screens.home

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.repository.ChannelRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeAdultGuideVisibilityTest {

    @Test
    fun `adult guide can hide generated categories`() {
        val category = Category(
            id = -88001L,
            name = "Blondes",
            type = ContentType.LIVE,
            isVirtual = true,
            isAdult = true
        )

        assertThat(canHideHomeCategory(category, adultGuideMode = true)).isTrue()
    }

    @Test
    fun `regular live tv still protects virtual categories from provider hide`() {
        val category = Category(
            id = ChannelRepository.ALL_CHANNELS_ID,
            name = "All Channels",
            type = ContentType.LIVE,
            isVirtual = true
        )

        assertThat(canHideHomeCategory(category, adultGuideMode = false)).isFalse()
    }

    @Test
    fun `adult guide visibility removes hidden categories and their channels from every category`() {
        val context = AdultGuideLiveContext(
            categories = listOf(
                Category(id = 101L, name = "Blondes", type = ContentType.LIVE, isAdult = true),
                Category(id = 102L, name = "MILF", type = ContentType.LIVE, isAdult = true)
            ),
            channelIdsByCategoryId = linkedMapOf(
                101L to listOf(1L, 2L, 3L, 4L),
                102L to listOf(2L, 4L)
            )
        )

        val filtered = filterAdultGuideVisibility(
            context = context,
            hiddenCategoryIds = setOf(102L),
            hiddenChannelIds = setOf(2L)
        )

        assertThat(filtered.categories.map(Category::id)).containsExactly(101L)
        assertThat(filtered.channelIdsByCategoryId[101L]).containsExactly(1L, 3L).inOrder()
        assertThat(filtered.channelIdsByCategoryId).doesNotContainKey(102L)
    }
}
