package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.device.rememberIsTelevisionDevice
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogFooterButton
import com.afterglowtv.app.ui.design.AppColors

@Composable
internal fun SettingsToolDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.76f,
    heightFraction: Float = 0.88f,
    content: @Composable BoxScope.() -> Unit,
) {
    val isTelevisionDevice = rememberIsTelevisionDevice()
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val resolvedWidthFraction = if (isTelevisionDevice) {
            widthFraction
        } else {
            when {
                maxWidth < 700.dp -> 0.9f
                maxWidth < 1000.dp -> maxOf(widthFraction, 0.62f)
                else -> widthFraction
            }
        }
        Surface(
            modifier = modifier
                .fillMaxWidth(resolvedWidthFraction)
                .fillMaxHeight(heightFraction),
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(containerColor = AppColors.SurfaceElevated),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AppColors.TiviSurfaceDeep.copy(alpha = 0.78f),
                                AppColors.BrandMuted.copy(alpha = 0.18f),
                                AppColors.SurfaceElevated,
                                AppColors.TiviSurfaceCool.copy(alpha = 0.22f),
                                AppColors.Surface,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1800f, 1100f),
                        ),
                    )
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    content = content,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    PremiumDialogFooterButton(
                        label = androidx.compose.ui.res.stringResource(R.string.player_close),
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}
