package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun LazyListScope.settingsLocalMediaSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onChooseLibrary: () -> Unit
) {
    item {
        SettingsSectionHeader(
            title = "Local Media",
            subtitle = "Folders you add here are indexed for local VOD and pseudo-live guide playback."
        )
    }
    item {
        ClickableSettingsRow(
            label = "Add media folder",
            value = if (uiState.isScanningLocalMedia) "Scanning..." else "Choose folder",
            onClick = onChooseLibrary,
            enabled = !uiState.isScanningLocalMedia
        )
    }
    if (uiState.localMediaLibraries.isEmpty()) {
        item {
            SettingsRow(
                label = "Libraries",
                value = "No folders added"
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
                        value = "${library.itemCount} files"
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
