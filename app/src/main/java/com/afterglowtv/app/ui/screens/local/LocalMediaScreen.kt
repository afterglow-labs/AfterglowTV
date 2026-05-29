package com.afterglowtv.app.ui.screens.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.AppMessageState
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.components.shell.VodActionChip
import com.afterglowtv.app.ui.components.shell.VodActionChipRow
import com.afterglowtv.app.ui.model.isAdultLocalMediaItem
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.model.LocalMediaBrowseResult
import com.afterglowtv.domain.model.LocalMediaFolderEntry
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.LocalMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

enum class LocalMediaSection {
    ALL,
    MOVIES,
    TV_SHOWS,
    OTHER,
    XXX
}

data class LocalMediaUiState(
    val libraries: List<LocalMediaLibrary> = emptyList(),
    val visibleLibraries: List<LocalMediaLibrary> = emptyList(),
    val selectedSection: LocalMediaSection = LocalMediaSection.ALL,
    val selectedLibraryId: Long? = null,
    val selectedLibraryName: String? = null,
    val currentPath: String = "",
    val folders: List<LocalMediaFolderEntry> = emptyList(),
    val developerModeEnabled: Boolean = false,
    val items: List<LocalMediaItem> = emptyList(),
    val sectionCounts: Map<LocalMediaSection, Int> = emptyMap(),
    val totalItemCount: Int = 0,
    val loadedItemCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canLoadMore: Boolean get() = loadedItemCount < totalItemCount
    val isBrowsingLibrary: Boolean get() = selectedLibraryId != null
}

