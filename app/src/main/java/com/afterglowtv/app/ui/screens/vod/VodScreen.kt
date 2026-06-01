package com.afterglowtv.app.ui.screens.vod

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.VodActionChip
import com.afterglowtv.app.ui.components.shell.VodActionChipRow
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.PlaybackHistory

enum class VodContainerMode {
    MOVIES,
    TV
}

@Composable
fun VodScreen(
    onMovieClick: (Movie) -> Unit,
    onContinueWatchingPlay: (PlaybackHistory) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String = Routes.VOD_CONTAINER
) {
    var selectedMode by rememberSaveable { mutableStateOf(VodContainerMode.MOVIES) }

    when (selectedMode) {
        VodContainerMode.MOVIES -> VodMoviesScreen(
            onMovieClick = onMovieClick,
            onContinueWatchingPlay = onContinueWatchingPlay,
            onNavigate = onNavigate,
            currentRoute = currentRoute,
            initialContainerMode = true,
            wordmark = "VOD",
            tagline = "Movies from your on-demand libraries.",
            containerMode = selectedMode,
            onContainerModeChange = { selectedMode = it }
        )

        VodContainerMode.TV -> VodMoviesScreen(
            onMovieClick = onMovieClick,
            onContinueWatchingPlay = onContinueWatchingPlay,
            onNavigate = onNavigate,
            currentRoute = currentRoute,
            initialContainerMode = true,
            wordmark = "VOD",
            tagline = "TV shows from your on-demand libraries.",
            containerMode = selectedMode,
            onContainerModeChange = { selectedMode = it }
        )
    }
}

@Composable
fun VodContainerHeader(
    wordmark: String,
    tagline: String,
    selectedMode: VodContainerMode?,
    onModeSelected: (VodContainerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AfterglowBrandStrip(
            wordmark = wordmark,
            tagline = tagline
        )
        if (selectedMode != null) {
            VodActionChipRow(
                actions = listOf(
                    VodActionChip(
                        key = VodContainerMode.MOVIES.name,
                        label = "Movies",
                        detail = "Films and standalone videos",
                        onClick = { onModeSelected(VodContainerMode.MOVIES) }
                    ),
                    VodActionChip(
                        key = VodContainerMode.TV.name,
                        label = "TV",
                        detail = "Shows and episodes",
                        onClick = { onModeSelected(VodContainerMode.TV) }
                    )
                ),
                selectedKey = selectedMode.name,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
