package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.di.MainPlayerEngine
import com.afterglowtv.app.player.PlayerNowPlayingState
import com.afterglowtv.app.player.PlayerNowPlayingStore
import com.afterglowtv.app.ui.components.PlayerRenderView
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnBackground
import com.afterglowtv.app.ui.theme.OnPrimary
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.PrimaryLight
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.SurfaceHighlight
import com.afterglowtv.player.PlaybackState
import com.afterglowtv.player.PlayerEngine
import com.afterglowtv.player.PlayerRenderSurfaceType
import com.afterglowtv.player.PlayerSurfaceResizeMode
import com.afterglowtv.domain.model.VideoFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Composable
internal fun SettingsNowPlayingSidecar(
    onReturnToPlayer: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsNowPlayingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (!uiState.visible) return

    Surface(
        modifier = modifier
            .width(370.dp)
            .padding(start = 18.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(14.dp),
        border = Border(
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.28f)),
            shape = RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_now_playing_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryLight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isPlaying) PrimaryLight else OnSurfaceDim
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, Primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            ) {
                PlayerRenderView(
                    playerEngine = viewModel.playerEngine,
                    resizeMode = PlayerSurfaceResizeMode.FIT,
                    surfaceType = PlayerRenderSurfaceType.TEXTURE_VIEW,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.64f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_now_playing_live),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.playbackState == PlaybackState.BUFFERING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryLight,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                NowPlayingActionButton(
                    label = stringResource(R.string.settings_now_playing_return),
                    primary = true,
                    onClick = onReturnToPlayer
                )
                NowPlayingActionButton(
                    label = stringResource(R.string.settings_now_playing_pip),
                    primary = false,
                    onClick = onEnterPictureInPicture
                )
            }
        }
    }
}

@Composable
private fun NowPlayingActionButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val container = if (primary) {
        Brush.horizontalGradient(listOf(Primary, PrimaryLight))
    } else {
        Brush.horizontalGradient(listOf(SurfaceHighlight, SurfaceHighlight.copy(alpha = 0.72f)))
    }
    val contentColor = if (primary) OnPrimary else OnBackground
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(container, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal data class SettingsNowPlayingUiState(
    val visible: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val statusLabel: String = "",
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
internal class SettingsNowPlayingViewModel @Inject constructor(
    @param:MainPlayerEngine
    val playerEngine: PlayerEngine,
    nowPlayingStore: PlayerNowPlayingStore
) : ViewModel() {
    val uiState: StateFlow<SettingsNowPlayingUiState> = combine(
        nowPlayingStore.state,
        playerEngine.playbackState,
        playerEngine.isPlaying,
        playerEngine.videoFormat
    ) { nowPlaying, playbackState, isPlaying, videoFormat ->
        nowPlaying.toUiState(playbackState, isPlaying, videoFormat)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsNowPlayingUiState()
    )

    private fun PlayerNowPlayingState.toUiState(
        playbackState: PlaybackState,
        isPlaying: Boolean,
        videoFormat: VideoFormat
    ): SettingsNowPlayingUiState {
        val visible = active &&
            isLive &&
            title.isNotBlank() &&
            playbackState != PlaybackState.IDLE &&
            playbackState != PlaybackState.ERROR &&
            playbackState != PlaybackState.ENDED
        val channelPrefix = channelNumber?.let { "CH $it" }
        val status = when {
            playbackState == PlaybackState.BUFFERING -> "BUFFERING"
            isPlaying -> "PLAYING"
            else -> "PAUSED"
        }
        val formatLabel = if (videoFormat.width > 0 && videoFormat.height > 0) {
            "${videoFormat.width}x${videoFormat.height}"
        } else {
            null
        }
        return SettingsNowPlayingUiState(
            visible = visible,
            title = listOfNotNull(channelPrefix, title).joinToString("  "),
            subtitle = listOfNotNull(subtitle.takeIf { it.isNotBlank() }, formatLabel).joinToString("  "),
            statusLabel = status,
            isPlaying = isPlaying,
            playbackState = playbackState
        )
    }
}
