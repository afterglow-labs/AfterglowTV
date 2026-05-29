package com.afterglowtv.app.ui.screens.local

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.ui.components.rememberCrossfadeImageModel
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.AppMessageState
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.components.shell.VodActionChip
import com.afterglowtv.app.ui.components.shell.VodActionChipRow
import com.afterglowtv.app.ui.model.isAdultLocalMediaItem
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.repository.LocalMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    val items: List<LocalMediaItem> = emptyList(),
    val totalItemCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class LocalMediaViewModel @Inject constructor(
    localMediaRepository: LocalMediaRepository
) : ViewModel() {
    private val selectedSection = MutableStateFlow(LocalMediaSection.ALL)

    val uiState: StateFlow<LocalMediaUiState> = combine(
        localMediaRepository.observeLibraries(),
        localMediaRepository.observeItems(),
        selectedSection
    ) { libraries, items, section ->
        LocalMediaUiState(
            libraries = libraries,
            selectedSection = section,
            items = items.filter { item -> item.matchesSection(section) },
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
    var browserLocation by remember(uiState.selectedSection) { mutableStateOf(LocalMediaBrowserLocation()) }
    val browserModel = remember(uiState.items, uiState.libraries, browserLocation) {
        buildBrowserModel(
            items = uiState.items,
            libraries = uiState.libraries,
            location = browserLocation
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VodActionChipRow(
            actions = LocalMediaSection.entries.map { section ->
                VodActionChip(
                    key = section.name,
                    label = stringResource(section.labelResId()),
                    detail = section.detail(uiState.totalItemCount, uiState.items.size),
                    onClick = {
                        browserLocation = LocalMediaBrowserLocation()
                        onSectionSelected(section)
                    }
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
            PlexFolderBrowser(
                model = browserModel,
                libraryById = libraryById,
                onLocationSelected = { browserLocation = it },
                onPlayItem = onPlayItem
            )
        }
    }
}

@Composable
private fun PlexFolderBrowser(
    model: LocalMediaBrowserModel,
    libraryById: Map<Long, LocalMediaLibrary>,
    onLocationSelected: (LocalMediaBrowserLocation) -> Unit,
    onPlayItem: (LocalMediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 178.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "browser_header") {
            PlexBrowserHeader(model = model)
        }

        if (model.canNavigateUp) {
            item(key = "folder:up") {
                PlexFolderTile(
                    title = "Up",
                    subtitle = model.parentTitle,
                    onClick = { onLocationSelected(model.parentLocation) },
                    isUpTile = true
                )
            }
        }

        items(model.folders, key = { "folder:${it.key}" }) { folder ->
            PlexFolderTile(
                title = folder.name,
                subtitle = folder.itemCount.formatFileCount(),
                onClick = { onLocationSelected(folder.location) }
            )
        }

        items(model.files, key = { "file:${it.id}" }) { item ->
            PlexMediaTile(
                item = item,
                libraryName = libraryById[item.libraryId]?.displayName
                    ?: libraryById[item.libraryId]?.name,
                onClick = { onPlayItem(item) }
            )
        }
    }
}

@Composable
private fun PlexBrowserHeader(model: LocalMediaBrowserModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = model.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            model.breadcrumb.forEachIndexed { index, crumb ->
                Text(
                    text = crumb,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index == model.breadcrumb.lastIndex) Color.White else Color(0xFFB8C0CC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (index != model.breadcrumb.lastIndex) {
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6F7A89)
                    )
                }
            }
        }
        Text(
            text = "${model.folders.size} folders  ${model.files.size} videos",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9AA5B5)
        )
    }
}

