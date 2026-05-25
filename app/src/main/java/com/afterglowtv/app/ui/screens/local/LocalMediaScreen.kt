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
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.repository.LocalMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val selectedSection: LocalMediaSection = LocalMediaSection.ALL,
    val developerModeEnabled: Boolean = false,
    val items: List<LocalMediaItem> = emptyList(),
    val totalItemCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class LocalMediaViewModel @Inject constructor(
    localMediaRepository: LocalMediaRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val selectedSection = MutableStateFlow(LocalMediaSection.ALL)

    val uiState: StateFlow<LocalMediaUiState> = combine(
        localMediaRepository.observeLibraries(),
        localMediaRepository.observeItems(),
        selectedSection,
        preferencesRepository.developerModeEnabled
    ) { libraries, items, selectedSection, developerModeEnabled ->
        val section = selectedSection.takeIf { it != LocalMediaSection.XXX || developerModeEnabled }
            ?: LocalMediaSection.ALL
        LocalMediaUiState(
            libraries = libraries,
            selectedSection = section,
            developerModeEnabled = developerModeEnabled,
            items = visibleLocalMediaItems(items, section, developerModeEnabled),
            totalItemCount = items.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalMediaUiState()
    )

    fun selectSection(section: LocalMediaSection) {
        selectedSection.value = section
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
            uiState.totalItemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppMessageState(
                        title = stringResource(R.string.local_media_no_videos_title),
                        subtitle = stringResource(R.string.local_media_no_videos_subtitle),
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
                    detail = section.detail(uiState.totalItemCount, uiState.items.size),
                    onClick = { onSectionSelected(section) }
                )
            },
            selectedKey = uiState.selectedSection.name,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )

        if (uiState.items.isEmpty()) {
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
                items(uiState.items, key = { it.id }) { item ->
                    LocalMediaItemRow(
                        item = item,
                        libraryName = libraryById[item.libraryId]?.displayName
                            ?: libraryById[item.libraryId]?.name,
                        onClick = { onPlayItem(item) }
                    )
                }
            }
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

internal fun visibleLocalMediaSections(developerModeEnabled: Boolean): List<LocalMediaSection> =
    LocalMediaSection.entries.filter { developerModeEnabled || it != LocalMediaSection.XXX }

internal fun visibleLocalMediaItems(
    items: List<LocalMediaItem>,
    section: LocalMediaSection,
    developerModeEnabled: Boolean
): List<LocalMediaItem> =
    items.filter { item ->
        val adult = isAdultLocalMediaItem(item)
        (developerModeEnabled || !adult) && item.matchesSection(section)
    }

private fun LocalMediaSection.labelResId(): Int = when (this) {
    LocalMediaSection.ALL -> R.string.local_media_filter_all
    LocalMediaSection.MOVIES -> R.string.local_media_filter_movies
    LocalMediaSection.TV_SHOWS -> R.string.local_media_filter_tv
    LocalMediaSection.OTHER -> R.string.local_media_filter_other
    LocalMediaSection.XXX -> R.string.local_media_filter_xxx
}

private fun LocalMediaSection.detail(totalCount: Int, visibleCount: Int): String =
    if (this == LocalMediaSection.ALL) "$totalCount files" else "$visibleCount shown"

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