private data class LocalMediaBrowseUiState(
    val libraryId: Long? = null,
    val path: String = "",
    val result: LocalMediaBrowseResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LocalMediaViewModel @Inject constructor(
    private val localMediaRepository: LocalMediaRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val selectedSection = MutableStateFlow(LocalMediaSection.ALL)
    private val pageLimit = MutableStateFlow(LOCAL_MEDIA_PAGE_SIZE)
    private val browseState = MutableStateFlow(LocalMediaBrowseUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LocalMediaUiState> = combine(
        localMediaRepository.observeLibraries(),
        selectedSection,
        preferencesRepository.developerModeEnabled,
        pageLimit,
        browseState
    ) { libraries, selectedSection, developerModeEnabled, limit, browse ->
        val section = selectedSection.takeIf { it != LocalMediaSection.XXX || developerModeEnabled }
            ?: LocalMediaSection.ALL
        val libraryGroups = LocalMediaLibraryGroups.from(libraries, developerModeEnabled)
        val librariesById = libraries.associateBy(LocalMediaLibrary::id)
        val sectionLibraryIds = section.libraryIdsForQuery(libraryGroups)
        val visibleLibraries = libraries
            .filter { library -> sectionLibraryIds == null || library.id in sectionLibraryIds }
            .filter { library -> developerModeEnabled || !library.looksAdult() }
        val browseResult = browse.result?.takeIf { it.library.id == browse.libraryId }
        val visibleItems = browseResult
            ?.items
            ?.let { items -> visibleLocalMediaItems(items, section, developerModeEnabled, librariesById) }
            .orEmpty()
            .take(limit)
        LocalMediaUiState(
            libraries = libraries,
            visibleLibraries = visibleLibraries,
            selectedSection = section,
            selectedLibraryId = browse.libraryId,
            selectedLibraryName = browse.libraryId?.let { librariesById[it]?.displayName ?: librariesById[it]?.name },
            currentPath = browse.path,
            folders = browseResult?.folders.orEmpty(),
            developerModeEnabled = developerModeEnabled,
            items = visibleItems,
            sectionCounts = librarySectionCounts(libraries, libraryGroups),
            totalItemCount = browseResult?.items?.size ?: visibleLibraries.size,
            loadedItemCount = visibleItems.size,
            isLoading = browse.isLoading,
            errorMessage = browse.errorMessage
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalMediaUiState()
    )

    fun selectSection(section: LocalMediaSection) {
        selectedSection.value = section
        pageLimit.value = LOCAL_MEDIA_PAGE_SIZE
        browseState.value = LocalMediaBrowseUiState()
    }

    fun loadMore() {
        pageLimit.value = (pageLimit.value + LOCAL_MEDIA_PAGE_SIZE).coerceAtMost(LOCAL_MEDIA_MAX_PAGE_LIMIT)
    }

    fun openLibrary(libraryId: Long) {
        browse(libraryId, "")
    }

    fun openFolder(path: String) {
        val libraryId = browseState.value.libraryId ?: return
        browse(libraryId, path)
    }

    fun navigateUp() {
        val current = browseState.value
        val libraryId = current.libraryId ?: return
        if (current.path.isBlank()) {
            browseState.value = LocalMediaBrowseUiState()
            return
        }
        browse(libraryId, current.path.substringBeforeLast('/', missingDelimiterValue = ""))
    }

    private fun browse(libraryId: Long, path: String) {
        val normalizedPath = path.trim('/')
        pageLimit.value = LOCAL_MEDIA_PAGE_SIZE
        browseState.value = LocalMediaBrowseUiState(
            libraryId = libraryId,
            path = normalizedPath,
            isLoading = true
        )
        viewModelScope.launch {
            when (val result = localMediaRepository.browseLibrary(libraryId, normalizedPath)) {
                is Result.Success -> browseState.value = LocalMediaBrowseUiState(
                    libraryId = libraryId,
                    path = normalizedPath,
                    result = result.data,
                    isLoading = false
                )
                is Result.Error -> browseState.value = LocalMediaBrowseUiState(
                    libraryId = libraryId,
                    path = normalizedPath,
                    isLoading = false,
                    errorMessage = result.message
                )
                Result.Loading -> Unit
            }
        }
    }
}

@Composable
fun LocalMediaScreen(
    onPlayItem: (LocalMediaItem) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: LocalMediaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreenScaffold(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = stringResource(R.string.nav_personal_guide),
        subtitle = null,
        navigationChrome = AppNavigationChrome.TopBar,
        compactHeader = true,
        showScreenHeader = false
    ) {
        AfterglowBrandStrip(
            wordmark = stringResource(R.string.nav_personal_guide),
            tagline = stringResource(R.string.local_media_subtitle),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.local_media_loading),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            uiState.libraries.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppMessageState(
                        title = stringResource(R.string.local_media_empty_title),
                        subtitle = stringResource(R.string.local_media_empty_subtitle),
                        action = {
                            Button(onClick = { onNavigate(Routes.SETTINGS) }) {
                                Text(stringResource(R.string.local_media_open_settings))
                            }
                        }
                    )
                }
            }
            else -> {
                LocalMediaLibraryContent(
                    uiState = uiState,
                    onSectionSelected = viewModel::selectSection,
                    onLibrarySelected = viewModel::openLibrary,
                    onFolderSelected = viewModel::openFolder,
                    onNavigateUp = viewModel::navigateUp,
                    onLoadMore = viewModel::loadMore,
                    onPlayItem = onPlayItem
                )
            }
        }
    }
}

