package com.afterglowtv.app.ui.screens.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.ui.components.shell.StatusPill
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppMotion
import com.afterglowtv.app.ui.design.LocalAppSpacing
import kotlinx.coroutines.delay

/**
 * AfterglowTV bottom info OSD. Renders the bottom ~180 dp of the screen
 * (matches AfterglowTV dimens) with a vertical gradient that fades into
 * a 75% black scrim against the deepest AfterglowTV surface.
 *
 * Auto-dismisses after 3 s (AfterglowTV's `0x7f0c0027` integer = 3000 ms).
 *
 * Content: channel logo + number + name + status pills (REC, CATCH-UP),
 * NOW program line + description, progress bar, NEXT line.
 */
data class InfoOsdState(
    val channelNumber: String,
    val channelName: String,
    val channelLogoUrl: String?,
    val nowTitle: String?,
    val nowDescription: String?,
    val nowProgress: Float?,
    val nowTimeRange: String?,
    val nextTitle: String?,
    val nextStartTime: String?,
    val isRecording: Boolean,
    val isCatchupAvailable: Boolean,
)

@Composable
fun InfoOsdBottom(
    state: InfoOsdState?,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMs: Long = AppMotion.OsdAutoHideMs,
) {
    if (visible && state != null) {
        LaunchedEffect(state) {
            delay(autoDismissMs)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = visible && state != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(140)),
        modifier = modifier.fillMaxWidth(),
    ) {
        val spacing = LocalAppSpacing.current
        val s = state ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.infoOsdHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.TiviSurfaceDeep.copy(alpha = 0f),
                            AppColors.TiviSurfaceDeep.copy(alpha = 0.94f),
                        ),
                    )
                )
                .padding(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelLogoBadge(
                        channelName = s.channelName,
                        logoUrl = s.channelLogoUrl,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(8.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.channelNumber,
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextTertiary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = s.channelName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AppColors.TextPrimary,
                            )
                            Spacer(Modifier.width(12.dp))
                            if (s.isRecording) {
                                StatusPill(
                                    label = "REC",
                                    containerColor = AppColors.EpgNowLine,
                                    contentColor = AppColors.TextPrimary,
                                )
                            }
                            if (s.isCatchupAvailable) {
                                Spacer(Modifier.width(6.dp))
                                StatusPill(
                                    label = "CATCH-UP",
                                    containerColor = AppColors.Info,
                                    contentColor = AppColors.TextPrimary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (s.nowTitle != null) {
                    Text(
                        text = buildString {
                            s.nowTimeRange?.let { append(it); append("   ") }
                            append(s.nowTitle)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                s.nowDescription?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                s.nowProgress?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { p.coerceIn(0f, 1f) },
                        color = AppColors.TiviAccent,
                        trackColor = AppColors.TiviSurfaceAccent,
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                    )
                }
                if (s.nextTitle != null) {
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Text(
                            text = "NEXT",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextTertiary,
                            modifier = Modifier.width(58.dp),
                        )
                        Text(
                            text = buildString {
                                s.nextStartTime?.let { append(it); append("   ") }
                                append(s.nextTitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
