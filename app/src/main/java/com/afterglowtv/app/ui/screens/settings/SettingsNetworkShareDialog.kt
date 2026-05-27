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
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.app.ui.theme.TextSecondary
import com.afterglowtv.domain.model.SmbShareConfig

@Composable
internal fun NetworkShareDialog(
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onAddShare: (SmbShareConfig) -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var shareName by rememberSaveable { mutableStateOf("") }
    var folderPath by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var domain by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("445") }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                NetworkShareField("Name", displayName, "NAS Movies") { displayName = it }
                NetworkShareField("Host", host, "192.168.1.20 or qnap.local") { host = it }
                NetworkShareField("Share", shareName, "Multimedia") { shareName = it }
                NetworkShareField("Folder", folderPath, "Movies/Action") { folderPath = it }
                NetworkShareField("Username", username, "Optional") { username = it }
                NetworkShareField(
                    label = "Password",
                    value = password,
                    placeholder = "Optional",
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { password = it }
                )
                NetworkShareField("Domain", domain, "Optional") { domain = it }
                NetworkShareField("Port", port, "445") { port = it.filter(Char::isDigit).take(5) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isScanning,
                onClick = {
                    val parsedPort = port.toIntOrNull() ?: 445
                    when {
                        host.isBlank() -> validationMessage = "Host is required."
                        shareName.isBlank() -> validationMessage = "Share is required."
                        parsedPort !in 1..65_535 -> validationMessage = "Port must be between 1 and 65535."
                        else -> {
                            onAddShare(
                                SmbShareConfig(
                                    host = host,
                                    shareName = shareName,
                                    path = folderPath,
                                    displayName = displayName,
                                    username = username,
                                    password = password,
                                    domain = domain,
                                    port = parsedPort
                                )
                            )
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(text = if (isScanning) "Scanning" else "Add", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = androidx.compose.ui.res.stringResource(R.string.settings_cancel), color = OnSurface)
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