@Composable
private fun PlexFolderTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isUpTile: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF17202C),
            focusedContainerColor = Color(0xFF26384E)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = if (isUpTile) {
                            listOf(Color(0xFF253144), Color(0xFF111923))
                        } else {
                            listOf(Color(0xFF26384E), Color(0xFF141D28))
                        }
                    )
                )
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(if (isUpTile) Color(0xFF9AA5B5) else Color(0xFFE5A00D))
                    .align(Alignment.TopStart)
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB8C0CC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlexMediaTile(
    item: LocalMediaItem,
    libraryName: String?,
    onClick: () -> Unit
) {
    val title = item.localDisplayTitle()
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF151B24),
            focusedContainerColor = Color(0xFF243246)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = localMediaFallbackPalette(title)))
        ) {
            (item.posterUri ?: item.backdropUri)?.takeIf { it.isNotBlank() }?.let { artworkUri ->
                AsyncImage(
                    model = rememberCrossfadeImageModel(artworkUri),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE6000000))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.localDetailLine(libraryName),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD3DAE5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item.durationMs.formatDuration().takeIf { it.isNotBlank() }?.let { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color(0x99000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private data class LocalMediaBrowserLocation(
    val libraryId: Long? = null,
    val path: List<String> = emptyList()
)

private data class LocalMediaBrowserModel(
    val title: String,
    val breadcrumb: List<String>,
    val folders: List<LocalMediaBrowserFolder>,
    val files: List<LocalMediaItem>,
    val canNavigateUp: Boolean,
    val parentLocation: LocalMediaBrowserLocation,
    val parentTitle: String
)

private data class LocalMediaBrowserFolder(
    val key: String,
    val name: String,
    val itemCount: Int,
    val location: LocalMediaBrowserLocation
)

private fun buildBrowserModel(
    items: List<LocalMediaItem>,
    libraries: List<LocalMediaLibrary>,
    location: LocalMediaBrowserLocation
): LocalMediaBrowserModel {
    val librariesById = libraries.associateBy(LocalMediaLibrary::id)
    if (location.libraryId == null) {
        val folders = libraries.mapNotNull { library ->
            val count = items.count { it.libraryId == library.id }
            if (count == 0) return@mapNotNull null
            LocalMediaBrowserFolder(
                key = "library:${library.id}",
                name = library.displayName ?: library.name,
                itemCount = count,
                location = LocalMediaBrowserLocation(libraryId = library.id)
            )
        }.sortedBy { it.name.lowercase() }
        return LocalMediaBrowserModel(
            title = "Personal Library",
            breadcrumb = listOf("Libraries"),
            folders = folders,
            files = emptyList(),
            canNavigateUp = false,
            parentLocation = LocalMediaBrowserLocation(),
            parentTitle = ""
        )
    }

    val library = librariesById[location.libraryId]
    val libraryName = library?.displayName ?: library?.name ?: "Library"
    val visibleItems = items.filter { it.libraryId == location.libraryId }
    val childFolders = linkedMapOf<String, MutableList<LocalMediaItem>>()
    val directFiles = mutableListOf<LocalMediaItem>()

    visibleItems.forEach { item ->
        val segments = item.folderSegments()
        if (!segments.startsWith(location.path)) return@forEach
        if (segments.size > location.path.size) {
            val childFolder = segments[location.path.size]
            childFolders.getOrPut(childFolder) { mutableListOf() }.add(item)
        } else {
            directFiles += item
        }
    }

    val folders = childFolders.map { (name, folderItems) ->
        LocalMediaBrowserFolder(
            key = "${location.libraryId}:${(location.path + name).joinToString("/")}",
            name = name,
            itemCount = folderItems.size,
            location = location.copy(path = location.path + name)
        )
    }.sortedBy { it.name.lowercase() }

    val parentLocation = when {
        location.path.isNotEmpty() -> location.copy(path = location.path.dropLast(1))
        else -> LocalMediaBrowserLocation()
    }
    val parentTitle = when {
        location.path.isNotEmpty() -> location.path.dropLast(1).lastOrNull() ?: libraryName
        else -> "Libraries"
    }

    return LocalMediaBrowserModel(
        title = location.path.lastOrNull() ?: libraryName,
        breadcrumb = listOf("Libraries", libraryName) + location.path,
        folders = folders,
        files = directFiles.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.localDisplayTitle() }),
        canNavigateUp = true,
        parentLocation = parentLocation,
        parentTitle = parentTitle
    )
}

private fun List<String>.startsWith(prefix: List<String>): Boolean {
    if (prefix.size > size) return false
    return prefix.indices.all { index -> this[index].equals(prefix[index], ignoreCase = true) }
}

private fun LocalMediaItem.folderSegments(): List<String> =
    folderPath
        .split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun Int.formatFileCount(): String = when (this) {
    1 -> "1 video"
    else -> "$this videos"
}

private fun localMediaFallbackPalette(title: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF253144), Color(0xFF141D28)),
        listOf(Color(0xFF26384E), Color(0xFF12212F)),
        listOf(Color(0xFF2E3544), Color(0xFF161B24)),
        listOf(Color(0xFF233044), Color(0xFF20242C)),
        listOf(Color(0xFF2B3340), Color(0xFF151B24))
    )
    return palettes[kotlin.math.abs(title.hashCode()) % palettes.size]
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
