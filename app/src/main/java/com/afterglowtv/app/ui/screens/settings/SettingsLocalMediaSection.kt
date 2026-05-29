package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.components.dialogs.PremiumDialog
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogActionButton
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogFooterButton

data class LocalMediaNetworkShareInput(
    val sharePath: String,
    val username: String?,
    val password: String?,
    val domain: String?,
    val guest: Boolean
)

internal fun LazyListScope.settingsLocalMediaSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onChooseLibrary: () -> Unit,
    onAddNetworkShare: () -> Unit
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
    item {
        ClickableSettingsRow(
            label = "Add network share",
            value = if (uiState.isScanningLocalMedia) "Scanning..." else "Enter path",
            onClick = onAddNetworkShare,
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

@Composable
internal fun AddLocalMediaNetworkShareDialog(
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onAdd: (LocalMediaNetworkShareInput) -> Unit
) {
    var sharePath by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf(false) }

    PremiumDialog(
        title = "Add network share",
        subtitle = "Use an UNC or mounted share path (for example: \\\\192.168.1.8\\Plex)",
        onDismissRequest = onDismiss,
        widthFraction = 0.5f,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EpgSourceTextField(
                    value = sharePath,
                    onValueChange = { sharePath = it },
                    placeholder = "//server/share or \\\\server\\share"
                )
                EpgSourceTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Username"
                )
                EpgSourceTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    visualTransformation = PasswordVisualTransformation()
                )
                EpgSourceTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    placeholder = "Domain or workgroup"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guest / anonymous",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = guest,
                        onCheckedChange = { guest = it }
                    )
                }
            }
            PremiumDialogActionButton(
                label = "Add",
                enabled = sharePath.isNotBlank() && !isScanning,
                onClick = {
                    onAdd(
                        LocalMediaNetworkShareInput(
                            sharePath = sharePath.trim(),
                            username = username.takeIf { it.isNotBlank() },
                            password = password.takeIf { it.isNotBlank() },
                            domain = domain.takeIf { it.isNotBlank() },
                            guest = guest
                        )
                    )
                }
            )
        },
        footer = {
            PremiumDialogFooterButton(
                label = "Cancel",
                onClick = onDismiss
            )
        }
    )
}
