package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afterglowtv.domain.model.LocalMediaLibrarySourceType

internal fun LazyListScope.settingsLocalMediaSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onChooseLibrary: () -> Unit
) {
    item {
        SettingsSectionHeader(
            title = "Local Media",
            subtitle = "Folders and network shares are indexed for local VOD and pseudo-live guide playback."
        )
    }
    item {
        var showNetworkShareDialog by rememberSaveable { mutableStateOf(false) }
        if (showNetworkShareDialog) {
            NetworkShareDialog(
                isScanning = uiState.isScanningLocalMedia,
                onDismiss = { showNetworkShareDialog = false },
                onAddShare = viewModel::addSmbLocalMediaLibrary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ClickableSettingsRow(
                label = "Add media folder",
                value = if (uiState.isScanningLocalMedia) "Scanning..." else "Choose folder",
                onClick = onChooseLibrary,
                enabled = !uiState.isScanningLocalMedia
            )
            ClickableSettingsRow(
                label = "Add network share",
                value = if (uiState.isScanningLocalMedia) "Scanning..." else "QNAP / SMB",
                onClick = { showNetworkShareDialog = true },
                enabled = !uiState.isScanningLocalMedia
            )
        }
    }
    if (uiState.localMediaLibraries.isEmpty()) {
        item {
            SettingsRow(
                label = "Libraries",
                value = "No folders or network shares added"
            )
        }
    } else {
        uiState.localMediaLibraries.forEach { library ->
            item(key = "local_media_${library.id}") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsRow(
                        label = library.displayName ?: library.name,
                        value = "${library.itemCount} files - ${library.sourceType.label()}"
                    )
                    ClickableSettingsRow(
                        label = "Rescan",
                        value = "Refresh index",
                        onClick = { viewModel.rescanLocalMediaLibrary(library.id) },
                        enabled = !uiState.isScanningLocalMedia,
                        indent = 16.dp
                    )
                    ClickableSettingsRow(
                        label = "Remove",
                        value = "Delete library",
                        onClick = { viewModel.deleteLocalMediaLibrary(library.id) },
                        enabled = !uiState.isScanningLocalMedia,
                        indent = 16.dp
                    )
                }
            }
        }
    }
}

private fun LocalMediaLibrarySourceType.label(): String = when (this) {
    LocalMediaLibrarySourceType.DOCUMENT_TREE -> "Folder"
    LocalMediaLibrarySourceType.SMB -> "Network"
}
