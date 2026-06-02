package com.afterglowtv.app.ui.screens.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.PlayerRenderView
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.LocalAppSpacing
import com.afterglowtv.app.ui.theme.OnBackground
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.player.PlayerEngine
import com.afterglowtv.player.PlayerRenderSurfaceType
import com.afterglowtv.player.PlayerSurfaceResizeMode

/**
 * Compact EPG preview tile. This intentionally renders the same auxiliary
 * PlayerEngine used by Live TV so a second OK can hand it to fullscreen
 * playback without reloading the stream.
 */
@Composable
fun EpgPipPreview(
    channel: Channel?,
    playerEngine: PlayerEngine?,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    val renderSurfaceType by (playerEngine?.renderSurfaceType)?.collectAsStateWithLifecycle(
        initialValue = PlayerRenderSurfaceType.SURFACE_VIEW
    ) ?: remember { mutableStateOf(PlayerRenderSurfaceType.SURFACE_VIEW) }

    Box(
        modifier = modifier
            .size(spacing.epgPipWidth, spacing.epgPipHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black)
            .border(2.dp, AppColors.PipPreviewOutline, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (channel != null && playerEngine != null && errorMessage == null) {
            PlayerRenderView(
                playerEngine = playerEngine,
                resizeMode = PlayerSurfaceResizeMode.FIT,
                surfaceType = renderSurfaceType,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.live_preview_placeholder_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = errorMessage ?: stringResource(R.string.live_preview_placeholder_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceDim,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        if (isLoading && channel != null) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.live_preview_loading),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}
