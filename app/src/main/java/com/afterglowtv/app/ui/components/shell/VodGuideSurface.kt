package com.afterglowtv.app.ui.components.shell

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.rememberCrossfadeImageModel
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.interaction.TvClickableSurface

data class VodGuideProgramCard(
    val key: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val badge: String?,
    val isLocked: Boolean = false,
    val textFirst: Boolean = false,
    val topLabel: String? = null,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)

@Composable
fun VodGuideLane(
    title: String,
    subtitle: String,
    programs: List<VodGuideProgramCard>,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester? = null,
    rowHeight: Dp = 118.dp,
    programWidth: Dp = 300.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VodGuideLaneHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier
                .width(188.dp)
                .fillMaxHeight()
        )

        LazyRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            itemsIndexed(programs, key = { _, program -> program.key }) { index, program ->
                VodGuideProgramSurface(
                    program = program,
                    modifier = Modifier
                        .width(programWidth)
                        .fillMaxHeight()
                        .then(
                            if (index == 0 && initialFocusRequester != null) {
                                Modifier.focusRequester(initialFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun VodGuideLaneHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(3.dp)
                .background(AppColors.BrandStrong, RoundedCornerShape(999.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VodGuideProgramSurface(
    program: VodGuideProgramCard,
    modifier: Modifier = Modifier
) {
    val lockedLabel = stringResource(R.string.home_locked_short)
    TvClickableSurface(
        onClick = program.onClick,
        onLongClick = program.onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.SurfaceElevated,
            focusedContainerColor = AppColors.SurfaceEmphasis,
            contentColor = AppColors.TextPrimary,
            focusedContentColor = AppColors.TextPrimary
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(0.dp, AppColors.Outline.copy(alpha = 0f)),
                shape = RoundedCornerShape(12.dp)
            ),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasArtwork = !program.imageUrl.isNullOrBlank()
            if (!program.textFirst || hasArtwork) {
                VodGuideThumbnail(
                    imageUrl = program.imageUrl,
                    title = program.title,
                    showFallbackInitial = !program.textFirst,
                    modifier = if (program.textFirst) {
                        Modifier
                            .fillMaxHeight()
                            .width(76.dp)
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(16f / 9f)
                    }
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                program.topLabel?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                program.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    program.badge?.takeIf { it.isNotBlank() }?.let {
                        VodGuideBadge(label = it)
                    }
                    if (program.isLocked) {
                        VodGuideBadge(label = lockedLabel, emphasis = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun VodGuideThumbnail(
    imageUrl: String?,
    title: String,
    showFallbackInitial: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.SurfaceAccent,
                        AppColors.TiviSurfaceDeep
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showFallbackInitial) {
            Text(
                text = title.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.TextSecondary
            )
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCrossfadeImageModel(imageUrl),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun VodGuideBadge(
    label: String,
    emphasis: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (emphasis) AppColors.Brand.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasis) AppColors.BrandStrong else AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
