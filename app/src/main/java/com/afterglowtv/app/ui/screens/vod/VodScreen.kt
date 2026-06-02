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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.afterglowtv.domain.model.Movie

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
                onMovieClick = onMovieClick,
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        VodCategoryRail(
            categories = uiState.categories,
            selectedCategoryKey = uiState.selectedCategoryKey,
            onCategoryClick = onCategoryClick,
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VodContentHeader(
                uiState = uiState,
                onSearchQueryChange = onSearchQueryChange,
                onLoadMore = onLoadMore,
                onLoadAll = onLoadAll
            )
            when {
                uiState.isLoading -> VodEmptyMessage("Loading VOD...")
                uiState.provider == null -> VodEmptyMessage("No active VOD source")
                uiState.items.isEmpty() -> VodEmptyMessage("No VOD titles found")
                else -> VodItemGrid(
                    movies = uiState.items,
                    onMovieClick = onMovieClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VodCategoryRail(
    categories: List<VodAlphaCategory>,
    selectedCategoryKey: String,
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
    onLoadAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.width(320.dp)
        )
        TvButton(
            onClick = onLoadMore,
            enabled = uiState.canLoadMore
        ) {
            Text("Load more")
        }
        TvButton(
            onClick = onLoadAll,
            enabled = uiState.canLoadMore
        ) {
            Text("Load all")
        }
    }
}

@Composable
private fun VodItemGrid(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 360.dp),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            VodTextTile(
                movie = movie,
                onClick = { onMovieClick(movie) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            )
        }
    }
}

@Composable
private fun VodTextTile(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = VodTitleFormatter.format(movie.name, movie.year)
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.SurfaceElevated,
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
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VodSmallArtwork(movie)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = displayTitle.title,
                    style = MaterialTheme.typography.titleSmall,
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
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(16f / 9f)
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
        val imageUrl = movie.posterUrl ?: movie.backdropUrl
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCrossfadeImageModel(imageUrl),
                contentDescription = movie.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
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
