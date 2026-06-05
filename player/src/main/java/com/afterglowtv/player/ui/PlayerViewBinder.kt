package com.afterglowtv.player.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.afterglowtv.player.PlayerRenderSurfaceType
import com.afterglowtv.player.PlayerSurfaceResizeMode
import com.afterglowtv.player.R

class PlayerViewBinder(
    private val subtitleStyleController: SubtitleStyleController
) {
    private var boundPlayerView: PlayerView? = null
    private var boundResizeMode: PlayerSurfaceResizeMode = PlayerSurfaceResizeMode.FIT

    fun createRenderView(
        context: Context,
        resizeMode: PlayerSurfaceResizeMode,
        surfaceType: PlayerRenderSurfaceType
    ): View {
        return (LayoutInflater.from(context).inflate(layoutResId(surfaceType), null) as PlayerView).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setShutterBackgroundColor(Color.BLACK)
            applyResizeMode(resizeMode)
        }
    }

    fun bind(renderView: View, player: androidx.media3.exoplayer.ExoPlayer, resizeMode: PlayerSurfaceResizeMode) {
        val playerView = renderView as? PlayerView ?: return
        boundResizeMode = resizeMode
        if (boundPlayerView !== playerView) {
            boundPlayerView?.player = null
            playerView.player = player
            boundPlayerView = playerView
        } else if (playerView.player !== player) {
            playerView.player = player
        }
        playerView.applyResizeMode(resizeMode)
        subtitleStyleController.apply(playerView)
    }

    fun attachPlayer(player: androidx.media3.exoplayer.ExoPlayer?) {
        boundPlayerView?.player = player
        boundPlayerView?.applyResizeMode(boundResizeMode)
        subtitleStyleController.apply(boundPlayerView)
    }

    fun release(renderView: View) {
        val playerView = renderView as? PlayerView ?: return
        if (boundPlayerView !== playerView && playerView.player == null) return
        playerView.player = null
        if (boundPlayerView === playerView) {
            boundPlayerView = null
        }
    }

    fun clear() {
        boundPlayerView?.player = null
        boundPlayerView = null
    }

    fun reapplyStyle() {
        subtitleStyleController.apply(boundPlayerView)
    }

    private fun PlayerView.applyResizeMode(surfaceResizeMode: PlayerSurfaceResizeMode) {
        val targetResizeMode = when (surfaceResizeMode) {
            PlayerSurfaceResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            PlayerSurfaceResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            PlayerSurfaceResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
        if (resizeMode == targetResizeMode) return

        resizeMode = targetResizeMode
        findViewById<View>(androidx.media3.ui.R.id.exo_content_frame)?.requestLayout()
        videoSurfaceView?.requestLayout()
        requestLayout()
        invalidate()
    }

    private fun layoutResId(surfaceType: PlayerRenderSurfaceType): Int = when (surfaceType) {
        PlayerRenderSurfaceType.TEXTURE_VIEW -> R.layout.player_texture_view
        PlayerRenderSurfaceType.AUTO,
        PlayerRenderSurfaceType.SURFACE_VIEW -> R.layout.player_surface_view
    }
}
