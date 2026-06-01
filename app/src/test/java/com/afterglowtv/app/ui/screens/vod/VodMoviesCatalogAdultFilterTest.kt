package com.afterglowtv.app.ui.screens.vod

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Movie
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodMoviesCatalogAdultFilterTest {
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

    @Test
    fun `movies mode excludes tv vod categories and tv mode keeps them`() {
        val categories = listOf(
            Category(id = 1L, name = "Movie VOD", type = ContentType.MOVIE),
            Category(id = 2L, name = "TV VOD", type = ContentType.MOVIE),
            Category(id = 3L, name = "Series VOD", type = ContentType.MOVIE)
        )

        assertThat(categories.filterForVodContainerMode(VodContainerMode.MOVIES).map { it.name })
            .containsExactly("Movie VOD")
        assertThat(categories.filterForVodContainerMode(VodContainerMode.TV).map { it.name })
            .containsExactly("TV VOD", "Series VOD")
            .inOrder()
    }

    @Test
    fun `movies mode excludes movie shaped tv vod rows and tv mode keeps them`() {
        val categories = listOf(
            Category(id = 1L, name = "Movie VOD", type = ContentType.MOVIE),
            Category(id = 2L, name = "TV VOD", type = ContentType.MOVIE)
        )
        val movies = listOf(
            Movie(id = 10L, name = "Feature", providerId = 7L, categoryId = 1L),
            Movie(id = 11L, name = "Episode One", providerId = 7L, categoryId = 2L),
            Movie(id = 12L, name = "Episode Two", providerId = 7L, categoryName = "TV VOD")
        )

        assertThat(movies.filterForVodContainerMode(VodContainerMode.MOVIES, categories).map { it.id })
            .containsExactly(10L)
        assertThat(movies.filterForVodContainerMode(VodContainerMode.TV, categories).map { it.id })
            .containsExactly(11L, 12L)
            .inOrder()
    }
}
