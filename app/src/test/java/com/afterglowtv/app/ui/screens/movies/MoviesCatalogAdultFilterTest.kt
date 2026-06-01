package com.afterglowtv.app.ui.screens.movies

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Movie
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoviesCatalogAdultFilterTest {
    @Test
    fun `normal movie categories exclude adult vod categories`() {
        val categories = listOf(
            Category(id = 1L, name = "Action", type = ContentType.MOVIE),
            Category(id = 2L, name = "XXX", type = ContentType.MOVIE, isAdult = true),
            Category(id = 3L, name = "XXX VOD", type = ContentType.MOVIE, isAdult = true)
        )

        assertThat(categories.withoutAdultVodCategories().map { it.name })
            .containsExactly("Action")
    }

    @Test
    fun `normal movie rows exclude adult vod rows by category and item metadata`() {
        val categories = listOf(
            Category(id = 1L, name = "Action", type = ContentType.MOVIE),
            Category(id = 2L, name = "XXX VOD", type = ContentType.MOVIE, isAdult = true)
        )
        val movies = listOf(
            Movie(id = 10L, name = "Space Movie", providerId = 7L, categoryId = 1L),
            Movie(id = 11L, name = "Generic Adult Title", providerId = 7L, categoryId = 2L),
            Movie(id = 12L, name = "Late Night", providerId = 7L, categoryName = "XXX", isAdult = true)
        )

        assertThat(movies.withoutAdultVodMovies(categories).map { it.id })
            .containsExactly(10L)
        assertThat(movies.onlyAdultVodMovies(categories).map { it.id })
            .containsExactly(11L, 12L)
    }
}
