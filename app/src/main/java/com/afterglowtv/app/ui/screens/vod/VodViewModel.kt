package com.afterglowtv.app.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.di.AuxiliaryPlayerEngine
import com.afterglowtv.app.ui.model.VodTitleFormatter
import com.afterglowtv.app.ui.model.VodViewMode
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.MovieRepository
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.player.PlaybackState
import com.afterglowtv.player.PlayerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Provider as InjectProvider

private const val INITIAL_VOD_PAGE_SIZE = 120
private const val VOD_PAGE_SIZE = 120
private const val NUMBER_CATEGORY_KEY = "#"
private const val VOD_PREVIEW_START_MS = 5 * 60 * 1_000L

data class VodAlphaCategory(
    val key: String,
    val label: String,
    val count: Int
)

data class VodBrowseSection(
    val key: String,
    val label: String,
    val count: Int,
    val items: List<Movie>
)

data class VodUiState(
    val provider: Provider? = null,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val viewMode: VodViewMode = VodViewMode.SHELVES,
    val selectedCategoryKey: String? = null,
    val categories: List<VodAlphaCategory> = emptyList(),
    val sections: List<VodBrowseSection> = emptyList(),
    val items: List<Movie> = emptyList(),
    val selectedItems: List<Movie> = emptyList(),
    val visibleItemCount: Int = 0,
    val totalItemCount: Int = 0,
    val canLoadMore: Boolean = false,
    val previewMovie: Movie? = null,
    val previewPlayerEngine: PlayerEngine? = null,
    val isPreviewLoading: Boolean = false,
    val previewErrorMessage: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class VodViewModel @Inject constructor(
    providerRepository: ProviderRepository,
    private val movieRepository: MovieRepository,
    private val preferencesRepository: PreferencesRepository,
    @AuxiliaryPlayerEngine
    private val playerEngineProvider: InjectProvider<PlayerEngine>
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val visibleLimit = MutableStateFlow(INITIAL_VOD_PAGE_SIZE)
    private val selectedCategoryKey = MutableStateFlow<String?>(null)
    private val selectedPreviewMovieId = MutableStateFlow<Long?>(null)
    private val previewPlayerState = MutableStateFlow(VodPreviewPlayerState())
    private var previewPlaybackJob: Job? = null
    private var previewErrorJob: Job? = null
    private var previewPlayerEngine: PlayerEngine? = null
    private var previewSessionVersion: Long = 0L

    private val activeVodProvider: Flow<Provider?> = combine(
        preferencesRepository.activeVodSource,
        providerRepository.getActiveProvider(),
        providerRepository.getProviders()
    ) { activeVodSource, activeProvider, providers ->
        selectVodProvider(
            activeVodSource = activeVodSource,
            activeProvider = activeProvider,
            providers = providers
        )
    }.distinctUntilChanged { old, new -> old?.id == new?.id }

    private val browserInputs = combine(
        searchQuery,
        visibleLimit,
        selectedCategoryKey,
        selectedPreviewMovieId,
        preferencesRepository.vodViewMode.map(VodViewMode::fromStorage)
    ) { query, limit, categoryKey, previewMovieId, viewMode ->
        VodBrowserInputs(
            searchQuery = query,
            visibleLimit = limit,
            selectedCategoryKey = categoryKey,
            previewMovieId = previewMovieId,
            viewMode = viewMode
        )
    }

    val uiState: StateFlow<VodUiState> = combine(
        activeVodProvider,
        browserInputs,
        previewPlayerState
    ) { provider, inputs, previewState ->
        VodQuery(provider, inputs, previewState)
    }.flatMapLatest { query ->
        val provider = query.provider
        if (provider == null) {
            flowOf(
                VodUiState(
                    provider = null,
                    isLoading = false,
                    searchQuery = query.inputs.searchQuery,
                    viewMode = query.inputs.viewMode,
                    selectedCategoryKey = query.inputs.selectedCategoryKey,
                    previewPlayerEngine = query.previewPlayerState.playerEngine,
                    isPreviewLoading = query.previewPlayerState.isLoading,
                    previewErrorMessage = query.previewPlayerState.errorMessage
                )
            )
        } else {
            combine(
                movieRepository.getMovies(provider.id),
                preferencesRepository.getHiddenCategoryIds(provider.id, ContentType.MOVIE)
            ) { movies, hiddenCategoryIds ->
                buildVodUiState(
                    provider = provider,
                    movies = movies,
                    hiddenCategoryIds = hiddenCategoryIds,
                    searchQuery = query.inputs.searchQuery,
                    visibleLimit = query.inputs.visibleLimit,
                    selectedPreviewMovieId = query.inputs.previewMovieId,
                    selectedCategoryKey = query.inputs.selectedCategoryKey,
                    viewMode = query.inputs.viewMode
                ).copy(
                    previewPlayerEngine = query.previewPlayerState.playerEngine,
                    isPreviewLoading = query.previewPlayerState.isLoading,
                    previewErrorMessage = query.previewPlayerState.errorMessage
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = VodUiState()
    )

    fun selectCategory(categoryKey: String) {
        selectedCategoryKey.value = categoryKey
        visibleLimit.value = INITIAL_VOD_PAGE_SIZE
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
        visibleLimit.value = INITIAL_VOD_PAGE_SIZE
    }

    fun loadMore() {
        visibleLimit.update { current -> current + VOD_PAGE_SIZE }
    }

    fun loadAll() {
        visibleLimit.value = Int.MAX_VALUE
    }

    fun previewMovie(movie: Movie) {
        if (selectedPreviewMovieId.value == movie.id && previewPlayerEngine != null) return

        selectedPreviewMovieId.value = movie.id
        val previewVersion = ++previewSessionVersion
        val engine = previewPlayerEngine ?: playerEngineProvider.get().also { previewPlayerEngine = it }
        previewPlaybackJob?.cancel()
        previewErrorJob?.cancel()
        previewPlayerState.value = VodPreviewPlayerState(
            playerEngine = engine,
            isLoading = true,
            errorMessage = null
        )

        previewPlaybackJob = viewModelScope.launch {
            engine.playbackState.collectLatest { playbackState ->
                if (!isActivePreviewSession(previewVersion, movie.id)) return@collectLatest
                previewPlayerState.update { state ->
                    state.copy(
                        isLoading = playbackState == PlaybackState.IDLE || playbackState == PlaybackState.BUFFERING,
                        errorMessage = when {
                            playbackState == PlaybackState.ERROR && state.errorMessage.isNullOrBlank() ->
                                "Preview failed"
                            playbackState != PlaybackState.ERROR -> null
                            else -> state.errorMessage
                        }
                    )
                }
            }
        }

        previewErrorJob = viewModelScope.launch {
            engine.error.collectLatest { error ->
                if (!isActivePreviewSession(previewVersion, movie.id)) return@collectLatest
                if (error != null) {
                    previewPlayerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message.ifBlank { "Preview failed" }
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            when (val result = movieRepository.getStreamInfo(movie)) {
                is Result.Success -> {
                    if (!isActivePreviewSession(previewVersion, movie.id)) return@launch
                    engine.stop()
                    engine.setDecoderMode(preferencesRepository.playerDecoderMode.first())
                    engine.setSurfaceMode(preferencesRepository.playerSurfaceMode.first())
                    engine.prepare(result.data)
                    engine.seekTo(VOD_PREVIEW_START_MS)
                    engine.setVolume(1f)
                    engine.play()
                }
                is Result.Error -> {
                    if (!isActivePreviewSession(previewVersion, movie.id)) return@launch
                    previewPlayerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun clearPreview() {
        if (previewPlayerEngine == null && selectedPreviewMovieId.value == null) return
        previewSessionVersion++
        previewPlaybackJob?.cancel()
        previewErrorJob?.cancel()
        previewPlaybackJob = null
        previewErrorJob = null
        previewPlayerEngine?.stop()
        previewPlayerEngine?.release()
        previewPlayerEngine = null
        selectedPreviewMovieId.value = null
        previewPlayerState.value = VodPreviewPlayerState()
    }

    override fun onCleared() {
        clearPreview()
        super.onCleared()
    }

    private fun isActivePreviewSession(version: Long, movieId: Long): Boolean =
        version == previewSessionVersion && selectedPreviewMovieId.value == movieId
}

private data class VodQuery(
    val provider: Provider?,
    val inputs: VodBrowserInputs,
    val previewPlayerState: VodPreviewPlayerState
)

private data class VodBrowserInputs(
    val searchQuery: String,
    val visibleLimit: Int,
    val selectedCategoryKey: String?,
    val previewMovieId: Long?,
    val viewMode: VodViewMode
)

private data class VodPreviewPlayerState(
    val playerEngine: PlayerEngine? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

internal fun buildVodUiState(
    provider: Provider,
    movies: List<Movie>,
    hiddenCategoryIds: Set<Long>,
    searchQuery: String,
    visibleLimit: Int,
    selectedPreviewMovieId: Long? = null,
    selectedCategoryKey: String? = null,
    viewMode: VodViewMode = VodViewMode.SHELVES
): VodUiState {
    val visibleMovies = movies
        .asSequence()
        .filterNot { movie -> movie.categoryId in hiddenCategoryIds }
        .toList()
    val normalizedQuery = searchQuery.trim()
    val filtered = visibleMovies
        .asSequence()
        .filter { movie ->
            normalizedQuery.isBlank() ||
                movie.name.contains(normalizedQuery, ignoreCase = true) ||
                movie.year?.contains(normalizedQuery, ignoreCase = true) == true
        }
        .sortedWith(vodMovieComparator())
        .toList()
    val categories = buildAlphabetCategories(filtered)
    val resolvedCategoryKey = selectedCategoryKey
        ?.takeIf { key -> categories.any { category -> category.key == key } }
        ?: categories.firstOrNull()?.key
    val limit = visibleLimit.coerceAtLeast(INITIAL_VOD_PAGE_SIZE)
    val items = filtered.take(limit)
    val sections = buildVodBrowseSections(categories, filtered, limit)
    val selectedItems = resolvedCategoryKey?.let { categoryKey ->
        filtered
            .filter { movie -> browseRangeForAlphabetKey(alphabetCategoryKey(movie.name)).key == categoryKey }
            .take(limit)
    }.orEmpty()
    val previewMovie = selectedPreviewMovieId?.let { movieId ->
        filtered.firstOrNull { movie -> movie.id == movieId }
    }
    val visibleCount = when (viewMode) {
        VodViewMode.SHELVES -> sections.sumOf { section -> section.items.size }
        VodViewMode.GUIDE -> selectedItems.size
        VodViewMode.GRID -> items.size
    }
    val totalCount = when (viewMode) {
        VodViewMode.GUIDE -> categories.firstOrNull { category -> category.key == resolvedCategoryKey }?.count ?: 0
        VodViewMode.SHELVES,
        VodViewMode.GRID -> filtered.size
    }
    val canLoadMore = when (viewMode) {
        VodViewMode.SHELVES -> sections.any { section -> section.items.size < section.count }
        VodViewMode.GUIDE -> selectedItems.size < totalCount
        VodViewMode.GRID -> items.size < filtered.size
    }

    return VodUiState(
        provider = provider,
        isLoading = false,
        searchQuery = searchQuery,
        viewMode = viewMode,
        selectedCategoryKey = resolvedCategoryKey,
        categories = categories,
        sections = sections,
        items = items,
        selectedItems = selectedItems,
        visibleItemCount = visibleCount,
        totalItemCount = totalCount,
        canLoadMore = canLoadMore,
        previewMovie = previewMovie
    )
}

internal fun buildAlphabetCategories(movies: List<Movie>): List<VodAlphaCategory> {
    if (movies.isEmpty()) return emptyList()
    val counts = movies
        .groupingBy { movie -> browseRangeForAlphabetKey(alphabetCategoryKey(movie.name)).key }
        .eachCount()
    return VOD_BROWSE_RANGES.mapNotNull { range ->
        counts[range.key]?.let { count ->
            VodAlphaCategory(range.key, range.label, count)
        }
    }
}

internal fun alphabetCategoryKey(title: String): String {
    val first = title.trim().firstOrNull { it.isLetterOrDigit() } ?: return NUMBER_CATEGORY_KEY
    return when {
        first.isLetter() -> first.uppercaseChar().toString()
        first.isDigit() -> NUMBER_CATEGORY_KEY
        else -> NUMBER_CATEGORY_KEY
    }
}

private fun vodMovieComparator(): Comparator<Movie> =
    compareBy<Movie> { movie ->
        VodTitleFormatter.format(movie.name, movie.year).title.lowercase(Locale.US)
    }.thenBy { movie ->
        movie.year.orEmpty()
    }.thenBy { movie ->
        movie.id
    }

private fun buildVodBrowseSections(
    categories: List<VodAlphaCategory>,
    items: List<Movie>,
    limitPerSection: Int
): List<VodBrowseSection> {
    if (categories.isEmpty() || items.isEmpty()) return emptyList()
    val itemsByRange = items.groupBy { movie -> browseRangeForAlphabetKey(alphabetCategoryKey(movie.name)).key }
    return categories.mapNotNull { category ->
        val sectionItems = itemsByRange[category.key].orEmpty()
        if (sectionItems.isEmpty()) {
            null
        } else {
            VodBrowseSection(
                key = category.key,
                label = category.label,
                count = category.count,
                items = sectionItems.take(limitPerSection)
            )
        }
    }
}

private data class VodBrowseRange(
    val key: String,
    val label: String,
    val firstLetter: Char?,
    val lastLetter: Char?
) {
    fun contains(alphabetKey: String): Boolean {
        if (firstLetter == null || lastLetter == null) return alphabetKey == NUMBER_CATEGORY_KEY
        val letter = alphabetKey.singleOrNull() ?: return false
        return letter in firstLetter..lastLetter
    }
}

private val VOD_BROWSE_RANGES = listOf(
    VodBrowseRange(NUMBER_CATEGORY_KEY, "#", firstLetter = null, lastLetter = null),
    VodBrowseRange("A-D", "A-D", firstLetter = 'A', lastLetter = 'D'),
    VodBrowseRange("E-H", "E-H", firstLetter = 'E', lastLetter = 'H'),
    VodBrowseRange("I-L", "I-L", firstLetter = 'I', lastLetter = 'L'),
    VodBrowseRange("M-P", "M-P", firstLetter = 'M', lastLetter = 'P'),
    VodBrowseRange("Q-T", "Q-T", firstLetter = 'Q', lastLetter = 'T'),
    VodBrowseRange("U-Z", "U-Z", firstLetter = 'U', lastLetter = 'Z')
)

private fun browseRangeForAlphabetKey(alphabetKey: String): VodBrowseRange =
    VOD_BROWSE_RANGES.firstOrNull { range -> range.contains(alphabetKey) } ?: VOD_BROWSE_RANGES.first()
