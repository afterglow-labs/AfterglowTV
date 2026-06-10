package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated

@Composable
internal fun SyncingOverlay(
    isSyncing: Boolean,
    providerName: String? = null,
    progress: String? = null
) {
    if (!isSyncing) return

    var minimized by remember { mutableStateOf(false) }
    LaunchedEffect(isSyncing) {
        if (isSyncing) minimized = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (minimized) {
            SyncStatusPill(onClick = { minimized = false })
        } else {
            SyncStatusPanel(
                providerName = providerName,
                progress = progress,
                onMinimize = { minimized = true }
            )
        }
    }
}

@Composable
private fun SyncStatusPill(onClick: () -> Unit) {
    TvClickableSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated.copy(alpha = 0.92f),
            focusedContainerColor = Primary.copy(alpha = 0.28f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Text(
            text = "Syncing in background...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface
        )
    }
}

@Composable
private fun SyncStatusPanel(
    providerName: String?,
    progress: String?,
    onMinimize: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(SurfaceElevated.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(color = Primary)
        Text(
            text = stringResource(R.string.settings_syncing_title),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface
        )
        Text(
            text = providerName ?: stringResource(R.string.settings_syncing_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDim
        )
        progress?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
        }
        TvClickableSurface(
            onClick = onMinimize,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Primary.copy(alpha = 0.20f),
                focusedContainerColor = Primary.copy(alpha = 0.34f)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Text(
                text = "Background",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface
            )
        }
    }
}