@Composable
private fun LocalMediaLibraryContent(
    uiState: LocalMediaUiState,
    onSectionSelected: (LocalMediaSection) -> Unit,
    onLibrarySelected: (Long) -> Unit,
    onFolderSelected: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayItem: (LocalMediaItem) -> Unit
) {
    val libraryById = remember(uiState.libraries) { uiState.libraries.associateBy(LocalMediaLibrary::id) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VodActionChipRow(
            actions = visibleLocalMediaSections(uiState.developerModeEnabled).map { section ->
                VodActionChip(
                    key = section.name,
                    label = stringResource(section.labelResId()),
                    detail = section.detail(uiState.sectionCounts[section] ?: 0),
                    onClick = { onSectionSelected(section) }
                )
            },
            selectedKey = uiState.selectedSection.name,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        if (uiState.isBrowsingLibrary) {
            LocalMediaFolderBrowser(
                uiState = uiState,
                libraryById = libraryById,
                onNavigateUp = onNavigateUp,
                onFolderSelected = onFolderSelected,
                onPlayItem = onPlayItem,
                onLoadMore = onLoadMore
            )
        } else if (uiState.visibleLibraries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppMessageState(
                    title = stringResource(R.string.local_media_no_results_title),
                    subtitle = stringResource(R.string.local_media_no_results_subtitle)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.visibleLibraries, key = { it.id }) { library ->
                    LocalMediaLibraryRow(
                        library = library,
                        onClick = { onLibrarySelected(library.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalMediaFolderBrowser(
    uiState: LocalMediaUiState,
    libraryById: Map<Long, LocalMediaLibrary>,
    onNavigateUp: () -> Unit,
    onFolderSelected: (String) -> Unit,
    onPlayItem: (LocalMediaItem) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "local_media_up") {
            Button(
                onClick = onNavigateUp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (uiState.currentPath.isBlank()) "All shares" else "Back",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = listOfNotNull(uiState.selectedLibraryName, uiState.currentPath.takeIf { it.isNotBlank() })
                            .joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (uiState.isLoading) {
            item(key = "local_media_folder_loading") {
                Text(
                    text = stringResource(R.string.local_media_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }
        }
        items(uiState.folders, key = { "folder:${it.path}" }) { folder ->
            LocalMediaFolderRow(
                folder = folder,
                onClick = { onFolderSelected(folder.path) }
            )
        }
        items(uiState.items, key = { "item:${it.id}:${it.uri}" }) { item ->
            LocalMediaItemRow(
                item = item,
                libraryName = libraryById[item.libraryId]?.displayName
                    ?: libraryById[item.libraryId]?.name,
                onClick = { onPlayItem(item) }
            )
        }
        if (!uiState.isLoading && uiState.folders.isEmpty() && uiState.items.isEmpty()) {
            item(key = "local_media_folder_empty") {
                AppMessageState(
                    title = stringResource(R.string.local_media_no_results_title),
                    subtitle = stringResource(R.string.local_media_no_results_subtitle)
                )
            }
        }
        if (uiState.canLoadMore) {
            item(key = "local_media_load_more") {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Load ${LOCAL_MEDIA_PAGE_SIZE} more (${uiState.loadedItemCount}/${uiState.totalItemCount})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalMediaLibraryRow(
    library: LocalMediaLibrary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = library.displayName ?: library.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = library.rootUri,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LocalMediaFolderRow(
    folder: LocalMediaFolderEntry,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Folder",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class LocalMediaQuery(
    val libraries: List<LocalMediaLibrary>,
    val libraryGroups: LocalMediaLibraryGroups,
    val section: LocalMediaSection,
    val developerModeEnabled: Boolean,
    val limit: Int,
    val mediaKinds: Set<LocalMediaKind>?,
    val libraryIds: Set<Long>?
)

private data class LocalMediaLibraryGroups(
    val allIds: Set<Long>,
    val normalIds: Set<Long>,
    val movieIds: Set<Long>,
    val otherIds: Set<Long>,
    val adultIds: Set<Long>
) {
    companion object {
        fun from(libraries: List<LocalMediaLibrary>, developerModeEnabled: Boolean): LocalMediaLibraryGroups {
            val adultIds = libraries.filter(LocalMediaLibrary::looksAdult).map(LocalMediaLibrary::id).toSet()
            val otherIds = libraries
                .filter { !it.looksAdult() && it.looksPersonalOrOther() }
                .map(LocalMediaLibrary::id)
                .toSet()
            val allIds = libraries.map(LocalMediaLibrary::id).toSet()
            val normalIds = if (developerModeEnabled) allIds else allIds - adultIds
            val movieIds = normalIds - otherIds - adultIds
            return LocalMediaLibraryGroups(
                allIds = allIds,
                normalIds = normalIds,
                movieIds = movieIds,
                otherIds = otherIds,
                adultIds = adultIds
            )
        }
    }
}

@Composable
private fun LocalMediaItemRow(
    item: LocalMediaItem,
    libraryName: String?,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.localDisplayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.localDetailLine(libraryName),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.durationMs.formatDuration(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun LocalMediaItem.matchesSection(section: LocalMediaSection): Boolean {
    val adult = isAdultLocalMediaItem(this)
    return when (section) {
        LocalMediaSection.ALL -> true
        LocalMediaSection.MOVIES -> mediaKind == LocalMediaKind.MOVIE && !adult
        LocalMediaSection.TV_SHOWS -> mediaKind == LocalMediaKind.EPISODE && !adult
        LocalMediaSection.OTHER -> mediaKind in setOf(LocalMediaKind.EXTRA, LocalMediaKind.UNKNOWN) && !adult
        LocalMediaSection.XXX -> adult
    }
}

private fun LocalMediaItem.matchesSection(
    section: LocalMediaSection,
    library: LocalMediaLibrary?
): Boolean {
    val adult = isAdultLocalMediaItem(this) || library?.looksAdult() == true
    val otherLibrary = library?.looksPersonalOrOther() == true
    return when (section) {
        LocalMediaSection.ALL -> true
        LocalMediaSection.MOVIES -> mediaKind == LocalMediaKind.MOVIE && !adult && !otherLibrary
        LocalMediaSection.TV_SHOWS -> mediaKind == LocalMediaKind.EPISODE && !adult
        LocalMediaSection.OTHER -> !adult && (otherLibrary || mediaKind in setOf(LocalMediaKind.EXTRA, LocalMediaKind.UNKNOWN))
        LocalMediaSection.XXX -> adult
    }
}

private fun LocalMediaSection.mediaKindsForQuery(): Set<LocalMediaKind>? = when (this) {
    LocalMediaSection.ALL -> null
    LocalMediaSection.MOVIES -> setOf(LocalMediaKind.MOVIE)
    LocalMediaSection.TV_SHOWS -> setOf(LocalMediaKind.EPISODE)
    LocalMediaSection.OTHER -> null
    LocalMediaSection.XXX -> null
}

private fun LocalMediaSection.libraryIdsForQuery(groups: LocalMediaLibraryGroups): Set<Long>? = when (this) {
    LocalMediaSection.ALL -> groups.normalIds
    LocalMediaSection.MOVIES -> groups.movieIds
    LocalMediaSection.TV_SHOWS -> groups.normalIds
    LocalMediaSection.OTHER -> groups.otherIds.takeIf { it.isNotEmpty() } ?: groups.normalIds
    LocalMediaSection.XXX -> groups.adultIds.takeIf { it.isNotEmpty() }
}

internal fun visibleLocalMediaSections(developerModeEnabled: Boolean): List<LocalMediaSection> =
    LocalMediaSection.entries.filter { developerModeEnabled || it != LocalMediaSection.XXX }

internal fun visibleLocalMediaItems(
    items: List<LocalMediaItem>,
    section: LocalMediaSection,
    developerModeEnabled: Boolean
): List<LocalMediaItem> =
    visibleLocalMediaItems(items, section, developerModeEnabled, emptyMap())

private fun visibleLocalMediaItems(
    items: List<LocalMediaItem>,
    section: LocalMediaSection,
    developerModeEnabled: Boolean,
    librariesById: Map<Long, LocalMediaLibrary>
): List<LocalMediaItem> =
    items.filter { item ->
        val library = librariesById[item.libraryId]
        val adult = isAdultLocalMediaItem(item) || library?.looksAdult() == true
        (developerModeEnabled || !adult) && item.matchesSection(section, library)
    }

private fun LocalMediaSection.labelResId(): Int = when (this) {
    LocalMediaSection.ALL -> R.string.local_media_filter_all
    LocalMediaSection.MOVIES -> R.string.local_media_filter_movies
    LocalMediaSection.TV_SHOWS -> R.string.local_media_filter_tv
    LocalMediaSection.OTHER -> R.string.local_media_filter_other
    LocalMediaSection.XXX -> R.string.local_media_filter_xxx
}

private fun LocalMediaSection.detail(count: Int): String =
    "$count shares"

private fun librarySectionCounts(
    libraries: List<LocalMediaLibrary>,
    groups: LocalMediaLibraryGroups
): Map<LocalMediaSection, Int> {
    val ids = libraries.map(LocalMediaLibrary::id).toSet()
    return mapOf(
        LocalMediaSection.ALL to (ids intersect groups.normalIds).size,
        LocalMediaSection.MOVIES to (ids intersect groups.movieIds).size,
        LocalMediaSection.TV_SHOWS to (ids intersect groups.normalIds).size,
        LocalMediaSection.OTHER to (ids intersect groups.otherIds).size,
        LocalMediaSection.XXX to (ids intersect groups.adultIds).size
    )
}

private fun observeSectionCounts(
    repository: LocalMediaRepository,
    groups: LocalMediaLibraryGroups
) = combine(
    repository.observeItemCount(libraryIds = groups.normalIds),
    repository.observeItemCount(setOf(LocalMediaKind.MOVIE), groups.movieIds),
    repository.observeItemCount(setOf(LocalMediaKind.EPISODE), groups.normalIds),
    repository.observeItemCount(libraryIds = groups.otherIds.takeIf { it.isNotEmpty() } ?: groups.normalIds),
    repository.observeItemCount(libraryIds = groups.adultIds)
) { allCount, movieCount, episodeCount, otherCount, adultCount ->
    mapOf(
        LocalMediaSection.ALL to allCount,
        LocalMediaSection.MOVIES to movieCount,
        LocalMediaSection.TV_SHOWS to episodeCount,
        LocalMediaSection.OTHER to otherCount,
        LocalMediaSection.XXX to adultCount
    )
}

private fun LocalMediaLibrary.looksAdult(): Boolean =
    listOf(name, displayName, rootUri).any { value ->
        val normalized = value.orEmpty().lowercase()
        ADULT_LIBRARY_TERMS.any(normalized::contains)
    }

private fun LocalMediaLibrary.looksPersonalOrOther(): Boolean =
    listOf(name, displayName, rootUri).any { value ->
        val normalized = value.orEmpty().lowercase()
        OTHER_LIBRARY_TERMS.any(normalized::contains)
    }

private fun LocalMediaItem.localDisplayTitle(): String {
    if (mediaKind == LocalMediaKind.EPISODE && !seriesTitle.isNullOrBlank()) {
        val episodeLabel = when {
            seasonNumber != null && episodeNumber != null -> "S${seasonNumber}E${episodeNumber}"
            episodeNumber != null -> "Episode $episodeNumber"
            else -> null
        }
        return listOfNotNull(seriesTitle, episodeLabel, title.takeIf { it != seriesTitle })
            .joinToString(" - ")
    }
    return title.ifBlank { displayName }
}

private fun LocalMediaItem.localDetailLine(libraryName: String?): String =
    listOfNotNull(
        mediaKind.label(),
        releaseYear?.toString(),
        genre?.takeIf(String::isNotBlank),
        libraryName?.takeIf(String::isNotBlank)
    ).joinToString(" - ")

private fun LocalMediaKind.label(): String = when (this) {
    LocalMediaKind.MOVIE -> "Movie"
    LocalMediaKind.EPISODE -> "TV Show"
    LocalMediaKind.EXTRA -> "Other"
    LocalMediaKind.UNKNOWN -> "Other"
}

private fun Long?.formatDuration(): String {
    val duration = this?.takeIf { it > 0L } ?: return ""
    val totalMinutes = duration / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private const val LOCAL_MEDIA_PAGE_SIZE = 200
private const val LOCAL_MEDIA_MAX_PAGE_LIMIT = 10_000
private val ADULT_LIBRARY_TERMS = listOf("xxx", "adult", "porn", "n0rp", "hentai", "ecchi", "r18")
private val OTHER_LIBRARY_TERMS = listOf("personal", "other", "misc", "clips", "home video", "home movies")
