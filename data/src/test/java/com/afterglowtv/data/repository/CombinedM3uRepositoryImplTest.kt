package com.afterglowtv.data.repository

import com.afterglowtv.domain.model.Channel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CombinedM3uRepositoryImplTest {

    @Test
    fun `dedupeCombinedChannels keeps first priority channel for duplicate stream url`() {
        val primary = Channel(
            id = 1L,
            name = "News HD",
            providerId = 10L,
            streamUrl = "http://example.test/live/news.m3u8"
        )
        val duplicate = Channel(
            id = 2L,
            name = "News HD Copy",
            providerId = 20L,
            streamUrl = " http://example.test/live/news.m3u8 "
        )
        val other = Channel(
            id = 3L,
            name = "Movies HD",
            providerId = 20L,
            streamUrl = "http://example.test/live/movies.m3u8"
        )

        val deduped = dedupeCombinedChannels(listOf(primary, duplicate, other))

        assertThat(deduped).isEqualTo(listOf(primary, other))
    }

    @Test
    fun `dedupeCombinedChannels falls back to normalized channel identity without stream url`() {
        val primary = Channel(
            id = 1L,
            name = "Local News",
            providerId = 10L,
            categoryName = "USA"
        )
        val duplicate = Channel(
            id = 2L,
            name = " local   news ",
            providerId = 20L,
            categoryName = "USA"
        )
        val sameNameDifferentGroup = Channel(
            id = 3L,
            name = "Local News",
            providerId = 20L,
            categoryName = "Canada"
        )

        val deduped = dedupeCombinedChannels(listOf(primary, duplicate, sameNameDifferentGroup))

        assertThat(deduped).isEqualTo(listOf(primary, sameNameDifferentGroup))
    }
}
