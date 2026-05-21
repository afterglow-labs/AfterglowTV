package com.afterglowtv.app.ui.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodPresentationTest {
    @Test
    fun vodViewModeKeepsLegacyStorageValuesCompatible() {
        assertThat(VodViewMode.fromStorage("modern")).isEqualTo(VodViewMode.SHELVES)
        assertThat(VodViewMode.fromStorage("classic")).isEqualTo(VodViewMode.GRID)
        assertThat(VodViewMode.fromStorage("guide")).isEqualTo(VodViewMode.GUIDE)
        assertThat(VodViewMode.fromStorage(null)).isEqualTo(VodViewMode.SHELVES)
    }

    @Test
    fun displayTitle_removesProviderNoiseButKeepsAdultTextVisible() {
        val formatted = VodTitleFormatter.format("HOT.MILF_COLLECTION.2024.1080p.WEB-DL.x264")

        assertThat(formatted.title).isEqualTo("Hot Milf Collection")
        assertThat(formatted.year).isEqualTo("2024")
    }

    @Test
    fun displayTitle_turnsBareUnderscoreNamesIntoReadableTitles() {
        val formatted = VodTitleFormatter.format("the_matrix_resurrections_2021_4k_hdr")

        assertThat(formatted.title).isEqualTo("The Matrix Resurrections")
        assertThat(formatted.year).isEqualTo("2021")
    }

    @Test
    fun shelfSectionsPreferProviderCategoriesThenGenresThenAlphabeticalFallback() {
        val sections = VodShelfSectionBuilder.build(
            items = listOf(
                VodShelfItem(id = "1", title = "Alien", providerCategory = "Sci-Fi", genres = listOf("Horror", "Sci-Fi")),
                VodShelfItem(id = "2", title = "Arrival", providerCategory = "Sci-Fi", genres = listOf("Sci-Fi")),
                VodShelfItem(id = "3", title = "Heat", providerCategory = null, genres = listOf("Crime", "Drama")),
                VodShelfItem(id = "4", title = "Casino", providerCategory = null, genres = emptyList()),
                VodShelfItem(id = "5", title = "Coda", providerCategory = "", genres = emptyList())
            ),
            minimumSectionSize = 2,
            maximumItemsPerSection = 12
        )

        assertThat(sections.map { it.title }).containsExactly("Sci-Fi", "C", "H").inOrder()
        assertThat(sections.first().items.map { it.title }).containsExactly("Alien", "Arrival").inOrder()
        assertThat(sections[1].items.map { it.title }).containsExactly("Casino", "Coda").inOrder()
        assertThat(sections.last().items.map { it.title }).containsExactly("Heat")
    }

    @Test
    fun guideRowsPreserveWholeGuideSortOrderInsteadOfSortingInsideEachRow() {
        val rows = VodGuideRowBuilder.build(
            items = listOf(
                VodGuideItem(id = "1", title = "Alpha", providerCategory = "Drama"),
                VodGuideItem(id = "2", title = "Bravo", providerCategory = "Action"),
                VodGuideItem(id = "3", title = "Zulu", providerCategory = "Drama"),
                VodGuideItem(id = "4", title = "Omega", providerCategory = null)
            ),
            uncategorizedTitle = "Other"
        )

        assertThat(rows.map { it.title }).containsExactly("Drama", "Action", "Other").inOrder()
        assertThat(rows.first().items.map { it.title }).containsExactly("Alpha", "Zulu").inOrder()
    }
}
