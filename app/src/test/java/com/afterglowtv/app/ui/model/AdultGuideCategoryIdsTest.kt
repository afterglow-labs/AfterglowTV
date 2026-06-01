package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.repository.AdultGuideCachedCategory
import com.afterglowtv.domain.repository.AdultGuideCacheSnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdultGuideCategoryIdsTest {

    @Test
    fun `generated adult category ids are distinct from generic virtual groups`() {
        val blondesId = adultGuideCategoryId("blondes")

        assertThat(blondesId).isNotEqualTo(VirtualCategoryIds.ADULT_GUIDE)
        assertThat(isAdultGuideGeneratedCategoryId(blondesId)).isTrue()
        assertThat(isAdultGuideGeneratedCategoryId(-42L)).isFalse()
    }

    @Test
    fun `cached adult category ids resolve only that category playlist`() {
        val cache = adultCache(
            AdultGuideCachedCategory("blondes", "Blondes", listOf(10L, 20L)),
            AdultGuideCachedCategory("trans", "Trans", listOf(30L))
        )

        assertThat(
            adultGuideCachedChannelIdsForCategory(
                cache = cache,
                categoryId = adultGuideCategoryId("blondes")
            )
        ).containsExactly(10L, 20L).inOrder()
    }

    @Test
    fun `all adult category resolves all cached channels without duplicates`() {
        val cache = adultCache(
            AdultGuideCachedCategory("blondes", "Blondes", listOf(10L, 20L)),
            AdultGuideCachedCategory("milf", "MILF", listOf(20L, 30L))
        )

        assertThat(
            adultGuideCachedChannelIdsForCategory(
                cache = cache,
                categoryId = VirtualCategoryIds.ADULT_GUIDE
            )
        ).containsExactly(10L, 20L, 30L).inOrder()
    }

    @Test
    fun `hidden generated adult categories remove shared channels from visible categories`() {
        val cartoonGay = Channel(id = 10L, name = "Cartoon Gay", providerId = 7L)
        val cartoonOnly = Channel(id = 20L, name = "Cartoon Solo", providerId = 7L)
        val categories = listOf(
            AdultGuideCategory("cartoon", "Cartoon", listOf(cartoonGay, cartoonOnly)),
            AdultGuideCategory("gay", "Gay", listOf(cartoonGay))
        )

        val filtered = filterAdultGuideCategoriesForHiddenIds(
            generatedCategories = categories,
            adultChannelIds = listOf(10L, 20L),
            hiddenCategoryIds = setOf(adultGuideCategoryId("gay"))
        )

        assertThat(filtered.allChannelIds).containsExactly(20L)
        assertThat(filtered.categories.map { it.title }).containsExactly("Cartoon")
        assertThat(filtered.categories.single().channelIds).containsExactly(20L)
    }

    @Test
    fun `non adult virtual category does not resolve from adult cache`() {
        val cache = adultCache(
            AdultGuideCachedCategory("blondes", "Blondes", listOf(10L, 20L))
        )

        assertThat(
            adultGuideCachedChannelIdsForCategory(
                cache = cache,
                categoryId = -42L
            )
        ).isNull()
    }

    private fun adultCache(
        vararg categories: AdultGuideCachedCategory
    ): AdultGuideCacheSnapshot = AdultGuideCacheSnapshot(
        providerId = 7L,
        playlistFingerprint = "provider:7:live:1234:3",
        categorizedChannelCount = 3,
        categories = categories.toList()
    )
}
