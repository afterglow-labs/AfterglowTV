package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdultGuidePresentationTest {

    @Test
    fun `generated adult categories include every title keyword match`() {
        val channel = Channel(
            id = 1L,
            name = "Blonde Trans MILF Cousin 24-7",
            providerId = 7L,
            categoryName = "XXX"
        )

        val categories = AdultGuideCategoryBuilder.build(
            channels = listOf(channel),
            providerCategories = emptyList()
        )

        assertThat(categories.map { it.title }).containsAtLeast(
            "All",
            "MILF",
            "Blondes",
            "Trans",
            "Taboo"
        )
        assertThat(categories.category("MILF").channels).containsExactly(channel)
        assertThat(categories.category("Blondes").channels).containsExactly(channel)
        assertThat(categories.category("Trans").channels).containsExactly(channel)
        assertThat(categories.category("Taboo").channels).containsExactly(channel)
    }

    @Test
    fun `adult category builder can omit all bucket until full guide load is complete`() {
        val channel = Channel(
            id = 1L,
            name = "Blonde Trans MILF Cousin 24-7",
            providerId = 7L,
            categoryName = "XXX"
        )

        val categories = AdultGuideCategoryBuilder.build(
            channels = listOf(channel),
            providerCategories = emptyList(),
            includeAllCategory = false
        )

        assertThat(categories.map { it.title }).doesNotContain("All")
        assertThat(categories.category("MILF").channels).containsExactly(channel)
    }

    @Test
    fun `broad adult sorting labels are not automatically explicit adult categories`() {
        assertThat(isLikelyAdultGuideCategory(Category(id = 1L, name = "MILF"))).isTrue()
        assertThat(isLikelyAdultGuideCategory(Category(id = 2L, name = "Family"))).isFalse()
        assertThat(isLikelyAdultGuideCategory(Category(id = 3L, name = "Asian"))).isFalse()
        assertThat(isLikelyAdultGuideCategory(Category(id = 4L, name = "Reality"))).isFalse()
        assertThat(isLikelyAdultGuideCategory(Category(id = 5L, name = "4K"))).isFalse()
    }

    @Test
    fun `adult vod category prefers title match over generic adult group`() {
        assertThat(
            AdultGuideCategoryBuilder.resolveVodCategoryTitle(
                title = "Some MILF Scene",
                providerCategory = "XXX Movies"
            )
        ).isEqualTo("MILF")
    }

    @Test
    fun `adult vod category falls back to adult provider group for generic title`() {
        assertThat(
            AdultGuideCategoryBuilder.resolveVodCategoryTitle(
                title = "Unknown Clip",
                providerCategory = "XXX Movies"
            )
        ).isEqualTo("XXX Movies")
    }

    @Test
    fun `adult category builder uses provider category context when title is generic`() {
        val channel = Channel(
            id = 2L,
            name = "Channel 24-7",
            providerId = 7L,
            categoryId = 10L,
            categoryName = "Trans"
        )

        val categories = AdultGuideCategoryBuilder.build(
            channels = listOf(channel),
            providerCategories = listOf(Category(id = 10L, name = "Trans", isAdult = true))
        )

        assertThat(categories.category("Trans").channels).containsExactly(channel)
    }

    @Test
    fun `unmatched adult channels use provider category before other`() {
        val channel = Channel(
            id = 3L,
            name = "Channel 888",
            providerId = 7L,
            categoryId = 30L,
            categoryName = "XXX",
            isAdult = true
        )

        val categories = AdultGuideCategoryBuilder.build(
            channels = listOf(channel),
            providerCategories = listOf(Category(id = 30L, name = "Adult Premium", isAdult = true))
        )

        assertThat(categories.category("Adult Premium").channels).containsExactly(channel)
    }

    private fun List<AdultGuideCategory>.category(title: String): AdultGuideCategory =
        single { it.title == title }
}
