package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.TextSecondary
import com.afterglowtv.domain.model.SmbShareConfig
import com.afterglowtv.domain.model.SmbShareUri

@Composable
internal fun NetworkShareDialog(
    isScanning: Boolean,
    status: String?,
    onDismiss: () -> Unit,
    onCancelScan: () -> Unit,
    onAddShare: (SmbShareConfig) -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf(BuildConfig.DEFAULT_NETWORK_SHARE_NAME) }
    var networkPath by rememberSaveable { mutableStateOf(BuildConfig.DEFAULT_NETWORK_SHARE_PATH) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var domain by rememberSaveable { mutableStateOf("") }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (isScanning) onCancelScan() else onDismiss()
        },
        title = { Text(text = "Add Network Share") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                validationMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
                if (isScanning) {
                    Text(
                        text = status ?: "Trying to connect to the network share...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
                NetworkShareField("Network path", networkPath, "\\\\NAS66B7CA\\Plex or \\\\192.168.1.8\\Plex") { value ->
                    networkPath = value
                }
                NetworkShareField("Name", displayName, "Plex") { displayName = it }
                NetworkShareField("Username", username, "Optional") { username = it }
                NetworkShareField(
                    label = "Password",
                    value = password,
                    placeholder = "Optional",
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { password = it }
                )
                NetworkShareField("Domain", domain, "Optional") { domain = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isScanning,
                onClick = {
                    val parsedPath = SmbShareUri.parse(networkPath)
                    when {
                        parsedPath == null ->
                            validationMessage = "Use a Windows share path like \\\\NAS66B7CA\\Plex."
                        else -> {
                            onAddShare(
                                SmbShareConfig(
                                    host = parsedPath.host,
                                    shareName = parsedPath.shareName,
                                    path = parsedPath.path,
                                    displayName = displayName,
                                    username = username,
                                    password = password,
                                    domain = domain,
                                    port = parsedPath.port
                                )
                            )
                        }
                    }
                }
            ) {
                Text(text = if (isScanning) "Working..." else "Add", color = Primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isScanning) onCancelScan() else onDismiss()
                }
            ) {
                Text(
                    text = if (isScanning) "Cancel load" else androidx.compose.ui.res.stringResource(R.string.settings_cancel),
                    color = OnSurface
                )
            }
        },
        containerColor = SurfaceElevated,
        titleContentColor = OnSurface,
        textContentColor = TextSecondary
    )
}

@Composable
private fun NetworkShareField(
    label: String,
    value: String,
    placeholder: String,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        EpgSourceTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            visualTransformation = visualTransformation
        )
    }
}
