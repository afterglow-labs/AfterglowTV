package com.afterglowtv.app.ui.screens.player.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.LocalAppSpacing
import com.afterglowtv.app.ui.design.AfterglowFocusRole
import com.afterglowtv.app.ui.design.afterglowFocus

/**
 * AfterglowTV channel row: # | logo | name (+ now title) (+ progress).
 *
 * Compact TV dimensions:
 *  - row height: 56 dp
 *  - logo cell: 40 dp square, 6 dp radius
 *  - channel-number column: 42 dp
 *  - now-progress: 2 dp linear bar
 */
data class LivePanelChannelRowState(
    val channelNumber: String,
    val name: String,
    val logoUrl: String?,
    val nowTitle: String?,
    val nowProgress: Float?, // 0f..1f, null hides the bar
    val isCurrent: Boolean,
    val isFavourite: Boolean,
)

@Composable
fun LivePanelChannelRow(
    state: LivePanelChannelRowState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.livePanelRowHeight)
            .afterglowFocus(role = AfterglowFocusRole.Row)
            .clickable(onClick = onClick)
    ) {
        val compact = maxWidth < 280.dp
        val hideLogo = maxWidth < 220.dp
        val horizontalPadding = if (compact) 8.dp else 12.dp
        val numberWidth = if (compact) 34.dp else 42.dp
        val logoSize = if (compact) 32.dp else 40.dp
        val textGap = if (compact) 8.dp else 12.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.channelNumber,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.TextSecondary,
                modifier = Modifier.width(numberWidth),
            )
            if (!hideLogo) {
                ChannelLogoBadge(
                    channelName = state.name,
                    logoUrl = state.logoUrl,
                    modifier = Modifier.size(logoSize),
                    shape = RoundedCornerShape(6.dp),
                )
                Spacer(Modifier.width(textGap))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state.isCurrent) AppColors.TiviAccentLight else AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.nowTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                state.nowProgress?.let { p ->
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        LinearProgressIndicator(
                            progress = { p.coerceIn(0f, 1f) },
                            color = AppColors.TiviAccent,
                            trackColor = AppColors.TiviSurfaceAccent,
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                        )
                    }
                }
            }
        }
    }
}
