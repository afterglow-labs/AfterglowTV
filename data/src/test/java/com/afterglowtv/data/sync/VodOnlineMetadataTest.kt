package com.afterglowtv.data.sync

import com.afterglowtv.data.local.entity.MovieEntity
import com.afterglowtv.data.local.entity.SeriesEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodOnlineMetadataTest {
    @Test
    fun `search seed strips provider release noise and extracts year`() {
        val seed = buildVodMetadataSearchSeed(
            rawTitle = "The.Matrix.1999.1080p.WEB-DL.x264",
            fallbackYear = null,
            categoryName = "Movies"
        )

        assertThat(seed.query).isEqualTo("The Matrix")
        assertThat(seed.year).isEqualTo("1999")
    }

    @Test
    fun `best match favors exact cleaned title and matching year`() {
        val best = selectBestCinemetaMatch(
            seed = VodMetadataSearchSeed(query = "The Matrix", year = "1999"),
            matches = listOf(
                CinemetaSearchMatch(
                    id = "tt0234215",
                    type = "movie",
                    name = "The Matrix Reloaded",
                    releaseInfo = "2003",
                    poster = null,
                    background = null
                ),
                CinemetaSearchMatch(
                    id = "tt0133093",
                    type = "movie",
                    name = "The Matrix",
                    releaseInfo = "1999",
                    poster = null,
                    background = null
                )
            )
        )

        assertThat(best?.id).isEqualTo("tt0133093")
    }

    @Test
    fun `weak online matches are rejected`() {
        val best = selectBestCinemetaMatch(
            seed = VodMetadataSearchSeed(query = "Friday Night Football", year = "2024"),
            matches = listOf(
                CinemetaSearchMatch(
                    id = "tt0133093",
                    type = "movie",
                    name = "The Matrix",
                    releaseInfo = "1999",
                    poster = null,
                    background = null
                )
            )
        )

        assertThat(best).isNull()
    }

    @Test
    fun `movie online metadata fills blanks without clobbering provider poster`() {
        val movie = MovieEntity(
            id = 7L,
            streamId = 70L,
            providerId = 3L,
            name = "The.Matrix.1999.1080p",
            posterUrl = "https://provider.example/poster.jpg",
            backdropUrl = null,
            rating = 0f,
            year = null
        )
        val enriched = mergeMovieOnlineMetadata(
            movie = movie,
            metadata = VodOnlineMetadata(
                id = "tt0133093",
                name = "The Matrix",
                posterUrl = "https://online.example/poster.jpg",
                backdropUrl = "https://online.example/backdrop.jpg",
                plot = "A hacker discovers reality is simulated.",
                cast = "Keanu Reeves, Laurence Fishburne",
                director = "Lana Wachowski, Lilly Wachowski",
                genre = "Action, Sci-Fi",
                releaseDate = "1999-03-31",
                duration = "136 min",
                rating = 8.7f,
                year = "1999",
                tmdbId = 603L,
                youtubeTrailer = "d0XTFAMmhrE"
            )
        )

        assertThat(enriched.posterUrl).isEqualTo("https://provider.example/poster.jpg")
        assertThat(enriched.backdropUrl).isEqualTo("https://online.example/backdrop.jpg")
        assertThat(enriched.plot).isEqualTo("A hacker discovers reality is simulated.")
        assertThat(enriched.rating).isEqualTo(8.7f)
        assertThat(enriched.year).isEqualTo("1999")
        assertThat(enriched.tmdbId).isEqualTo(603L)
    }

    @Test
    fun `series online metadata fills artwork and runtime fields`() {
        val series = SeriesEntity(
            id = 9L,
            seriesId = 90L,
            providerId = 3L,
            name = "Breaking.Bad.2008.720p",
            posterUrl = null,
            backdropUrl = null,
            episodeRunTime = null
        )
        val enriched = mergeSeriesOnlineMetadata(
            series = series,
            metadata = VodOnlineMetadata(
                id = "tt0903747",
                name = "Breaking Bad",
                posterUrl = "https://online.example/poster.jpg",
                backdropUrl = "https://online.example/backdrop.jpg",
                plot = "A chemistry teacher turns to crime.",
                cast = "Bryan Cranston, Aaron Paul",
                director = null,
                genre = "Crime, Drama",
                releaseDate = "2008-01-20",
                duration = "49 min",
                rating = 9.5f,
                year = "2008",
                tmdbId = 1396L,
                youtubeTrailer = "HhesaQXLuRY"
            )
        )

        assertThat(enriched.posterUrl).isEqualTo("https://online.example/poster.jpg")
        assertThat(enriched.backdropUrl).isEqualTo("https://online.example/backdrop.jpg")
        assertThat(enriched.episodeRunTime).isEqualTo("49 min")
        assertThat(enriched.tmdbId).isEqualTo(1396L)
    }
}
