package com.afterglowtv.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.domain.model.EpgSource

@Composable
internal fun EpgSourceCard(
    source: EpgSource,
    isRefreshing: Boolean,
    pendingDelete: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSetPendingDelete: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(source.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text(displayableEpgUrl(source.url), style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim, maxLines = 1)
                    if (source.lastError != null) {
                        Text("Error: ${source.lastError}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF5350))
                    }
                    if (source.lastSuccessAt > 0L) {
                        val ago = (System.currentTimeMillis() - source.lastSuccessAt) / 60000
                        Text("Last synced: ${ago}m ago", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                    }
                }
                val sourceActionShape = RoundedCornerShape(8.dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.focusGroup()
                ) {
                    TvClickableSurface(
                        onClick = { onToggleEnabled(!source.enabled) },
                        shape = ClickableSurfaceDefaults.shape(sourceActionShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (source.enabled) Color(0xFF66BB6A).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = if (source.enabled) Color(0xFF66BB6A).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                        ),
                        border = epgActionBorder(sourceActionShape),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                    ) {
                        Text(
                            if (source.enabled) "ON" else "OFF",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (source.enabled) Color(0xFF66BB6A) else OnSurfaceDim
                        )
                    }
                    TvClickableSurface(
                        onClick = onRefresh,
                        shape = ClickableSurfaceDefaults.shape(sourceActionShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Primary.copy(alpha = 0.15f),
                            focusedContainerColor = Primary.copy(alpha = 0.3f)
                        ),
                        border = epgActionBorder(sourceActionShape, enabled = !isRefreshing),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                        enabled = !isRefreshing
                    ) {
                        Text(
                            if (isRefreshing) "..." else "Refresh",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                    if (pendingDelete) {
                        TvClickableSurface(
                            onClick = { onSetPendingDelete(false) },
                            shape = ClickableSurfaceDefaults.shape(sourceActionShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                focusedContainerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            border = epgActionBorder(sourceActionShape),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                        ) {
                            Text("Cancel", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                        }
                        TvClickableSurface(
                            onClick = onDelete,
                            shape = ClickableSurfaceDefaults.shape(sourceActionShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color(0xFFEF5350).copy(alpha = 0.25f),
                                focusedContainerColor = Color(0xFFEF5350).copy(alpha = 0.45f)
                            ),
                            border = epgActionBorder(sourceActionShape),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                        ) {
                            Text("Confirm Delete", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF5350))
                        }
                    } else {
                        TvClickableSurface(
                            onClick = { onSetPendingDelete(true) },
                            shape = ClickableSurfaceDefaults.shape(sourceActionShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color(0xFFEF5350).copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFFEF5350).copy(alpha = 0.25f)
                            ),
                            border = epgActionBorder(sourceActionShape),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                        ) {
                            Text("Delete", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF5350))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddEpgSourceCard(viewModel: SettingsViewModel) {
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var pickerError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { result ->
        result.data?.data?.let { selectedUri ->
            persistReadPermissionIfAvailable(context, selectedUri)
            runCatching {
                copyEpgUriToInternalFile(context, selectedUri)
            }.onSuccess { fileUrl ->
                pickerError = null
                newUrl = fileUrl
            }.onFailure { error ->
                pickerError = error.message ?: "Could not copy selected EPG file."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add EPG Source", style = MaterialTheme.typography.titleSmall, color = Color.White)
            EpgSourceTextField(value = newName, onValueChange = { newName = it }, placeholder = "Source name")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    EpgSourceTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        placeholder = "XMLTV URL (HTTP/HTTPS) or browse file"
                    )
                }
                val addActionShape = RoundedCornerShape(8.dp)
                TvClickableSurface(
                    onClick = {
                        val mimeTypes = arrayOf("text/xml", "application/xml", "application/octet-stream", "*/*")
                        launchDocumentPickerSafely(
                            unavailableMessage = "Could not open the file picker. Paste an EPG URL or a file:// path instead.",
                            onError = { pickerError = it },
                            launchPrimary = { filePickerLauncher.launch(openDocumentIntent(mimeTypes)) },
                            launchFallback = { filePickerLauncher.launch(getContentIntent(mimeTypes)) }
                        )
                    },
                    shape = ClickableSurfaceDefaults.shape(addActionShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Primary.copy(alpha = 0.15f),
                        focusedContainerColor = Primary.copy(alpha = 0.3f)
                    ),
                    border = epgActionBorder(addActionShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                ) {
                    Text("Browse", modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.labelMedium, color = Primary)
                }
            }
            pickerError?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF5350))
            }
            val addSourceShape = RoundedCornerShape(8.dp)
            TvClickableSurface(
                onClick = {
                    if (!isSubmitting && newName.isNotBlank() && newUrl.isNotBlank()) {
                        val nameToSubmit = newName.trim()
                        val urlToSubmit = newUrl.trim()
                        isSubmitting = true
                        viewModel.addEpgSource(nameToSubmit, urlToSubmit,
                            onSuccess = {
                                newName = ""
                                newUrl = ""
                                isSubmitting = false
                            },
                            onError = { isSubmitting = false }
                        )
                    }
                },
                enabled = newName.isNotBlank() && newUrl.isNotBlank() && !isSubmitting,
                shape = ClickableSurfaceDefaults.shape(addSourceShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFF66BB6A).copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF66BB6A).copy(alpha = 0.4f)
                ),
                border = epgActionBorder(addSourceShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
            ) {
                Text("Add Source", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color(0xFF66BB6A), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun epgActionBorder(shape: RoundedCornerShape, enabled: Boolean = true) =
    ClickableSurfaceDefaults.border(
        border = Border(
            border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.08f else 0.04f)),
            shape = shape
        ),
        focusedBorder = Border(
            border = BorderStroke(FocusSpec.BorderWidth, Color.White),
            shape = shape
        )
    )

private fun displayableEpgUrl(url: String): String = when {
    url.startsWith("content://") -> {
        val lastSegment = try { android.net.Uri.parse(url).lastPathSegment } catch (_: Exception) { null }
        val decoded = lastSegment?.let { android.net.Uri.decode(it) }?.substringAfterLast("/")?.substringAfterLast("\\")
        if (!decoded.isNullOrBlank() && decoded.length < 60) "local: $decoded" else "local file"
    }
    else -> url
}
