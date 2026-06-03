package com.afterglowtv.app.ui.screens.vod

import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.app.ui.model.VodViewMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodSourceSelectionTest {
    @Test
    fun `active vod source wins over active live provider`() {
        val liveProvider = Provider(
            id = 1L,
            name = "Live",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/live.m3u"
        )
        val vodProvider = Provider(
            id = 2L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u",
            m3uPlaylistKind = ProviderM3uPlaylistKind.VOD
        )

        val selected = selectVodProvider(
            activeVodSource = ActiveLiveSource.ProviderSource(vodProvider.id),
            activeProvider = liveProvider,
            providers = listOf(liveProvider, vodProvider)
        )

        assertThat(selected).isEqualTo(vodProvider)
    }

    @Test
    fun `falls back to active provider when no vod source exists`() {
        val provider = Provider(
            id = 3L,
            name = "Portal",
            type = ProviderType.XTREAM_CODES,
            serverUrl = "https://example.com"
        )

        val selected = selectVodProvider(
            activeVodSource = null,
            activeProvider = provider,
            providers = listOf(provider)
        )

        assertThat(selected).isEqualTo(provider)
    }

    @Test
    fun `alphabet categories group letters into compact ranges without all`() {
        val categories = buildAlphabetCategories(
            listOf(
                Movie(id = 1L, name = "731"),
                Movie(id = 2L, name = "Adam"),
                Movie(id = 3L, name = "Drew"),
                Movie(id = 4L, name = "Echo"),
                Movie(id = 5L, name = "Hannah"),
                Movie(id = 6L, name = "Iris"),
                Movie(id = 7L, name = "Luna"),
                Movie(id = 8L, name = "Mom"),
                Movie(id = 9L, name = "Peach"),
                Movie(id = 10L, name = "Queen"),
                Movie(id = 11L, name = "Tango"),
                Movie(id = 12L, name = "Unknown"),
                Movie(id = 13L, name = "Zulu")
            )
        )

        assertThat(categories.map { it.label }).containsExactly("#", "A-D", "E-H", "I-L", "M-P", "Q-T", "U-Z").inOrder()
        assertThat(categories.associate { it.label to it.count })
            .containsExactly("#", 1, "A-D", 2, "E-H", 2, "I-L", 2, "M-P", 2, "Q-T", 2, "U-Z", 2)
    }

    @Test
    fun `vod state exposes loaded titles as range sections`() {
        val provider = Provider(
            id = 4L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u"
        )

        val state = buildVodUiState(
            provider = provider,
            movies = listOf(
                Movie(id = 1L, providerId = provider.id, name = "731"),
                Movie(id = 2L, providerId = provider.id, name = "Blonde Night"),
                Movie(id = 3L, providerId = provider.id, name = "Teen City")
            ),
            hiddenCategoryIds = emptySet(),
            searchQuery = "",
            visibleLimit = 120,
            selectedPreviewMovieId = 2L
        )

        assertThat(state.sections.map { it.label }).containsExactly("#", "A-D", "Q-T").inOrder()
        assertThat(state.sections.associate { it.label to it.items.map(Movie::name) })
            .containsExactly("#", listOf("731"), "A-D", listOf("Blonde Night"), "Q-T", listOf("Teen City"))
        assertThat(state.previewMovie?.name).isEqualTo("Blonde Night")
    }

    @Test
    fun `vod container defaults to movie vod without leaking tv vod entries`() {
        val provider = Provider(
            id = 6L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u"
        )

        val state = buildVodUiState(
            provider = provider,
            movies = listOf(
                Movie(id = 1L, providerId = provider.id, name = "Movie One", categoryName = "Movie VOD"),
                Movie(id = 2L, providerId = provider.id, name = "Show Episode", categoryName = "TV VOD"),
                Movie(id = 3L, providerId = provider.id, name = "Standalone", categoryName = "Studio")
            ),
            hiddenCategoryIds = emptySet(),
            searchQuery = "",
            visibleLimit = 120
        )

        assertThat(state.selectedContentKind).isEqualTo(VodContentKind.MOVIE)
        assertThat(state.contentTabs.map { it.label }).containsExactly("Movie VOD", "TV VOD").inOrder()
        assertThat(state.contentTabs.associate { it.kind to it.count })
            .containsExactly(VodContentKind.MOVIE, 2, VodContentKind.TV, 1)
        assertThat(state.items.map(Movie::name)).containsExactly("Movie One", "Standalone").inOrder()
        assertThat(state.sections.flatMap { it.items }.map(Movie::name))
            .doesNotContain("Show Episode")
    }

    @Test
    fun `vod container can switch to tv vod entries`() {
        val provider = Provider(
            id = 7L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u"
        )

        val state = buildVodUiState(
            provider = provider,
            movies = listOf(
                Movie(id = 1L, providerId = provider.id, name = "Movie One", categoryName = "Movie VOD"),
                Movie(id = 2L, providerId = provider.id, name = "Show Episode", categoryName = "TV VOD"),
                Movie(id = 3L, providerId = provider.id, name = "Another Show", categoryName = "TV VOD")
            ),
            hiddenCategoryIds = emptySet(),
            searchQuery = "",
            visibleLimit = 120,
            selectedContentKind = VodContentKind.TV
        )

        assertThat(state.selectedContentKind).isEqualTo(VodContentKind.TV)
        assertThat(state.items.map(Movie::name)).containsExactly("Another Show", "Show Episode").inOrder()
        assertThat(state.categories.map { it.label }).containsExactly("A-D", "Q-T").inOrder()
    }

    @Test
    fun `vod container layout scopes loaded titles to selected category range`() {
        val provider = Provider(
            id = 5L,
            name = "VOD",
            type = ProviderType.M3U,
            serverUrl = "https://example.com/vod.m3u"
        )

        val state = buildVodUiState(
            provider = provider,
            movies = listOf(
                Movie(id = 1L, providerId = provider.id, name = "Adam"),
                Movie(id = 2L, providerId = provider.id, name = "Drew"),
                Movie(id = 3L, providerId = provider.id, name = "Tango")
            ),
            hiddenCategoryIds = emptySet(),
            searchQuery = "",
            visibleLimit = 120,
            selectedCategoryKey = "A-D",
            viewMode = VodViewMode.CONTAINER
        )

        assertThat(state.viewMode).isEqualTo(VodViewMode.CONTAINER)
        assertThat(state.selectedItems.map(Movie::name)).containsExactly("Adam", "Drew").inOrder()
        assertThat(state.visibleItemCount).isEqualTo(2)
        assertThat(state.totalItemCount).isEqualTo(2)
        assertThat(state.canLoadMore).isFalse()
    }
}
