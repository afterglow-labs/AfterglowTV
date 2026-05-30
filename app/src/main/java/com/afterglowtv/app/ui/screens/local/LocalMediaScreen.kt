package com.afterglowtv.app.ui.screens.local

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.AppMessageState
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.components.shell.VodActionChip
import com.afterglowtv.app.ui.components.shell.VodActionChipRow
import com.afterglowtv.app.ui.components.rememberCrossfadeImageModel
import com.afterglowtv.app.ui.interaction.mouseClickable
import com.afterglowtv.app.ui.model.isAdultLocalMediaItem
import com.afterglowtv.app.ui.theme.FocusBorder
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.PrimaryLight
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.app.ui.theme.TextSecondary
import coil3.compose.AsyncImage
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

    BackHandler(enabled = uiState.isBrowsingLibrary) {
        viewModel.navigateUp()
    }

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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 172.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.visibleLibraries, key = { it.id }) { library ->
                    LocalMediaFolderCard(
                        title = library.displayName ?: library.name,
                        subtitle = library.rootUri,
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 172.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "local_media_up", span = { GridItemSpan(maxLineSpan) }) {
            LocalMediaBreadcrumb(uiState = uiState, onNavigateUp = onNavigateUp)
        }
        if (uiState.isLoading) {
            item(key = "local_media_folder_loading", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.local_media_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }
        }
        items(uiState.folders, key = { "folder:${it.path}" }) { folder ->
            LocalMediaFolderCard(
                title = folder.name,
                subtitle = null,
                onClick = { onFolderSelected(folder.path) }
            )
        }
        items(
            uiState.items,
            key = { "item:${it.id}:${it.uri}" }
        ) { item ->
            LocalMediaFileCard(
                item = item,
                libraryName = libraryById[item.libraryId]?.displayName
                    ?: libraryById[item.libraryId]?.name,
                onClick = { onPlayItem(item) }
            )
        }
        if (!uiState.isLoading && uiState.folders.isEmpty() && uiState.items.isEmpty()) {
            item(key = "local_media_folder_empty", span = { GridItemSpan(maxLineSpan) }) {
                AppMessageState(
                    title = stringResource(R.string.local_media_no_results_title),
                    subtitle = stringResource(R.string.local_media_no_results_subtitle)
                )
            }
        }
        if (uiState.canLoadMore) {
            item(key = "local_media_load_more", span = { GridItemSpan(maxLineSpan) }) {
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
private fun LocalMediaBreadcrumb(
    uiState: LocalMediaUiState,
    onNavigateUp: () -> Unit
) {
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
                text = listOfNotNull(
                    uiState.selectedLibraryName,
                    uiState.currentPath.takeIf { it.isNotBlank() }
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LocalMediaFolderCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .mouseClickable(focusRequester = focusRequester, onClick = onClick),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight,
            contentColor = OnSurface,
            focusedContentColor = OnSurface
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FolderGlyph(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FolderGlyph(modifier: Modifier = Modifier) {
    val bodyColor = Primary
    val accentColor = PrimaryLight
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = h * 0.10f

        // Geometry of a classic folder seen straight-on.
        val backTop = h * 0.20f          // top of the back panel / tab
        val frontTop = h * 0.40f         // where the front flap begins
        val tabWidth = w * 0.42f
        val tabSlope = w * 0.10f         // diagonal cut from tab into the back panel

        // ── Back panel (with the raised tab) ─────────────────────────
        val backPanel = Path().apply {
            moveTo(r, backTop)
            // tab top edge
            lineTo(tabWidth, backTop)
            // diagonal up-step into the main panel
            lineTo(tabWidth + tabSlope, frontTop)
            lineTo(w - r, frontTop)
            // round top-right of the panel
            quadraticBezierTo(w, frontTop, w, frontTop + r)
            lineTo(w, h - r)
            quadraticBezierTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticBezierTo(0f, h, 0f, h - r)
            lineTo(0f, backTop + r)
            quadraticBezierTo(0f, backTop, r, backTop)
            close()
        }
        drawPath(
            path = backPanel,
            brush = Brush.verticalGradient(colors = listOf(accentColor, bodyColor))
        )

        // ── Front flap (the part that opens) ─────────────────────────
        val flapTop = h * 0.46f
        val frontFlap = Path().apply {
            moveTo(r, flapTop)
            lineTo(w - r, flapTop)
            quadraticBezierTo(w, flapTop, w, flapTop + r)
            lineTo(w, h - r)
            quadraticBezierTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticBezierTo(0f, h, 0f, h - r)
            lineTo(0f, flapTop + r)
            quadraticBezierTo(0f, flapTop, r, flapTop)
            close()
        }
        drawPath(
            path = frontFlap,
            brush = Brush.verticalGradient(
                colors = listOf(bodyColor, accentColor)
            )
        )
        // Subtle highlight line where the flap meets the back panel.
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(r, flapTop),
            end = Offset(w - r, flapTop),
            strokeWidth = h * 0.02f
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalMediaFileCard(
    item: LocalMediaItem,
    libraryName: String?,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val artworkUri = item.posterUri?.takeIf { it.isNotBlank() }
        ?: item.backdropUri?.takeIf { it.isNotBlank() }
    val fileType = remember(item.mimeType, item.uri) { item.fileType() }
    val duration = item.durationMs.formatDuration()
    val badge = remember(item.uri) { item.fileExtensionBadge() }
    val adult = remember(item.uri) { isAdultLocalMediaItem(item) }
    val parsed = remember(item.uri, item.title, item.displayName) {
        item.parsedDisplayName(adult, libraryName)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .mouseClickable(focusRequester = focusRequester, onClick = onClick),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight,
            contentColor = OnSurface,
            focusedContentColor = OnSurface
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
            ) {
                if (artworkUri != null) {
                    AsyncImage(
                        model = rememberCrossfadeImageModel(artworkUri),
                        contentDescription = item.localDisplayTitle(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FileTypeGlyph(
                        type = fileType,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (badge.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                if (duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = parsed.primary,
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface,
                textAlign = if (focused) TextAlign.Start else TextAlign.Center,
                maxLines = if (focused) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focused) Modifier.basicMarquee() else Modifier)
            )
            if (parsed.secondary != null) {
                Text(
                    text = parsed.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = if (focused) TextAlign.Start else TextAlign.Center,
                    maxLines = if (focused) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focused) Modifier.basicMarquee() else Modifier)
                )
            }
            if (parsed.tertiary != null) {
                Text(
                    text = parsed.tertiary,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = if (focused) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focused) Modifier.basicMarquee() else Modifier)
                )
            }
        }
    }
}

private enum class LocalFileType { VIDEO, AUDIO, IMAGE, OTHER }

private fun LocalMediaItem.fileType(): LocalFileType {
    val mime = mimeType?.lowercase().orEmpty()
    val ext = uri.substringBefore('?').substringAfterLast('.', "").lowercase()
    return when {
        mime.startsWith("video") ||
            ext in setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts", "m2ts", "3gp") ->
            LocalFileType.VIDEO
        mime.startsWith("audio") ||
            ext in setOf("mp3", "flac", "wav", "aac", "ogg", "oga", "m4a", "wma", "opus") ->
            LocalFileType.AUDIO
        mime.startsWith("image") ||
            ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif") ->
            LocalFileType.IMAGE
        else -> LocalFileType.OTHER
    }
}

private fun LocalMediaItem.fileExtensionBadge(): String =
    uri.substringBefore('?').substringAfterLast('.', "").uppercase().takeIf { it.length in 1..5 }.orEmpty()

@Composable
private fun FileTypeGlyph(type: LocalFileType, modifier: Modifier = Modifier) {
    val accent = PrimaryLight
    val body = Primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Backdrop wash so the glyph never sits on a flat void.
        drawRect(brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.20f), body.copy(alpha = 0.32f))))

        val cx = w / 2f
        val cy = h / 2f
        val unit = minOf(w, h)

        when (type) {
            LocalFileType.VIDEO -> {
                val r = unit * 0.22f
                drawCircle(color = Color.White.copy(alpha = 0.92f), radius = r, center = Offset(cx, cy))
                val t = r * 0.9f
                val play = Path().apply {
                    moveTo(cx - t * 0.4f, cy - t * 0.7f)
                    lineTo(cx + t * 0.8f, cy)
                    lineTo(cx - t * 0.4f, cy + t * 0.7f)
                    close()
                }
                drawPath(play, color = body)
            }
            LocalFileType.AUDIO -> {
                val r = unit * 0.10f
                val stemH = unit * 0.34f
                val x1 = cx - unit * 0.12f
                val x2 = cx + unit * 0.16f
                val topY = cy - stemH * 0.5f
                val baseY = cy + stemH * 0.5f
                drawCircle(color = Color.White.copy(alpha = 0.92f), radius = r, center = Offset(x1, baseY))
                drawCircle(color = Color.White.copy(alpha = 0.92f), radius = r, center = Offset(x2, baseY - r * 0.4f))
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = Offset(x1 + r, baseY),
                    end = Offset(x1 + r, topY),
                    strokeWidth = unit * 0.035f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = Offset(x2 + r, baseY - r * 0.4f),
                    end = Offset(x2 + r, topY - r * 0.4f),
                    strokeWidth = unit * 0.035f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = Offset(x1 + r, topY),
                    end = Offset(x2 + r, topY - r * 0.4f),
                    strokeWidth = unit * 0.05f
                )
            }
            LocalFileType.IMAGE -> {
                val boxSize = unit * 0.5f
                val left = cx - boxSize / 2f
                val top = cy - boxSize / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(boxSize * 0.12f, boxSize * 0.12f)
                )
                drawCircle(color = accent, radius = boxSize * 0.12f, center = Offset(left + boxSize * 0.3f, top + boxSize * 0.3f))
                val mountain = Path().apply {
                    moveTo(left + boxSize * 0.12f, top + boxSize * 0.85f)
                    lineTo(left + boxSize * 0.42f, top + boxSize * 0.5f)
                    lineTo(left + boxSize * 0.62f, top + boxSize * 0.72f)
                    lineTo(left + boxSize * 0.78f, top + boxSize * 0.55f)
                    lineTo(left + boxSize * 0.88f, top + boxSize * 0.85f)
                    close()
                }
                drawPath(mountain, color = body)
            }
            LocalFileType.OTHER -> {
                // A simple document sheet with a folded corner.
                val sheetW = unit * 0.42f
                val sheetH = unit * 0.54f
                val left = cx - sheetW / 2f
                val top = cy - sheetH / 2f
                val fold = sheetW * 0.32f
                val sheet = Path().apply {
                    moveTo(left, top)
                    lineTo(left + sheetW - fold, top)
                    lineTo(left + sheetW, top + fold)
                    lineTo(left + sheetW, top + sheetH)
                    lineTo(left, top + sheetH)
                    close()
                }
                drawPath(sheet, color = Color.White.copy(alpha = 0.9f))
                val corner = Path().apply {
                    moveTo(left + sheetW - fold, top)
                    lineTo(left + sheetW, top + fold)
                    lineTo(left + sheetW - fold, top + fold)
                    close()
                }
                drawPath(corner, color = accent)
            }
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

/**
 * Cleaned, multi-line display derived from a release-style filename. Modeled on the token
 * approach used by scene-release parsers (parse-torrent-name / guessit): everything before the
 * first "stop" token (resolution, source, codec, etc.) is treated as the meaningful name.
 */
private data class ParsedDisplayName(
    val primary: String,
    val secondary: String?,
    val tertiary: String?
)

// Tokens that mark the end of the human-readable portion of a release name.
private val RELEASE_STOP_TOKENS: Set<String> = setOf(
    "480p", "540p", "576p", "720p", "1080p", "1080i", "2160p", "4320p", "4k", "8k", "uhd", "hd", "sd",
    "bluray", "blu-ray", "brrip", "bdrip", "bd", "webrip", "web-dl", "webdl", "web", "hdtv", "pdtv",
    "dvdrip", "dvd", "dvdscr", "hdrip", "hdcam", "camrip", "cam", "ts", "tc", "telesync", "remux",
    "amzn", "nf", "dsnp", "hmax", "atvp", "hulu", "max", "pcok", "stan",
    "x264", "x265", "h264", "h265", "h", "hevc", "avc", "xvid", "divx", "av1", "vp9",
    "10bit", "8bit", "hi10p",
    "aac", "ac3", "eac3", "dd", "ddp", "dd5", "dts", "dtshd", "truehd", "flac", "mp3", "opus", "atmos",
    "proper", "repack", "rerip", "internal", "limited", "extended", "theatrical", "unrated", "uncut",
    "remastered", "imax", "hdr", "hdr10", "hdr10plus", "dv", "dovi", "sdr", "xxx", "multi", "dual",
    "subbed", "dubbed", "complete", "season", "mp4", "mkv", "avi", "mov", "wmv", "ws"
)

private val SEASON_EPISODE_REGEX =
    Regex("""[Ss](\d{1,2})[ ._-]*[Ee](\d{1,3})|(\d{1,2})x(\d{1,3})""")
private val YEAR_REGEX = Regex("""\b(19\d{2}|20\d{2})\b""")
private val ADULT_DATE_REGEX = Regex("""\b(\d{2})[._-](\d{2})[._-](\d{2})\b""")

private fun String.titleCaseWords(): String =
    split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            if (word.length <= 1) word.uppercase()
            else word.replaceFirstChar { it.uppercase() }
        }

private fun String.normalizeSeparators(): String =
    replace('.', ' ')
        .replace('_', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

/** Tokenize a raw release string and return the cleaned title before the first stop token. */
private fun titleBeforeStopTokens(raw: String): Pair<String, List<String>> {
    val tokens = raw.split('.', '_', ' ', '-', '(', ')', '[', ']', '{', '}')
        .filter { it.isNotBlank() }
    val titleTokens = mutableListOf<String>()
    var stopIndex = tokens.size
    for ((index, token) in tokens.withIndex()) {
        if (token.lowercase() in RELEASE_STOP_TOKENS || YEAR_REGEX.matches(token)) {
            stopIndex = index
            break
        }
        titleTokens += token
    }
    val remainder = if (stopIndex < tokens.size) tokens.drop(stopIndex) else emptyList()
    return titleTokens.joinToString(" ").titleCaseWords() to remainder
}

/** Pick the most informative raw name: the true on-disk filename, or the parent folder if richer. */
private fun LocalMediaItem.bestRawReleaseName(): String {
    fun decode(value: String): String =
        runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    val path = uri.substringBefore('?').trimEnd('/')
    val fileName = decode(path.substringAfterLast('/', "")).substringBeforeLast('.', "")
    val folderName = decode(path.substringBeforeLast('/', "").substringAfterLast('/', ""))
    val fileBase = displayName.substringBeforeLast('.', displayName)

    // Score each candidate by how much real release metadata it carries.
    fun score(candidate: String): Int {
        if (candidate.isBlank()) return -1
        val lower = candidate.lowercase()
        var s = 0
        if (SEASON_EPISODE_REGEX.containsMatchIn(candidate)) s += 4
        if (ADULT_DATE_REGEX.containsMatchIn(candidate)) s += 4
        if (RELEASE_STOP_TOKENS.any { token -> Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(lower) }) s += 3
        if (YEAR_REGEX.containsMatchIn(candidate)) s += 1
        s += candidate.length / 20
        return s
    }

    val candidates = listOf(fileName, fileBase, folderName, title).filter { it.isNotBlank() }
    return candidates.maxByOrNull(::score)?.takeIf { score(it) > 0 }
        ?: candidates.firstOrNull().orEmpty().ifBlank { displayName }
}

private fun parseTvDisplayName(raw: String): ParsedDisplayName? {
    val match = SEASON_EPISODE_REGEX.find(raw) ?: return null
    val season = (match.groupValues[1].ifBlank { match.groupValues[3] }).toIntOrNull()
    val episode = (match.groupValues[2].ifBlank { match.groupValues[4] }).toIntOrNull()
    val seriesRaw = raw.substring(0, match.range.first)
    val series = seriesRaw.normalizeSeparators()
        .replace(YEAR_REGEX, "")
        .replace(Regex("""[(\[]\s*[)\]]"""), "")
        .trim(' ', '-', '.')
        .titleCaseWords()
        .ifBlank { return null }
    val afterEpisode = raw.substring(match.range.last + 1)
    val (episodeTitleRaw, _) = titleBeforeStopTokens(afterEpisode)
    val episodeTitle = episodeTitleRaw
        .takeUnless { it.equals(series, ignoreCase = true) }
        ?.takeUnless { SEASON_EPISODE_REGEX.containsMatchIn(it) }
        ?.takeUnless { it.contains(series, ignoreCase = true) }
    val seLabel = when {
        season != null && episode != null ->
            "S%02d - E%02d".format(season, episode)
        episode != null -> "E%02d".format(episode)
        else -> null
    }
    return ParsedDisplayName(
        primary = series,
        secondary = episodeTitle?.takeIf { it.isNotBlank() },
        tertiary = seLabel
    )
}

private fun parseAdultDisplayName(raw: String): ParsedDisplayName? {
    val dateMatch = ADULT_DATE_REGEX.find(raw)
    if (dateMatch != null) {
        val studio = raw.substring(0, dateMatch.range.first)
            .normalizeSeparators().titleCaseWords().trim()
        val afterDate = raw.substring(dateMatch.range.last + 1)
        val (performersOrTitle, _) = titleBeforeStopTokens(afterDate)
        val (yy, mm, dd) = Triple(
            dateMatch.groupValues[1],
            dateMatch.groupValues[2],
            dateMatch.groupValues[3]
        )
        val dateLabel = "20$yy-$mm-$dd"
        val studioLine = listOfNotNull(studio.takeIf { it.isNotBlank() }, dateLabel)
            .joinToString(" · ")
        return ParsedDisplayName(
            primary = performersOrTitle.ifBlank { studio.ifBlank { return null } },
            secondary = null,
            tertiary = studioLine.takeIf { it.isNotBlank() }
        )
    }
    // No date: fall back to title-before-stop-tokens.
    val (cleaned, _) = titleBeforeStopTokens(raw)
    return cleaned.takeIf { it.isNotBlank() }
        ?.let { ParsedDisplayName(primary = it, secondary = null, tertiary = null) }
}

private fun parseMovieDisplayName(raw: String): ParsedDisplayName {
    val yearMatch = YEAR_REGEX.find(raw)
    val titlePart = if (yearMatch != null) raw.substring(0, yearMatch.range.first) else raw
    val cleaned = titleBeforeStopTokens(titlePart).first.ifBlank {
        titlePart.normalizeSeparators().titleCaseWords()
    }
    return ParsedDisplayName(
        primary = cleaned.ifBlank { raw.normalizeSeparators().titleCaseWords() },
        secondary = null,
        tertiary = yearMatch?.value
    )
}

private fun LocalMediaItem.parsedDisplayName(
    adult: Boolean,
    libraryName: String?
): ParsedDisplayName {
    val raw = bestRawReleaseName()
    val parsed = when {
        adult -> parseAdultDisplayName(raw)
        mediaKind == LocalMediaKind.EPISODE || SEASON_EPISODE_REGEX.containsMatchIn(raw) ->
            parseTvDisplayName(raw)
        mediaKind == LocalMediaKind.MOVIE -> parseMovieDisplayName(raw)
        else -> null
    }
    return parsed ?: ParsedDisplayName(
        primary = localDisplayTitle(),
        secondary = localDetailLine(libraryName).takeIf { it.isNotBlank() },
        tertiary = null
    )
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
