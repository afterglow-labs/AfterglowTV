package com.afterglowtv.app.ui.screens.epg

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.LocalAppSpacing

/**
 * Tiny corner PiP preview pinned to the top-right of the EPG grid. Silently
 * plays the focused channel's stream so the user can preview without
 * leaving the guide. Afterglow TV dimensions: ~260 x 146 dp (16:9), 6 dp
 * corner radius, 2 dp accent outline.
 *
 * The ExoPlayer is recreated on URL change to avoid surface juggling on
 * cheaper Fire TV sticks.
 */
@OptIn(UnstableApi::class)
@Composable
fun EpgPipPreview(
    streamUrl: String?,
    modifier: Modifier = Modifier,
    previewWidth: Dp? = null,
    previewHeight: Dp? = null
) {
    if (streamUrl.isNullOrBlank()) return
    val spacing = LocalAppSpacing.current
    val width = previewWidth ?: spacing.epgPipWidth
    val height = previewHeight ?: spacing.epgPipHeight
    val ctx = LocalContext.current
    val player = remember(streamUrl) {
        ExoPlayer.Builder(ctx).build().also {
            it.setMediaItem(MediaItem.fromUri(streamUrl))
            it.volume = 0f
            it.prepare()
            it.playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width, height)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, AppColors.PipPreviewOutline, RoundedCornerShape(6.dp)),
        ) {
            AndroidView(
                factory = { c -> PlayerView(c).apply { useController = false } },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
