package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.AdultCachedCategory
import com.afterglowtv.domain.repository.AdultCacheSnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdultCategoryIdsTest {

    @Test
    fun `generated adult category ids are distinct from generic virtual groups`() {
        val blondesId = adultCategoryId("blondes")

        assertThat(blondesId).isNotEqualTo(VirtualCategoryIds.ADULT)
        assertThat(isAdultGeneratedCategoryId(blondesId)).isTrue()
        assertThat(isAdultGeneratedCategoryId(-42L)).isFalse()
    }

    @Test
    fun `cached adult category ids resolve only that category playlist`() {
        val cache = adultCache(
            AdultCachedCategory("blondes", "Blondes", listOf(10L, 20L)),
            AdultCachedCategory("trans", "Trans", listOf(30L))
        )

        assertThat(
            adultCachedChannelIdsForCategory(
                cache = cache,
                categoryId = adultCategoryId("blondes")
            )
        ).containsExactly(10L, 20L).inOrder()
    }

    @Test
    fun `all adult category resolves all cached channels without duplicates`() {
        val cache = adultCache(
            AdultCachedCategory("blondes", "Blondes", listOf(10L, 20L)),
            AdultCachedCategory("milf", "MILF", listOf(20L, 30L))
        )

        assertThat(
            adultCachedChannelIdsForCategory(
                cache = cache,
                categoryId = VirtualCategoryIds.ADULT
            )
        ).containsExactly(10L, 20L, 30L).inOrder()
    }

    @Test
    fun `non adult virtual category does not resolve from adult cache`() {
        val cache = adultCache(
            AdultCachedCategory("blondes", "Blondes", listOf(10L, 20L))
        )

        assertThat(
            adultCachedChannelIdsForCategory(
                cache = cache,
                categoryId = -42L
            )
        ).isNull()
    }

    private fun adultCache(
        vararg categories: AdultCachedCategory
    ): AdultCacheSnapshot = AdultCacheSnapshot(
        providerId = 7L,
        playlistFingerprint = "provider:7:live:1234:3",
        categorizedChannelCount = 3,
        categories = categories.toList()
    )
}
