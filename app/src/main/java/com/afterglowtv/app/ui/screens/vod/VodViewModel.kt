package com.afterglowtv.app.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.ui.model.VodTitleFormatter
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.repository.MovieRepository
import com.afterglowtv.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private const val INITIAL_VOD_PAGE_SIZE = 120
private const val VOD_PAGE_SIZE = 120
private const val ALL_CATEGORY_KEY = "__all__"
private const val NUMBER_CATEGORY_KEY = "#"

data class VodAlphaCategory(
    val key: String,
    val label: String,
    val count: Int
)

data class VodUiState(
    val provider: Provider? = null,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategoryKey: String = ALL_CATEGORY_KEY,
    val categories: List<VodAlphaCategory> = emptyList(),
    val items: List<Movie> = emptyList(),
    val visibleItemCount: Int = 0,
    val totalItemCount: Int = 0,
    val canLoadMore: Boolean = false
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class VodViewModel @Inject constructor(
    providerRepository: ProviderRepository,
    private val movieRepository: MovieRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedCategoryKey = MutableStateFlow(ALL_CATEGORY_KEY)
    private val visibleLimit = MutableStateFlow(INITIAL_VOD_PAGE_SIZE)

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

    val uiState: StateFlow<VodUiState> = combine(
        activeVodProvider,
        searchQuery,
        selectedCategoryKey,
        visibleLimit
    ) { provider, query, categoryKey, limit ->
        VodQuery(provider, query, categoryKey, limit)
    }.flatMapLatest { query ->
        val provider = query.provider
        if (provider == null) {
            flowOf(
                VodUiState(
                    provider = null,
                    isLoading = false,
                    searchQuery = query.searchQuery,
                    selectedCategoryKey = query.categoryKey
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
                    searchQuery = query.searchQuery,
                    selectedCategoryKey = query.categoryKey,
                    visibleLimit = query.visibleLimit
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
}

private data class VodQuery(
    val provider: Provider?,
    val searchQuery: String,
    val categoryKey: String,
    val visibleLimit: Int
)

internal fun buildVodUiState(
    provider: Provider,
    movies: List<Movie>,
    hiddenCategoryIds: Set<Long>,
    searchQuery: String,
    selectedCategoryKey: String,
    visibleLimit: Int
): VodUiState {
    val visibleMovies = movies
        .asSequence()
        .filterNot { movie -> movie.categoryId in hiddenCategoryIds }
        .toList()
    val categories = buildAlphabetCategories(visibleMovies)
    val selectedKey = selectedCategoryKey.takeIf { key ->
        key == ALL_CATEGORY_KEY || categories.any { category -> category.key == key }
    } ?: ALL_CATEGORY_KEY
    val normalizedQuery = searchQuery.trim()
    val filtered = visibleMovies
        .asSequence()
        .filter { movie -> selectedKey == ALL_CATEGORY_KEY || alphabetCategoryKey(movie.name) == selectedKey }
        .filter { movie ->
            normalizedQuery.isBlank() ||
                movie.name.contains(normalizedQuery, ignoreCase = true) ||
                movie.year?.contains(normalizedQuery, ignoreCase = true) == true
        }
        .sortedWith(vodMovieComparator())
        .toList()
    val limit = visibleLimit.coerceAtLeast(INITIAL_VOD_PAGE_SIZE)
    val items = filtered.take(limit)

    return VodUiState(
        provider = provider,
        isLoading = false,
        searchQuery = searchQuery,
        selectedCategoryKey = selectedKey,
        categories = categories,
        items = items,
        visibleItemCount = items.size,
        totalItemCount = filtered.size,
        canLoadMore = items.size < filtered.size
    )
}

internal fun buildAlphabetCategories(movies: List<Movie>): List<VodAlphaCategory> {
    if (movies.isEmpty()) return emptyList()
    val counts = movies.groupingBy { movie -> alphabetCategoryKey(movie.name) }.eachCount()
    val categoryKeys = buildList {
        if (counts.containsKey(NUMBER_CATEGORY_KEY)) add(NUMBER_CATEGORY_KEY)
        ('A'..'Z').forEach { letter ->
            val key = letter.toString()
            if (counts.containsKey(key)) add(key)
        }
    }
    return buildList {
        add(VodAlphaCategory(ALL_CATEGORY_KEY, "All", movies.size))
        categoryKeys.forEach { key ->
            add(VodAlphaCategory(key, key, counts[key] ?: 0))
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
