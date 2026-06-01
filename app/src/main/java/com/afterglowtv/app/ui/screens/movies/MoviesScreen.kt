package com.afterglowtv.app.ui.screens.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.*
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.afterglowtv.app.ui.components.PlayerRenderView
import com.afterglowtv.app.ui.components.SearchInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.afterglowtv.app.device.rememberIsTelevisionDevice
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.ui.components.CategoryRow
import com.afterglowtv.app.ui.components.ContinueWatchingRow
import com.afterglowtv.app.ui.components.MovieCard
import com.afterglowtv.app.ui.components.SelectionChip
import com.afterglowtv.app.ui.components.SelectionChipRow
import com.afterglowtv.app.ui.components.SavedCategoryContextCard
import com.afterglowtv.app.ui.components.SavedCategoryShortcut
import com.afterglowtv.app.ui.components.SavedCategoryShortcutsRow
import com.afterglowtv.app.ui.theme.*
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.LibraryFilterType
import com.afterglowtv.domain.model.LibrarySortBy
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.PlaybackHistory
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.afterglowtv.app.R
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.border
import com.afterglowtv.app.ui.components.ReorderTopBar
import com.afterglowtv.app.ui.components.dialogs.DeleteGroupDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.BrowseSearchLaunchCard
import com.afterglowtv.app.ui.components.shell.LoadMoreCard
import com.afterglowtv.app.ui.components.shell.InfiniteScrollEffect
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppMessageState
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import androidx.tv.material3.Border
import com.afterglowtv.app.ui.components.dialogs.RenameGroupDialog
import com.afterglowtv.app.ui.components.shell.VodActionChip
import com.afterglowtv.app.ui.components.shell.VodActionChipRow
import com.afterglowtv.app.ui.components.shell.VodCategoryOption
import com.afterglowtv.app.ui.components.shell.VodCategoryPickerDialog
import com.afterglowtv.app.ui.components.shell.VodBrowseOptionsDialog
import com.afterglowtv.app.ui.components.shell.VodClassicCategoryOption
import com.afterglowtv.app.ui.components.shell.VodClassicContentHeader
import com.afterglowtv.app.ui.components.shell.VodClassicSplitLayout
import com.afterglowtv.app.ui.components.shell.VodGuideLane
import com.afterglowtv.app.ui.components.shell.VodGuideProgramCard
import com.afterglowtv.app.ui.components.shell.VodHeroStrip
import com.afterglowtv.app.ui.components.shell.VodSectionHeader
import com.afterglowtv.app.ui.design.FocusRestoreHost
import com.afterglowtv.app.ui.design.requestFocusSafely
import com.afterglowtv.app.ui.model.AdultGuideCategoryBuilder
import com.afterglowtv.app.ui.model.VodGuideItem
import com.afterglowtv.app.ui.model.VodGuideRowBuilder
import com.afterglowtv.app.ui.model.VodTitleFormatter
import com.afterglowtv.app.ui.model.VodViewMode
import com.afterglowtv.app.ui.screens.vod.HandleVodUserMessage
import com.afterglowtv.app.ui.screens.vod.ProtectedVodPinDialog
import com.afterglowtv.app.ui.screens.vod.VodBrowseDefaults
import com.afterglowtv.app.ui.screens.vod.vodActiveFilterSortDetail
import com.afterglowtv.player.PlayerRenderSurfaceType
import com.afterglowtv.player.PlayerEngine
import com.afterglowtv.player.PlayerSurfaceResizeMode
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoviesScreen(
    onMovieClick: (Movie) -> Unit,
    onContinueWatchingPlay: (PlaybackHistory) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    initialGuideMode: Boolean = false,
    initialAdultGuideMode: Boolean = false,
    wordmark: String? = null,
    tagline: String? = null,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    remember(viewModel) {
        viewModel.resetPreviewRowsForScreenEntry()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val initialContentFocusRequester = remember { FocusRequester() }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingMovie by remember { mutableStateOf<Movie?>(null) }
    var pendingCategory by remember { mutableStateOf<Category?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(initialGuideMode, initialAdultGuideMode) {
        if (initialAdultGuideMode) {
            viewModel.openAdultVodGuide()
        } else if (initialGuideMode) {
            viewModel.openVodGuide()
        }
    }

    HandleVodUserMessage(
        userMessage = uiState.userMessage,
        snackbarHostState = snackbarHostState,
        onShown = viewModel::userMessageShown
    )

    BackHandler(
        enabled = uiState.selectedCategory != null &&
            !uiState.isReorderMode &&
            (uiState.vodViewMode == VodViewMode.SHELVES || uiState.selectedCategory != uiState.fullLibraryCategoryName)
    ) {
        if (uiState.vodViewMode == VodViewMode.SHELVES) {
            viewModel.selectCategory(null)
        } else {
            viewModel.selectFullLibraryBrowse()
        }
    }

    ProtectedVodPinDialog(
        visible = showPinDialog,
        error = pinError,
        incorrectPinMessage = context.getString(R.string.movies_incorrect_pin),
        onDismissRequest = {
            showPinDialog = false
            pinError = null
            pendingMovie = null
            pendingCategory = null
        },
        onVerified = {
            showPinDialog = false
            pinError = null
            pendingMovie?.let(onMovieClick)
            pendingCategory?.let(viewModel::unlockCategory)
            pendingMovie = null
            pendingCategory = null
        },
        onErrorChange = { pinError = it },
        verifyPin = viewModel::verifyPin
    )

    Box(modifier = Modifier.fillMaxSize()) {
        FocusRestoreHost(
            enabled = !uiState.isLoading && uiState.errorMessage == null,
            onRestore = {
                delay(100)
                initialContentFocusRequester.requestFocusSafely(tag = "MoviesScreen", target = "Initial movies content")
            }
        ) {
        AppScreenScaffold(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            title = stringResource(R.string.nav_movies),
            subtitle = null,
            navigationChrome = AppNavigationChrome.TopBar,
            compactHeader = true,
            showScreenHeader = false
        ) {
        AfterglowBrandStrip(
            wordmark = wordmark ?: "Movies",
            tagline = tagline ?: "Your VOD library, sorted and ready.",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        )
        if (uiState.isReorderMode && uiState.reorderCategory != null) {
            ReorderTopBar(
                categoryName = uiState.reorderCategory!!.name,
                onSave = { viewModel.saveReorder() },
                onCancel = { viewModel.exitCategoryReorderMode() },
                subtitle = stringResource(R.string.movies_reorder_subtitle)
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = stringResource(R.string.movies_loading),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else if (uiState.errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppMessageState(
                    title = stringResource(R.string.home_error_load_failed),
                    subtitle = uiState.errorMessage ?: ""
                )
            }
        } else if (!uiState.hasProviders) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppMessageState(
                    title = stringResource(R.string.home_add_first_provider),
                    subtitle = stringResource(R.string.home_add_first_provider_subtitle)
                )
            }
	        } else if (!uiState.hasActiveProvider || (uiState.selectedCategory == null && uiState.moviesByCategory.isEmpty() && uiState.libraryCount == 0 && uiState.searchQuery.isBlank() && !uiState.isLoadingPreviewRows)) {
	            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
	                AppMessageState(
	                    title = stringResource(R.string.vod_sync_needed_title),
	                    subtitle = stringResource(R.string.vod_sync_needed_subtitle),
	                    action = if (uiState.hasActiveProvider) {
	                        {
	                            Button(
	                                onClick = viewModel::refreshProviderVodLibrary,
	                                enabled = !uiState.isRefreshingProvider
	                            ) {
	                                Text(
	                                    text = stringResource(
	                                        if (uiState.isRefreshingProvider) {
	                                            R.string.vod_syncing_provider
	                                        } else {
	                                            R.string.vod_sync_provider_action
	                                        }
	                                    )
	                                )
	                            }
	                        }
	                    } else {
	                        null
	                    }
	                )
	            }
	        } else if (uiState.selectedCategory == null && uiState.searchQuery.isBlank() && uiState.moviesByCategory.isEmpty() && !uiState.isLoadingPreviewRows) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppMessageState(
                    title = stringResource(R.string.movies_no_found),
                    subtitle = stringResource(R.string.movies_no_found_subtitle)
                )
            }
        } else {
            MoviesVodContent(
                uiState = uiState,
                selectedFilterType = uiState.selectedLibraryFilterType,
                onSelectedFilterTypeChange = viewModel::setSelectedLibraryFilterType,
                selectedSortBy = uiState.selectedLibrarySortBy,
                onSelectedSortByChange = viewModel::setSelectedLibrarySortBy,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                onMovieClick = onMovieClick,
                onContinueWatchingPlay = onContinueWatchingPlay,
                onProtectedMovieClick = { movie ->
                    pendingCategory = null
                    pendingMovie = movie
                    showPinDialog = true
                },
                onProtectedCategoryClick = { category ->
                    pendingMovie = null
                    pendingCategory = category
                    showPinDialog = true
                },
                onShowDialog = viewModel::onShowDialog,
                onShowCategoryOptions = viewModel::showCategoryOptions,
                onSelectCategory = viewModel::selectCategory,
                onSelectFullLibraryBrowse = viewModel::selectFullLibraryBrowse,
                onOpenContinueWatching = {
                    viewModel.setSelectedLibraryFilterType(LibraryFilterType.IN_PROGRESS)
                    viewModel.setSelectedLibrarySortBy(LibrarySortBy.LIBRARY)
                    viewModel.selectFullLibraryBrowse()
                },
                onOpenTopRated = {
                    viewModel.setSelectedLibraryFilterType(LibraryFilterType.TOP_RATED)
                    viewModel.setSelectedLibrarySortBy(LibrarySortBy.RATING)
                    viewModel.selectFullLibraryBrowse()
                },
	                onOpenFresh = {
	                    viewModel.setSelectedLibraryFilterType(LibraryFilterType.RECENTLY_UPDATED)
	                    viewModel.setSelectedLibrarySortBy(LibrarySortBy.RELEASE)
	                    viewModel.selectFullLibraryBrowse()
	                },
	                onOpenVodGuide = viewModel::openVodGuide,
	                onOpenAdultVodGuide = viewModel::openAdultVodGuide,
	                onLoadMore = viewModel::loadMoreSelectedCategory,
                onPreviewAdultVodMovie = viewModel::previewAdultVodMovie,
                onBeginAdultVodPreviewHandoff = viewModel::beginAdultVodPreviewHandoff,
                onLoadMorePreviewRows = viewModel::loadMorePreviewRows,
                onDismissReorder = viewModel::exitCategoryReorderMode,
                initialFocusRequester = initialContentFocusRequester
            )
        }
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (uiState.showDialog && uiState.selectedMovieForDialog != null) {
        val movie = uiState.selectedMovieForDialog!!
        com.afterglowtv.app.ui.components.dialogs.AddToGroupDialog(
            contentTitle = movie.name,
            groups = uiState.categories.filter { it.isVirtual && it.id != VodBrowseDefaults.FAVORITES_SENTINEL_ID },
            isFavorite = movie.isFavorite,
            memberOfGroups = uiState.dialogGroupMemberships,
            onDismiss = { viewModel.onDismissDialog() },
            onToggleFavorite = {
                if (movie.isFavorite) viewModel.removeFavorite(movie) else viewModel.addFavorite(movie)
            },
            onAddToGroup = { group -> viewModel.addToGroup(movie, group) },
            onRemoveFromGroup = { group -> viewModel.removeFromGroup(movie, group) },
            onCreateGroup = { name -> viewModel.createCustomGroup(name) }
        )
    }

    if (uiState.selectedCategoryForOptions != null) {
        val category = uiState.selectedCategoryForOptions!!
        com.afterglowtv.app.ui.components.dialogs.CategoryOptionsDialog(
            category = category,
            onDismissRequest = { viewModel.dismissCategoryOptions() },
            onHide = if (!category.isVirtual) {
                { viewModel.hideCategory(category) }
            } else null,
            onRename = if (category.isVirtual && category.id != VodBrowseDefaults.FAVORITES_SENTINEL_ID) {
                { viewModel.requestRenameGroup(category) }
            } else null,
            onDelete = if (category.isVirtual && category.id != VodBrowseDefaults.FAVORITES_SENTINEL_ID) {
                {
                    viewModel.requestDeleteGroup(category)
                }
            } else null,
            onReorderChannels = if (category.isVirtual) {
                { viewModel.enterCategoryReorderMode(category) }
            } else null
        )
    }

    if (uiState.showRenameGroupDialog && uiState.groupToRename != null) {
        RenameGroupDialog(
            initialName = uiState.groupToRename!!.name,
            errorMessage = uiState.renameGroupError,
            onDismissRequest = { viewModel.cancelRenameGroup() },
            onConfirm = { name -> viewModel.confirmRenameGroup(name) }
        )
    }

    if (uiState.showDeleteGroupDialog && uiState.groupToDelete != null) {
        DeleteGroupDialog(
            groupName = uiState.groupToDelete!!.name,
            onDismissRequest = { viewModel.cancelDeleteGroup() },
            onConfirmDelete = { viewModel.confirmDeleteGroup() }
        )
    }
}

@Composable
private fun MoviesVodContent(
    uiState: MoviesUiState,
    selectedFilterType: LibraryFilterType,
    onSelectedFilterTypeChange: (LibraryFilterType) -> Unit,
    selectedSortBy: LibrarySortBy,
    onSelectedSortByChange: (LibrarySortBy) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onContinueWatchingPlay: (PlaybackHistory) -> Unit,
    onProtectedMovieClick: (Movie) -> Unit,
    onProtectedCategoryClick: (Category) -> Unit,
    onShowDialog: (Movie) -> Unit,
    onShowCategoryOptions: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectFullLibraryBrowse: () -> Unit,
    onOpenContinueWatching: () -> Unit,
    onOpenTopRated: () -> Unit,
    onOpenFresh: () -> Unit,
    onOpenVodGuide: () -> Unit,
    onOpenAdultVodGuide: () -> Unit,
    onLoadMore: () -> Unit,
    onPreviewAdultVodMovie: (Movie) -> Unit,
    onBeginAdultVodPreviewHandoff: (Movie) -> Boolean,
    onLoadMorePreviewRows: () -> Unit,
    onDismissReorder: () -> Unit,
    initialFocusRequester: FocusRequester
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val favoriteCardWidth = when {
        screenWidth < 700.dp -> 136.dp
        !isTelevisionDevice && screenWidth < 900.dp -> 148.dp
        !isTelevisionDevice && screenWidth < 1280.dp -> 152.dp
        else -> 160.dp
    }
    val loadingSectionHeight = when {
        screenWidth < 700.dp -> 220.dp
        !isTelevisionDevice && screenWidth < 1280.dp -> 260.dp
        else -> 300.dp
    }
    var showCategoryPicker by remember { mutableStateOf(false) }
    val favoriteMovies = uiState.moviesByCategory[uiState.favoriteCategoryName].orEmpty()
    val freshMovies = uiState.libraryLensRows[MovieLibraryLens.FRESH].orEmpty()
    val topRatedMovies = uiState.libraryLensRows[MovieLibraryLens.TOP_RATED].orEmpty()
    val continueWatching = uiState.continueWatching
    val heroMovie = freshMovies.firstOrNull() ?: topRatedMovies.firstOrNull() ?: favoriteMovies.firstOrNull()
    val categoryByName = remember(uiState.providerCategories, uiState.categories, uiState.favoriteCategoryName) {
        buildMap<String, Category> {
            uiState.providerCategories.forEach { put(it.name, it) }
            uiState.categories.forEach { put(it.name, it) }
            put(
                uiState.favoriteCategoryName,
                Category(
                    id = VodBrowseDefaults.FAVORITES_SENTINEL_ID,
                    name = uiState.favoriteCategoryName,
                    type = com.afterglowtv.domain.model.ContentType.MOVIE,
                    isVirtual = true
                )
            )
        }
    }
    val isCategoryLocked = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds, categoryByName) {
        { category: Category ->
            category.isUserProtected &&
                uiState.parentalControlLevel in 1..2 &&
                kotlin.math.abs(category.id) !in uiState.unlockedCategoryIds
        }
    }
    val isCategoryHidden = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds, categoryByName) {
        { category: Category ->
            category.isUserProtected &&
                uiState.parentalControlLevel >= 3 &&
                kotlin.math.abs(category.id) !in uiState.unlockedCategoryIds
        }
    }
    val isMovieLocked = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds) {
        { movie: Movie ->
            val categoryId = movie.categoryId
            movie.isUserProtected &&
                uiState.parentalControlLevel in 1..2 &&
                (categoryId == null || kotlin.math.abs(categoryId) !in uiState.unlockedCategoryIds)
        }
    }
    val openProtectedCategory: (Category) -> Unit = onProtectedCategoryClick
    val browseCategoryNames = remember(uiState.categoryNames, uiState.providerCategories, uiState.categories, uiState.favoriteCategoryName) {
        buildList {
            val names = linkedSetOf<String>()
            uiState.categoryNames.forEach(names::add)
            uiState.providerCategories.forEach { names.add(it.name) }
            uiState.categories.forEach { names.add(it.name) }
            names.add(uiState.favoriteCategoryName)
            addAll(names)
        }
    }
    val visibleCategoryNames = remember(browseCategoryNames, categoryByName, uiState.parentalControlLevel, uiState.unlockedCategoryIds) {
        browseCategoryNames.filter { name ->
            val category = categoryByName[name]
            category == null || !isCategoryHidden(category)
        }
    }
    val visibleCategoryNameSet = remember(visibleCategoryNames) {
        visibleCategoryNames.toSet()
    }
    val catEntries = remember(uiState.moviesByCategory, visibleCategoryNameSet, uiState.favoriteCategoryName) {
        uiState.moviesByCategory.entries
            .filter { (name, items) ->
                name != uiState.favoriteCategoryName && name in visibleCategoryNameSet && items.isNotEmpty()
            }
            .toList()
    }
    val fallbackMovieId = if (heroMovie == null) {
        favoriteMovies.firstOrNull()?.id
            ?: freshMovies.firstOrNull()?.id
            ?: topRatedMovies.firstOrNull()?.id
            ?: catEntries.firstOrNull()?.value?.firstOrNull()?.id
    } else null
    val categoryOptions = remember(visibleCategoryNames, uiState.categoryCounts, categoryByName, uiState.parentalControlLevel, uiState.unlockedCategoryIds) {
        visibleCategoryNames.map { name ->
            val matchedCategory = categoryByName[name]
            val lockedCategory = matchedCategory?.takeIf(isCategoryLocked)
            val locked = lockedCategory != null
            VodCategoryOption(
                name = name,
                count = uiState.categoryCounts[name] ?: 0,
                onClick = {
                    if (lockedCategory != null) openProtectedCategory(lockedCategory) else onSelectCategory(name)
                },
                onLongClick = matchedCategory?.takeIf { !locked }?.let { category ->
                    { onShowCategoryOptions(category.name) }
                },
                isLocked = locked
            )
        }
    }

    if (showCategoryPicker) {
        VodCategoryPickerDialog(
            title = stringResource(R.string.vod_category_picker_title),
            subtitle = stringResource(R.string.vod_category_picker_subtitle),
            categories = categoryOptions,
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (uiState.vodViewMode == VodViewMode.GUIDE) {
        MoviesVodGuideContent(
            uiState = uiState,
            selectedFilterType = selectedFilterType,
            onSelectedFilterTypeChange = onSelectedFilterTypeChange,
            selectedSortBy = selectedSortBy,
            onSelectedSortByChange = onSelectedSortByChange,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onMovieClick = onMovieClick,
            onProtectedMovieClick = onProtectedMovieClick,
            onShowDialog = onShowDialog,
            onSelectFullLibraryBrowse = onSelectFullLibraryBrowse,
            onOpenVodGuide = onOpenVodGuide,
            onOpenAdultVodGuide = onOpenAdultVodGuide,
            onLoadMore = onLoadMore,
            onPreviewAdultVodMovie = onPreviewAdultVodMovie,
            onBeginAdultVodPreviewHandoff = onBeginAdultVodPreviewHandoff,
            initialFocusRequester = initialFocusRequester
        )
        return
    }

    LaunchedEffect(uiState.vodViewMode, uiState.selectedCategory, uiState.isReorderMode) {
        if (uiState.vodViewMode == VodViewMode.GRID && uiState.selectedCategory == null && !uiState.isReorderMode) {
            onSelectFullLibraryBrowse()
        }
    }

    if (uiState.vodViewMode == VodViewMode.GRID && uiState.selectedCategory == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (uiState.selectedCategory == null) {
        val previewListState = androidx.compose.foundation.lazy.rememberLazyListState()
        InfiniteScrollEffect(
            listState = previewListState,
            enabled = uiState.vodInfiniteScroll,
            canLoadMore = uiState.hasMorePreviewRows,
            isLoading = uiState.isLoadingPreviewRows,
            onLoadMore = onLoadMorePreviewRows
        )
        LazyColumn(
            state = previewListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item(key = "hero") {
            if (heroMovie != null) {
                VodHeroStrip(
                        title = heroMovie.name,
                        subtitle = heroMovie.plot?.takeIf { it.isNotBlank() }
                            ?: heroMovie.year
                            ?: stringResource(R.string.movies_library_lens_subtitle),
                        actionLabel = stringResource(R.string.player_resume).substringBefore(" "),
                        onClick = {
                            val isLocked = isMovieLocked(heroMovie)
                            if (isLocked) onProtectedMovieClick(heroMovie) else onMovieClick(heroMovie)
                        },
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 6.dp)
                            .focusRequester(initialFocusRequester)
                    )
            }
            }
            item(key = "actions") {
            VodActionChipRow(
                    actions = buildList {
	                        add(
	                            VodActionChip(
	                                key = "browse_all",
	                                label = stringResource(R.string.library_full_browse_title_movies),
	                                detail = stringResource(R.string.library_full_browse_subtitle, uiState.libraryCount),
	                                onClick = onSelectFullLibraryBrowse
	                            )
	                        )
	                        add(
	                            VodActionChip(
	                                key = "vod_guide",
	                                label = stringResource(R.string.vod_guide_title),
	                                detail = stringResource(R.string.vod_guide_detail),
	                                onClick = onOpenVodGuide
	                            )
	                        )
	                        add(
	                            VodActionChip(
	                                key = "xxx_vod_guide",
	                                label = stringResource(R.string.adult_on_demand_title),
	                                detail = stringResource(R.string.adult_on_demand_detail),
	                                onClick = onOpenAdultVodGuide
	                            )
	                        )
	                        add(
	                            VodActionChip(
	                                key = "categories",
                                label = stringResource(R.string.movies_categories_title),
                                detail = "${visibleCategoryNames.count { name -> categoryByName[name]?.id != VodBrowseDefaults.FAVORITES_SENTINEL_ID }} groups",
                                onClick = { showCategoryPicker = true }
                            )
                        )
                        if (favoriteMovies.isNotEmpty()) {
                            add(
                                VodActionChip(
                                    key = "favorites",
                                    label = stringResource(R.string.favorites_title),
                                    detail = stringResource(R.string.library_saved_items_count, favoriteMovies.size),
                                    onClick = { onSelectCategory(uiState.favoriteCategoryName) }
                                )
                            )
                        }
                        if (continueWatching.isNotEmpty()) {
                            add(
                                VodActionChip(
                                    key = "resume",
                                    label = stringResource(R.string.library_lens_continue),
                                    detail = "${continueWatching.size} items",
                                    onClick = onOpenContinueWatching
                                )
                            )
                        }
                        if (topRatedMovies.isNotEmpty()) {
                            add(
                                VodActionChip(
                                    key = MovieLibraryLens.TOP_RATED.name,
                                    label = stringResource(R.string.library_lens_top_rated),
                                    detail = "${topRatedMovies.size} picks",
                                    onClick = onOpenTopRated
                                )
                            )
                        }
                        if (freshMovies.isNotEmpty()) {
                            add(
                                VodActionChip(
                                    key = MovieLibraryLens.FRESH.name,
                                    label = stringResource(R.string.library_lens_fresh_movies),
                                    detail = "${freshMovies.size} picks",
                                    onClick = onOpenFresh
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
            }
            if (continueWatching.isNotEmpty()) {
            item(key = "continue_watching") {
                ContinueWatchingRow(
                        items = continueWatching,
                        onItemClick = onContinueWatchingPlay
                    )
            }
            }
            if (favoriteMovies.isNotEmpty()) {
            item(key = "favorites_row") {
                CategoryRow(
                    title = stringResource(R.string.favorites_title),
                    items = favoriteMovies,
                    onSeeAll = { onSelectCategory(uiState.favoriteCategoryName) },
                    keySelector = { it.id }
                ) { movie ->
                        val isLocked = isMovieLocked(movie)
                        MovieCard(
                            movie = movie,
                            isLocked = isLocked,
                            onClick = {
                                if (isLocked) onProtectedMovieClick(movie) else onMovieClick(movie)
                            },
                            onLongClick = { onShowDialog(movie) },
                            modifier = Modifier.width(favoriteCardWidth)
                        )
                }
            }
            }
            if (freshMovies.isNotEmpty()) {
            item(key = "fresh_row") {
                CategoryRow(
                    title = stringResource(R.string.library_lens_fresh_movies),
                    items = freshMovies,
                    onSeeAll = null,
                    keySelector = { it.id }
                ) { movie ->
                        val isLocked = isMovieLocked(movie)
                        MovieCard(
                            movie = movie,
                            isLocked = isLocked,
                            onClick = { if (isLocked) onProtectedMovieClick(movie) else onMovieClick(movie) },
                            onLongClick = { onShowDialog(movie) }
                        )
                }
            }
            }
            if (topRatedMovies.isNotEmpty()) {
            item(key = "top_rated_row") {
                CategoryRow(
                    title = stringResource(R.string.library_lens_top_rated),
                    items = topRatedMovies,
                    onSeeAll = null,
                    keySelector = { it.id }
                ) { movie ->
                        val isLocked = isMovieLocked(movie)
                        MovieCard(
                            movie = movie,
                            isLocked = isLocked,
                            onClick = { if (isLocked) onProtectedMovieClick(movie) else onMovieClick(movie) },
                            onLongClick = { onShowDialog(movie) }
                        )
                }
            }
            }
            items(catEntries, key = { it.key }) { entry ->
                val categoryName = entry.key
                val movies = entry.value
                val matchedCategory = categoryByName[categoryName]
                val lockedCategory = matchedCategory?.takeIf(isCategoryLocked)
                CategoryRow(
                    title = categoryName,
                    items = movies,
                    onSeeAll = {
                        if (lockedCategory != null) openProtectedCategory(lockedCategory) else onSelectCategory(categoryName)
                    },
                    keySelector = { it.id }
                ) { movie ->
                    val isLocked = isMovieLocked(movie)
                    MovieCard(
                        movie = movie,
                        isLocked = isLocked,
                        onClick = { if (isLocked) onProtectedMovieClick(movie) else onMovieClick(movie) },
                        onLongClick = { onShowDialog(movie) }
                        ,
                        modifier = if (movie.id == fallbackMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier
                    )
                }
            }
            if (uiState.isLoadingPreviewRows) {
                item(key = "preview_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            if (uiState.hasMorePreviewRows && !uiState.isLoadingPreviewRows && !uiState.vodInfiniteScroll && catEntries.isNotEmpty()) {
                item(key = "load_more_preview_trigger") {
                    LoadMoreCard(
                        label = stringResource(
                            R.string.library_load_more,
                            catEntries.size,
                            visibleCategoryNames.size
                        ),
                        onClick = onLoadMorePreviewRows,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
        return
    }

    val baseMovies = uiState.selectedCategoryItems
    val filteredGridMovies = remember(baseMovies, uiState.isReorderMode, uiState.filteredMovies) {
        if (uiState.isReorderMode) uiState.filteredMovies else baseMovies
    }
    var draggingMovie by remember { mutableStateOf<Movie?>(null) }
    var showBrowseOptions by rememberSaveable(uiState.selectedCategory) { mutableStateOf(false) }
    var showSearchBar by rememberSaveable(uiState.selectedCategory) { mutableStateOf(searchQuery.isNotBlank()) }
    val initialGridMovieId = filteredGridMovies.firstOrNull()?.id

    if (showBrowseOptions) {
        VodBrowseOptionsDialog(
            title = stringResource(R.string.nav_movies),
            filterTitle = stringResource(R.string.library_filter_title),
            filterChips = movieFilterChips(),
            selectedFilterKey = selectedFilterType.name,
            onFilterSelected = { key ->
                LibraryFilterType.entries.firstOrNull { it.name == key }?.let(onSelectedFilterTypeChange)
            },
            sortTitle = stringResource(R.string.library_sort_title),
            sortChips = movieSortChips(),
            selectedSortKey = selectedSortBy.name,
            onSortSelected = { key ->
                LibrarySortBy.entries.firstOrNull { it.name == key }?.let(onSelectedSortByChange)
            },
            onDismiss = { showBrowseOptions = false }
        )
    }

    val modernGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    InfiniteScrollEffect(
        gridState = modernGridState,
        enabled = !uiState.isReorderMode,
        canLoadMore = uiState.canLoadMoreSelectedCategory,
        isLoading = uiState.isLoadingSelectedCategory,
        onLoadMore = onLoadMore
    )
    LazyVerticalGrid(
        state = modernGridState,
        columns = GridCells.Adaptive(minSize = 136.dp),
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (uiState.isReorderMode && event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                        draggingMovie = null
                        onDismissReorder()
                        true
                    } else false
                } else false
            },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            VodSectionHeader(
                title = when (uiState.selectedCategory) {
                    uiState.fullLibraryCategoryName -> stringResource(R.string.library_full_browse_title_movies)
                    else -> uiState.selectedCategory ?: stringResource(R.string.nav_movies)
                }
            )
        }

        if (!uiState.isReorderMode) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val browseOptionsDetail = vodActiveFilterSortDetail(selectedFilterType, selectedSortBy)
                val hasActiveFilterSort = selectedFilterType != LibraryFilterType.ALL || selectedSortBy != LibrarySortBy.LIBRARY
                VodActionChipRow(
                    actions = buildList {
                        add(
                            VodActionChip(
                                key = "back_home",
                                label = stringResource(R.string.nav_movies),
                                onClick = { onSelectCategory(null) }
                            )
                        )
                        add(
                            VodActionChip(
                                key = "categories",
                                label = stringResource(R.string.movies_categories_title),
                                onClick = { showCategoryPicker = true }
                            )
                        )
                        add(
                            VodActionChip(
                                key = "search_toggle",
                                label = stringResource(
                                    if (showSearchBar) R.string.library_action_hide_search else R.string.search_title
                                ),
                                onClick = { showSearchBar = !showSearchBar }
                            )
                        )
                        add(
                            VodActionChip(
                                key = "browse_options",
                                label = stringResource(R.string.library_action_filters_sort),
                                detail = browseOptionsDetail,
                                onClick = { showBrowseOptions = true }
                            )
                        )
                        if (uiState.selectedCategory != uiState.fullLibraryCategoryName) {
                            add(
                                VodActionChip(
                                    key = uiState.fullLibraryCategoryName,
                                    label = stringResource(R.string.library_full_browse_title_movies),
                                    onClick = onSelectFullLibraryBrowse
                                )
                            )
                        }
                    },
                    selectedKey = if (hasActiveFilterSort) "browse_options" else null,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )
            }

            if (showSearchBar) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchInput(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = stringResource(R.string.movies_search_placeholder),
                        onSearch = {},
                        focusRequester = initialFocusRequester,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (uiState.isLoadingSelectedCategory) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(loadingSectionHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = stringResource(R.string.movies_loading),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else if (filteredGridMovies.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(loadingSectionHeight),
                    contentAlignment = Alignment.Center
                ) {
                    AppMessageState(
                        title = stringResource(R.string.movies_no_found),
                        subtitle = stringResource(R.string.movies_no_found_subtitle)
                    )
                }
            }
        } else {
            gridItems(filteredGridMovies, key = { it.id }) { movie ->
                val isLocked = isMovieLocked(movie)
                val isDraggingThis = draggingMovie == movie
                MovieCard(
                    movie = movie,
                    isLocked = isLocked,
                    isReorderMode = uiState.isReorderMode,
                    isDragging = isDraggingThis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .then(if (!showSearchBar && movie.id == initialGridMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier),
                    onClick = {
                        if (uiState.isReorderMode) {
                            draggingMovie = if (isDraggingThis) null else movie
                        } else if (isLocked) {
                            onProtectedMovieClick(movie)
                        } else {
                            onMovieClick(movie)
                        }
                    },
                    onLongClick = {
                        if (!uiState.isReorderMode) onShowDialog(movie)
                    }
                )
            }
        }
    }
}

@Composable
private fun MoviesVodGuideContent(
    uiState: MoviesUiState,
    selectedFilterType: LibraryFilterType,
    onSelectedFilterTypeChange: (LibraryFilterType) -> Unit,
    selectedSortBy: LibrarySortBy,
    onSelectedSortByChange: (LibrarySortBy) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onProtectedMovieClick: (Movie) -> Unit,
    onShowDialog: (Movie) -> Unit,
    onSelectFullLibraryBrowse: () -> Unit,
    onOpenVodGuide: () -> Unit,
    onOpenAdultVodGuide: () -> Unit,
    onLoadMore: () -> Unit,
    onPreviewAdultVodMovie: (Movie) -> Unit,
    onBeginAdultVodPreviewHandoff: (Movie) -> Boolean,
    initialFocusRequester: FocusRequester
) {
    var showBrowseOptions by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }
    val categoryById = remember(uiState.providerCategories) {
        uiState.providerCategories.associate { kotlin.math.abs(it.id) to it.name }
    }
    val isMovieLocked = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds) {
        { movie: Movie ->
            val categoryId = movie.categoryId
            movie.isUserProtected &&
                uiState.parentalControlLevel in 1..2 &&
                (categoryId == null || kotlin.math.abs(categoryId) !in uiState.unlockedCategoryIds)
        }
    }

    LaunchedEffect(uiState.vodViewMode, uiState.selectedCategory, uiState.isReorderMode) {
        if (
            uiState.vodViewMode == VodViewMode.GUIDE &&
            uiState.selectedCategory != uiState.fullLibraryCategoryName &&
            !uiState.isReorderMode
        ) {
            onSelectFullLibraryBrowse()
        }
    }

    if (showBrowseOptions) {
        VodBrowseOptionsDialog(
            title = stringResource(R.string.settings_vod_view_mode_guide),
            filterTitle = stringResource(R.string.library_filter_title),
            filterChips = movieFilterChips(),
            selectedFilterKey = selectedFilterType.name,
            onFilterSelected = { key ->
                LibraryFilterType.entries.firstOrNull { it.name == key }?.let(onSelectedFilterTypeChange)
            },
            sortTitle = stringResource(R.string.library_sort_title),
            sortChips = movieSortChips(),
            selectedSortKey = selectedSortBy.name,
            onSortSelected = { key ->
                LibrarySortBy.entries.firstOrNull { it.name == key }?.let(onSelectedSortByChange)
            },
            onDismiss = { showBrowseOptions = false }
        )
    }

    val guideMovies = uiState.selectedCategoryItems
    val guideRows = remember(guideMovies, categoryById) {
        VodGuideRowBuilder.build(
            items = guideMovies.map { movie ->
                val displayTitle = VodTitleFormatter.format(movie.name, movie.year)
                VodGuideItem(
                    id = movie.id.toString(),
                    title = displayTitle.title,
                    providerCategory = if (uiState.showAdultVodGuide) {
                        AdultGuideCategoryBuilder.resolveVodCategoryTitle(
                            title = movie.name,
                            providerCategory = movie.categoryName
                                ?: movie.categoryId?.let { categoryById[kotlin.math.abs(it)] }
                        )
                    } else {
                        movie.categoryName
                            ?: movie.categoryId?.let { categoryById[kotlin.math.abs(it)] }
                    }
                )
            },
            uncategorizedTitle = "Other"
        )
    }
    val moviesById = remember(guideMovies) {
        guideMovies.associateBy { it.id.toString() }
    }
    val waitingForFullLibrary = uiState.selectedCategory != uiState.fullLibraryCategoryName
    val guideListState = androidx.compose.foundation.lazy.rememberLazyListState()

    InfiniteScrollEffect(
        listState = guideListState,
        enabled = !waitingForFullLibrary,
        canLoadMore = uiState.canLoadMoreSelectedCategory,
        isLoading = uiState.isLoadingSelectedCategory,
        onLoadMore = onLoadMore
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
	        val hasActiveFilterSort = selectedFilterType != LibraryFilterType.ALL || selectedSortBy != LibrarySortBy.LIBRARY
	        VodClassicContentHeader(
	            title = stringResource(
	                if (uiState.showAdultVodGuide) R.string.nav_adult_guide else R.string.vod_guide_title
	            ),
	            subtitle = stringResource(R.string.vod_classic_results_count, uiState.selectedCategoryTotalCount),
	            actions = buildList {
	                add(
	                    VodActionChip(
	                        key = if (uiState.showAdultVodGuide) "vod_guide" else "xxx_vod_guide",
	                        label = stringResource(
	                            if (uiState.showAdultVodGuide) R.string.nav_movies else R.string.adult_on_demand_title
	                        ),
	                        onClick = if (uiState.showAdultVodGuide) onOpenVodGuide else onOpenAdultVodGuide
	                    )
	                )
	                add(
	                    VodActionChip(
	                        key = "search_toggle",
                        label = stringResource(
                            if (showSearchBar) R.string.library_action_hide_search else R.string.search_title
                        ),
                        onClick = { showSearchBar = !showSearchBar }
                    )
                )
                add(
                    VodActionChip(
                        key = "browse_options",
                        label = stringResource(R.string.library_action_filters_sort),
                        detail = vodActiveFilterSortDetail(selectedFilterType, selectedSortBy),
                        onClick = { showBrowseOptions = true }
                    )
                )
            },
            selectedActionKey = if (hasActiveFilterSort) "browse_options" else null,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (showSearchBar) {
            SearchInput(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(R.string.movies_search_placeholder),
                onSearch = {},
                focusRequester = initialFocusRequester,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (uiState.showAdultVodGuide) 12.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        LazyColumn(
            state = guideListState,
            modifier = Modifier
                .weight(if (uiState.showAdultVodGuide) 1.08f else 1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                waitingForFullLibrary || uiState.isLoadingSelectedCategory -> {
                    item(key = "guide_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
                guideRows.isEmpty() -> {
                    item(key = "guide_empty") {
                        AppMessageState(
                            title = stringResource(R.string.movies_no_found),
                            subtitle = stringResource(R.string.movies_no_found_subtitle)
                        )
                    }
                }
                else -> {
                    itemsIndexed(guideRows, key = { _, row -> row.title }) { rowIndex, row ->
                        val programs = row.items.mapNotNull { guideItem ->
                            val movie = moviesById[guideItem.id] ?: return@mapNotNull null
                            val displayTitle = VodTitleFormatter.format(movie.name, movie.year)
                            VodGuideProgramCard(
                                key = movie.id.toString(),
                                title = displayTitle.title,
                                subtitle = vodMetadata(
                                    displayTitle.year ?: movie.year,
                                    movie.duration ?: formatDurationSeconds(movie.durationSeconds)
                                ),
                                imageUrl = movie.backdropUrl ?: movie.posterUrl,
                                badge = movie.genre?.substringBefore(",")?.trim()?.takeIf(String::isNotBlank)
                                    ?: movie.containerExtension?.uppercase(),
                                isLocked = isMovieLocked(movie),
                                textFirst = uiState.showAdultVodGuide,
                                topLabel = if (uiState.showAdultVodGuide) "VOD" else null,
                                onClick = {
                                    when {
                                        isMovieLocked(movie) -> onProtectedMovieClick(movie)
                                        uiState.showAdultVodGuide && uiState.adultVodPreviewMovieId == movie.id -> {
                                            val handedOff = onBeginAdultVodPreviewHandoff(movie)
                                            if (handedOff) onMovieClick(movie) else onPreviewAdultVodMovie(movie)
                                        }
                                        uiState.showAdultVodGuide -> onPreviewAdultVodMovie(movie)
                                        else -> onMovieClick(movie)
                                    }
                                },
                                onLongClick = { onShowDialog(movie) }
                            )
                        }
                        VodGuideLane(
                            title = row.title,
                            subtitle = "${programs.size} titles",
                            programs = programs,
                            initialFocusRequester = if (!showSearchBar && rowIndex == 0) initialFocusRequester else null,
                            rowHeight = if (uiState.showAdultVodGuide) 86.dp else 118.dp,
                            programWidth = if (uiState.showAdultVodGuide) 250.dp else 300.dp,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                    if (uiState.canLoadMoreSelectedCategory && !uiState.isLoadingSelectedCategory) {
                        item(key = "guide_load_more") {
                            LoadMoreCard(
                                label = stringResource(
                                    R.string.library_load_more,
                                    uiState.selectedCategoryLoadedCount,
                                    uiState.selectedCategoryTotalCount
                                ),
                                onClick = onLoadMore,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        if (uiState.showAdultVodGuide) {
            AdultVodPreviewPane(
                movie = guideMovies.firstOrNull { it.id == uiState.adultVodPreviewMovieId },
                playerEngine = uiState.adultVodPreviewPlayerEngine,
                isLoading = uiState.isAdultVodPreviewLoading,
                errorMessage = uiState.adultVodPreviewErrorMessage,
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight()
            )
        }
        }
    }
}

@Composable
private fun AdultVodPreviewPane(
    movie: Movie?,
    playerEngine: PlayerEngine?,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val renderSurfaceType by (playerEngine?.renderSurfaceType)?.collectAsStateWithLifecycle(
        initialValue = PlayerRenderSurfaceType.SURFACE_VIEW
    ) ?: remember { mutableStateOf(PlayerRenderSurfaceType.SURFACE_VIEW) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleSmall,
                color = Primary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (movie != null && playerEngine != null && errorMessage == null) {
                    PlayerRenderView(
                        playerEngine = playerEngine,
                        resizeMode = PlayerSurfaceResizeMode.FIT,
                        surfaceType = renderSurfaceType,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Select a title to preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnBackground
                        )
                        Text(
                            text = errorMessage ?: "Press OK to load a preview. Press OK again for full screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                    }
                }

                if (isLoading && movie != null) {
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Loading preview...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            if (movie != null) {
                val displayTitle = VodTitleFormatter.format(movie.name, movie.year)
                Text(
                    text = displayTitle.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                vodMetadata(displayTitle.year ?: movie.year, movie.duration ?: formatDurationSeconds(movie.durationSeconds))
                    ?.let { metadata ->
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                Text(
                    text = "VOD",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentAmber
                )
                Text(
                    text = "Press OK again to open playback",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun MoviesVodClassicContent(
    uiState: MoviesUiState,
    selectedFilterType: LibraryFilterType,
    onSelectedFilterTypeChange: (LibraryFilterType) -> Unit,
    selectedSortBy: LibrarySortBy,
    onSelectedSortByChange: (LibrarySortBy) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onProtectedMovieClick: (Movie) -> Unit,
    onProtectedCategoryClick: (Category) -> Unit,
    onShowDialog: (Movie) -> Unit,
    onShowCategoryOptions: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectFullLibraryBrowse: () -> Unit,
    onOpenContinueWatching: () -> Unit,
    onOpenFresh: () -> Unit,
    onLoadMore: () -> Unit,
    onDismissReorder: () -> Unit,
    initialFocusRequester: FocusRequester
) {
    val allLabel = stringResource(R.string.vod_classic_all)
    val continueLabel = stringResource(R.string.vod_classic_continue_watching)
    val recentLabel = stringResource(R.string.vod_classic_recently_added)
    val categoryByName = remember(uiState.providerCategories, uiState.categories, uiState.favoriteCategoryName) {
        buildMap<String, Category> {
            uiState.providerCategories.forEach { put(it.name, it) }
            uiState.categories.forEach { put(it.name, it) }
            put(
                uiState.favoriteCategoryName,
                Category(
                    id = VodBrowseDefaults.FAVORITES_SENTINEL_ID,
                    name = uiState.favoriteCategoryName,
                    type = com.afterglowtv.domain.model.ContentType.MOVIE,
                    isVirtual = true
                )
            )
        }
    }
    val isCategoryLocked = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds, categoryByName) {
        { category: Category ->
            category.isUserProtected &&
                uiState.parentalControlLevel in 1..2 &&
                kotlin.math.abs(category.id) !in uiState.unlockedCategoryIds
        }
    }
    val isCategoryHidden = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds, categoryByName) {
        { category: Category ->
            category.isUserProtected &&
                uiState.parentalControlLevel >= 3 &&
                kotlin.math.abs(category.id) !in uiState.unlockedCategoryIds
        }
    }
    val isMovieLocked = remember(uiState.parentalControlLevel, uiState.unlockedCategoryIds) {
        { movie: Movie ->
            val categoryId = movie.categoryId
            movie.isUserProtected &&
                uiState.parentalControlLevel in 1..2 &&
                (categoryId == null || kotlin.math.abs(categoryId) !in uiState.unlockedCategoryIds)
        }
    }
    val openProtectedCategory: (Category) -> Unit = onProtectedCategoryClick
    val browseCategoryNames = remember(uiState.categoryNames, uiState.providerCategories, uiState.categories, uiState.favoriteCategoryName) {
        buildList {
            val names = linkedSetOf<String>()
            uiState.categoryNames.forEach(names::add)
            uiState.providerCategories.forEach { names.add(it.name) }
            uiState.categories.forEach { names.add(it.name) }
            names.add(uiState.favoriteCategoryName)
            addAll(names)
        }
    }
    val visibleCategoryNames = remember(browseCategoryNames, categoryByName, uiState.parentalControlLevel, uiState.unlockedCategoryIds) {
        browseCategoryNames.filter { name ->
            val category = categoryByName[name]
            category == null || !isCategoryHidden(category)
        }
    }
    var categoryQuery by rememberSaveable { mutableStateOf("") }
    var showBrowseOptions by rememberSaveable(uiState.selectedCategory) { mutableStateOf(false) }
    var showSearchBar by rememberSaveable(uiState.selectedCategory) { mutableStateOf(searchQuery.isNotBlank()) }
    val baseMovies = uiState.selectedCategoryItems
    val filteredGridMovies = remember(baseMovies, uiState.isReorderMode, uiState.filteredMovies) {
        if (uiState.isReorderMode) uiState.filteredMovies else baseMovies
    }
    var draggingMovie by remember { mutableStateOf<Movie?>(null) }
    val initialGridMovieId = filteredGridMovies.firstOrNull()?.id

    LaunchedEffect(uiState.vodViewMode, uiState.selectedCategory, uiState.isReorderMode) {
        if (uiState.vodViewMode == VodViewMode.GUIDE && uiState.selectedCategory == null && !uiState.isReorderMode) {
            onSelectFullLibraryBrowse()
        }
    }

    if (showBrowseOptions) {
        VodBrowseOptionsDialog(
            title = stringResource(R.string.nav_movies),
            filterTitle = stringResource(R.string.library_filter_title),
            filterChips = movieFilterChips(),
            selectedFilterKey = selectedFilterType.name,
            onFilterSelected = { key ->
                LibraryFilterType.entries.firstOrNull { it.name == key }?.let(onSelectedFilterTypeChange)
            },
            sortTitle = stringResource(R.string.library_sort_title),
            sortChips = movieSortChips(),
            selectedSortKey = selectedSortBy.name,
            onSortSelected = { key ->
                LibrarySortBy.entries.firstOrNull { it.name == key }?.let(onSelectedSortByChange)
            },
            onDismiss = { showBrowseOptions = false }
        )
    }

    val selectedKey = when {
        uiState.selectedCategory == uiState.favoriteCategoryName -> "favorites"
        uiState.selectedCategory == uiState.fullLibraryCategoryName && selectedFilterType == LibraryFilterType.IN_PROGRESS -> "continue"
        uiState.selectedCategory == uiState.fullLibraryCategoryName && selectedFilterType == LibraryFilterType.RECENTLY_UPDATED -> "recent"
        uiState.selectedCategory == null || uiState.selectedCategory == uiState.fullLibraryCategoryName -> "all"
        else -> "category:${uiState.selectedCategory}"
    }
    val continueCount = remember(uiState.continueWatching) {
        uiState.continueWatching.map { it.contentId }.distinct().size
    }
    val recentCount = uiState.libraryLensRows[MovieLibraryLens.FRESH]?.size ?: 0
    val railOptions = remember(
        visibleCategoryNames,
        uiState.categoryCounts,
        uiState.favoriteCategoryName,
        uiState.selectedCategory,
        selectedFilterType,
        categoryQuery,
        continueCount,
        recentCount,
        uiState.libraryCount,
        uiState.unlockedCategoryIds,
        uiState.parentalControlLevel
    ) {
        buildList {
            add(
                VodClassicCategoryOption(
                    key = "all",
                    label = allLabel,
                    count = uiState.libraryCount,
                    isSelected = selectedKey == "all",
                    onClick = onSelectFullLibraryBrowse
                )
            )
            add(
                VodClassicCategoryOption(
                    key = "favorites",
                    label = uiState.favoriteCategoryName,
                    count = uiState.categoryCounts[uiState.favoriteCategoryName] ?: 0,
                    isSelected = selectedKey == "favorites",
                    onClick = { onSelectCategory(uiState.favoriteCategoryName) }
                )
            )
            add(
                VodClassicCategoryOption(
                    key = "continue",
                    label = continueLabel,
                    count = continueCount,
                    isSelected = selectedKey == "continue",
                    onClick = onOpenContinueWatching
                )
            )
            add(
                VodClassicCategoryOption(
                    key = "recent",
                    label = recentLabel,
                    count = recentCount,
                    isSelected = selectedKey == "recent",
                    onClick = onOpenFresh
                )
            )
            visibleCategoryNames
                .filterNot { it == uiState.favoriteCategoryName }
                .forEach { name ->
                    val matchedCategory = categoryByName[name]
                    val lockedCategory = matchedCategory?.takeIf(isCategoryLocked)
                    val locked = lockedCategory != null
                    add(
                        VodClassicCategoryOption(
                            key = "category:$name",
                            label = name,
                            count = uiState.categoryCounts[name] ?: 0,
                            isSelected = selectedKey == "category:$name",
                            onClick = {
                                if (lockedCategory != null) openProtectedCategory(lockedCategory) else onSelectCategory(name)
                            },
                            onLongClick = matchedCategory?.takeIf { !locked }?.let { { onShowCategoryOptions(name) } },
                            isLocked = locked
                        )
                    )
                }
        }.filter { option ->
            categoryQuery.isBlank() || option.label.contains(categoryQuery.trim(), ignoreCase = true)
        }
    }

    VodClassicSplitLayout(
        railTitle = stringResource(R.string.nav_movies),
        railSearchValue = categoryQuery,
        onRailSearchValueChange = { categoryQuery = it },
        railSearchPlaceholder = stringResource(R.string.vod_classic_category_search),
        categories = railOptions
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val hasActiveFilterSortClassic = selectedFilterType != LibraryFilterType.ALL || selectedSortBy != LibrarySortBy.LIBRARY
            VodClassicContentHeader(
                title = when {
                    selectedKey == "all" -> allLabel
                    selectedKey == "continue" -> continueLabel
                    selectedKey == "recent" -> recentLabel
                    else -> uiState.selectedCategory ?: allLabel
                },
                subtitle = stringResource(
                    R.string.vod_classic_results_count,
                    filteredGridMovies.size
                ),
                actions = buildList {
                    add(
                        VodActionChip(
                            key = "search_toggle",
                            label = stringResource(
                                if (showSearchBar) R.string.library_action_hide_search else R.string.search_title
                            ),
                            onClick = { showSearchBar = !showSearchBar }
                        )
                    )
                    add(
                        VodActionChip(
                            key = "browse_options",
                            label = stringResource(R.string.library_action_filters_sort),
                            detail = vodActiveFilterSortDetail(selectedFilterType, selectedSortBy),
                            onClick = { showBrowseOptions = true }
                        )
                    )
                },
                selectedActionKey = if (hasActiveFilterSortClassic) "browse_options" else null
            )

            if (showSearchBar) {
                SearchInput(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.movies_search_placeholder),
                    onSearch = {},
                    focusRequester = initialFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val classicGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            InfiniteScrollEffect(
                gridState = classicGridState,
                enabled = !uiState.isReorderMode,
                canLoadMore = uiState.canLoadMoreSelectedCategory,
                isLoading = uiState.isLoadingSelectedCategory,
                onLoadMore = onLoadMore
            )
            LazyVerticalGrid(
                state = classicGridState,
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (uiState.isReorderMode && event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                draggingMovie = null
                                onDismissReorder()
                                true
                            } else false
                        } else false
                    },
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.isLoadingSelectedCategory) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                } else if (filteredGridMovies.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AppMessageState(
                            title = stringResource(R.string.movies_no_found),
                            subtitle = stringResource(R.string.vod_classic_empty_category)
                        )
                    }
                } else {
                    gridItems(filteredGridMovies, key = { it.id }) { movie ->
                        val isLocked = isMovieLocked(movie)
                        val isDraggingThis = draggingMovie == movie
                        MovieCard(
                            movie = movie,
                            isLocked = isLocked,
                            isReorderMode = uiState.isReorderMode,
                            isDragging = isDraggingThis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .then(if (!showSearchBar && movie.id == initialGridMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier),
                            onClick = {
                                if (uiState.isReorderMode) {
                                    draggingMovie = if (isDraggingThis) null else movie
                                } else if (isLocked) {
                                    onProtectedMovieClick(movie)
                                } else {
                                    onMovieClick(movie)
                                }
                            },
                            onLongClick = {
                                if (!uiState.isReorderMode) onShowDialog(movie)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun movieLibraryLensLabel(lens: MovieLibraryLens): String =
    when (lens) {
        MovieLibraryLens.FAVORITES -> stringResource(R.string.library_lens_favorites)
        MovieLibraryLens.CONTINUE -> stringResource(R.string.library_lens_continue)
        MovieLibraryLens.TOP_RATED -> stringResource(R.string.library_lens_top_rated)
        MovieLibraryLens.FRESH -> stringResource(R.string.library_lens_fresh_movies)
    }

private fun movieFilterChips(): List<SelectionChip> {
    return listOf(
        SelectionChip(LibraryFilterType.ALL.name, "All"),
        SelectionChip(LibraryFilterType.FAVORITES.name, "Favorites"),
        SelectionChip(LibraryFilterType.IN_PROGRESS.name, "Resume"),
        SelectionChip(LibraryFilterType.UNWATCHED.name, "Unwatched"),
        SelectionChip(LibraryFilterType.RECENTLY_UPDATED.name, "Recent"),
        SelectionChip(LibraryFilterType.TOP_RATED.name, "Top Rated")
    )
}

private fun movieSortChips(): List<SelectionChip> {
    return LibrarySortBy.entries.map { sort ->
        SelectionChip(
            key = sort.name,
            label = when (sort) {
                LibrarySortBy.LIBRARY -> "Library Order"
                LibrarySortBy.TITLE -> "A-Z"
                LibrarySortBy.RELEASE -> "Newest"
                LibrarySortBy.UPDATED -> "Recently Updated"
                LibrarySortBy.RATING -> "Rating"
                LibrarySortBy.WATCH_COUNT -> "Recent Activity"
            }
        )
    }
}

private fun vodMetadata(vararg values: String?): String? {
    return values
        .mapNotNull { it?.takeIf(String::isNotBlank) }
        .joinToString(" | ")
        .takeIf(String::isNotBlank)
}

private fun formatDurationSeconds(seconds: Int): String? {
    if (seconds <= 0) return null
    val totalMinutes = (seconds + 59) / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
