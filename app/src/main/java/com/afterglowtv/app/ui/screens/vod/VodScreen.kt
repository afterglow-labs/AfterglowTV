package com.afterglowtv.app.ui.screens.vod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.PlayerRenderView
import com.afterglowtv.app.ui.components.SearchInput
import com.afterglowtv.app.ui.components.rememberCrossfadeImageModel
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.interaction.TvButton
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.model.VodTitleFormatter
import com.afterglowtv.app.ui.model.VodViewMode
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.player.PlayerRenderSurfaceType
import com.afterglowtv.player.PlayerSurfaceResizeMode

@Suppress("PropertyName")
internal object VodLayoutMetrics {
    val ShelfRowHeight = 58.dp
    val ShelfHeaderWidth = 88.dp
    val ShelfTileWidth = 188.dp
    val ShelfRowSpacing = 6.dp
    val ShelfItemSpacing = 6.dp
    val GridMinTileWidth = 210.dp
    val GridTileHeight = 58.dp
    val TilePadding = 5.dp
    val TileArtworkWidth = 48.dp
}

@Composable
fun VodScreen(
    onNavigate: (String) -> Unit,
    currentRoute: String,
    onMovieClick: (Movie) -> Unit,
    viewModel: VodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreenScaffold(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = stringResource(R.string.nav_vod_container),
        navigationChrome = AppNavigationChrome.TopBar,
        compactHeader = true,
        showScreenHeader = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AfterglowBrandStrip(
                wordmark = "VOD",
                tagline = uiState.provider?.name ?: "On-demand library",
            )
            VodBrowser(
                uiState = uiState,
                onSearchQueryChange = viewModel::setSearchQuery,
                onCategoryClick = viewModel::selectCategory,
                onLoadMore = viewModel::loadMore,
                onLoadAll = viewModel::loadAll,
                onMovieClick = { movie ->
                    if (uiState.previewMovie?.id == movie.id) {
                        viewModel.clearPreview()
                        onMovieClick(movie)
                    } else {
                        viewModel.previewMovie(movie)
                    }
                },
                onOpenMovie = { movie ->
                    viewModel.clearPreview()
                    onMovieClick(movie)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun VodBrowser(
    uiState: VodUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onLoadAll: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onOpenMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VodContentHeader(
                uiState = uiState,
                onSearchQueryChange = onSearchQueryChange,
                onLoadMore = onLoadMore,
                onLoadAll = onLoadAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            )
            when {
                uiState.isLoading -> VodEmptyMessage("Loading VOD...")
                uiState.provider == null -> VodEmptyMessage("No active VOD source")
                uiState.items.isEmpty() -> VodEmptyMessage("No VOD titles found")
                uiState.viewMode == VodViewMode.GUIDE -> VodGuideRailBrowser(
                    uiState = uiState,
                    onCategoryClick = onCategoryClick,
                    onMovieClick = onMovieClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                uiState.viewMode == VodViewMode.GRID -> VodItemGrid(
                    movies = uiState.items,
                    selectedMovieId = uiState.previewMovie?.id,
                    onMovieClick = onMovieClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                else -> VodShelfRows(
                    sections = uiState.sections,
                    selectedMovieId = uiState.previewMovie?.id,
                    onMovieClick = onMovieClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
        VodPreviewPane(
            movie = uiState.previewMovie,
            playerEngine = uiState.previewPlayerEngine,
            isLoading = uiState.isPreviewLoading,
            errorMessage = uiState.previewErrorMessage,
            onOpenMovie = onOpenMovie,
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun VodCategoryRail(
    categories: List<VodAlphaCategory>,
    selectedCategoryKey: String?,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Browse",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories, key = { it.key }) { category ->
                VodCategoryRow(
                    category = category,
                    selected = category.key == selectedCategoryKey,
                    onClick = { onCategoryClick(category.key) }
                )
            }
        }
    }
}

@Composable
private fun VodCategoryRow(
    category: VodAlphaCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AppColors.Brand.copy(alpha = 0.22f) else AppColors.SurfaceElevated,
            focusedContainerColor = AppColors.SurfaceEmphasis,
            contentColor = if (selected) AppColors.BrandStrong else AppColors.TextPrimary,
            focusedContentColor = AppColors.TextPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = FocusSpec.FocusedScale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun VodContentHeader(
    uiState: VodUiState,
    onSearchQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onLoadAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "VOD",
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.TextPrimary
            )
            Text(
                text = "${uiState.visibleItemCount} of ${uiState.totalItemCount} titles",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
        SearchInput(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Search VOD",
            modifier = Modifier.width(180.dp)
        )
        TvButton(
            onClick = onLoadMore,
            enabled = uiState.canLoadMore,
            modifier = Modifier.width(110.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Load more",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        TvButton(
            onClick = onLoadAll,
            enabled = uiState.canLoadMore,
            modifier = Modifier.width(96.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Load all",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun VodGuideRailBrowser(
    uiState: VodUiState,
    onCategoryClick: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        VodCategoryRail(
            categories = uiState.categories,
            selectedCategoryKey = uiState.selectedCategoryKey,
            onCategoryClick = onCategoryClick,
            modifier = Modifier
                .width(196.dp)
                .fillMaxHeight()
        )
        VodItemGrid(
            movies = uiState.selectedItems,
            selectedMovieId = uiState.previewMovie?.id,
            onMovieClick = onMovieClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun VodShelfRows(
    sections: List<VodBrowseSection>,
    selectedMovieId: Long?,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(VodLayoutMetrics.ShelfRowSpacing)
    ) {
        items(sections, key = { it.key }) { section ->
            VodShelfSectionRow(
                section = section,
                selectedMovieId = selectedMovieId,
                onMovieClick = onMovieClick
            )
        }
    }
}

@Composable
private fun VodShelfSectionRow(
    section: VodBrowseSection,
    selectedMovieId: Long?,
    onMovieClick: (Movie) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VodLayoutMetrics.ShelfRowHeight),
        horizontalArrangement = Arrangement.spacedBy(VodLayoutMetrics.ShelfItemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(VodLayoutMetrics.ShelfHeaderWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.SurfaceElevated)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AppColors.BrandStrong)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${section.count} titles",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(VodLayoutMetrics.ShelfItemSpacing),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(section.items, key = { it.id }) { movie ->
                VodTextTile(
                    movie = movie,
                    selected = movie.id == selectedMovieId,
                    onClick = { onMovieClick(movie) },
                    modifier = Modifier
                        .width(VodLayoutMetrics.ShelfTileWidth)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun VodItemGrid(
    movies: List<Movie>,
    selectedMovieId: Long?,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = VodLayoutMetrics.GridMinTileWidth),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            VodTextTile(
                movie = movie,
                selected = movie.id == selectedMovieId,
                onClick = { onMovieClick(movie) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VodLayoutMetrics.GridTileHeight)
            )
        }
    }
}

@Composable
private fun VodTextTile(
    movie: Movie,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = VodTitleFormatter.format(movie.name, movie.year)
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AppColors.Brand.copy(alpha = 0.2f) else AppColors.SurfaceElevated,
            focusedContainerColor = AppColors.SurfaceEmphasis,
            contentColor = AppColors.TextPrimary,
            focusedContentColor = AppColors.TextPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = FocusSpec.FocusedScale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(VodLayoutMetrics.TilePadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VodSmallArtwork(movie)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayTitle.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        displayTitle.year ?: movie.year,
                        movie.duration?.takeIf { it.isNotBlank() },
                        movie.genre?.takeIf { it.isNotBlank() }
                    ).joinToString("  |  ").ifBlank { "VOD" },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                movie.categoryName?.takeIf { it.isNotBlank() && it != "Movie VOD" && it != "TV VOD" }?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.BrandStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun VodSmallArtwork(movie: Movie) {
    val imageUrl = movie.posterUrl ?: movie.backdropUrl
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(AppColors.BrandStrong.copy(alpha = 0.76f))
        )
        return
    }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(VodLayoutMetrics.TileArtworkWidth)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceEmphasis),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AppColors.BrandStrong.copy(alpha = 0.72f))
        )
        AsyncImage(
            model = rememberCrossfadeImageModel(imageUrl),
            contentDescription = movie.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun VodPreviewPane(
    movie: Movie?,
    playerEngine: com.afterglowtv.player.PlayerEngine?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val renderSurfaceType by (playerEngine?.renderSurfaceType)?.collectAsStateWithLifecycle(
        initialValue = PlayerRenderSurfaceType.SURFACE_VIEW
    ) ?: remember { mutableStateOf(PlayerRenderSurfaceType.SURFACE_VIEW) }
    val displayTitle = movie?.let { VodTitleFormatter.format(it.name, it.year) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
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
                    modifier = Modifier.padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Press OK to load a preview.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = errorMessage ?: "Press OK again for full screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            if (isLoading && movie != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.66f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = AppColors.BrandStrong,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Loading preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextPrimary
                    )
                }
            }
        }
        if (movie != null && displayTitle != null) {
            Text(
                text = displayTitle.title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    displayTitle.year ?: movie.year,
                    movie.duration?.takeIf { it.isNotBlank() },
                    movie.genre?.takeIf { it.isNotBlank() }
                ).joinToString("  |  ").ifBlank { "VOD" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            TvButton(onClick = { onOpenMovie(movie) }) {
                Text("Full screen")
            }
        }
    }
}

@Composable
private fun VodEmptyMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextSecondary
        )
    }
}
