package com.afterglowtv.app.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.R
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.Secondary

internal fun LazyListScope.settingsBackupSection(
    onCreateBackup: () -> Unit,
    onShareBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackupActionCard(
                    icon = "\u2191",
                    title = stringResource(R.string.settings_backup_data),
                    subtitle = stringResource(R.string.settings_backup_export_subtitle),
                    accent = Primary,
                    onClick = onCreateBackup,
                    modifier = Modifier.weight(1f)
                )
                BackupActionCard(
                    icon = "\u21aa",
                    title = stringResource(R.string.settings_backup_share_data),
                    subtitle = stringResource(R.string.settings_backup_share_subtitle),
                    accent = Primary,
                    onClick = onShareBackup,
                    modifier = Modifier.weight(1f)
                )
            }
            BackupActionCard(
                icon = "\u2193",
                title = stringResource(R.string.settings_restore_data),
                subtitle = stringResource(R.string.settings_restore_subtitle),
                accent = Secondary,
                onClick = onRestoreBackup,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun BackupActionCard(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvClickableSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = accent.copy(alpha = 0.12f),
            focusedContainerColor = accent.copy(alpha = 0.28f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = accent, textAlign = TextAlign.Center)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim, textAlign = TextAlign.Center)
        }
    }
}

internal fun LazyListScope.settingsAboutSection(
    uiState: SettingsUiState,
    context: Context,
    buildVerificationLabel: String,
    onOpenUri: (String) -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallDownloadedUpdate: () -> Unit,
    onDownloadLatestUpdate: () -> Unit,
    onSetAutoCheckAppUpdates: (Boolean) -> Unit,
    onSetAutoDownloadAppUpdates: (Boolean) -> Unit,
    onSetDeveloperModeEnabled: (Boolean) -> Unit,
    onRefreshDownloadState: () -> Unit,
    onViewCrashReport: () -> Unit,
    onShareCrashReport: () -> Unit,
    onDeleteCrashReport: () -> Unit
) {
    item {
        var developerTapCount by rememberSaveable { mutableStateOf(0) }
        var showDeveloperPasswordDialog by rememberSaveable { mutableStateOf(false) }
        var developerPassword by rememberSaveable { mutableStateOf("") }
        var developerPasswordError by rememberSaveable { mutableStateOf(false) }
        val downloadStatus = uiState.appUpdate.downloadStatus
        val showSideloadUpdates = StorePolicy.current.enableSideloadUpdates
        LaunchedEffect(downloadStatus) {
            if (downloadStatus == com.afterglowtv.app.update.AppUpdateDownloadStatus.Downloading) {
                while (true) {
                    kotlinx.coroutines.delay(2000L)
                    onRefreshDownloadState()
                }
            }
        }
        if (showDeveloperPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeveloperPasswordDialog = false
                    developerPassword = ""
                    developerPasswordError = false
                },
                title = { Text(text = "Developer Mode") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter the unlock code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceDim
                        )
                        OutlinedTextField(
                            value = developerPassword,
                            onValueChange = {
                                developerPassword = it.take(8)
                                developerPasswordError = false
                            },
                            singleLine = true,
                            isError = developerPasswordError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            )
                        )
                        if (developerPasswordError) {
                            Text(
                                text = "Incorrect code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (developerPassword == "1337") {
                                onSetDeveloperModeEnabled(true)
                                showDeveloperPasswordDialog = false
                                developerPassword = ""
                                developerPasswordError = false
                                developerTapCount = 0
                            } else {
                                developerPasswordError = true
                            }
                        }
                    ) {
                        Text(text = "Unlock")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeveloperPasswordDialog = false
                            developerPassword = ""
                            developerPasswordError = false
                        }
                    ) {
                        Text(text = "Cancel")
                    }
                }
            )
        }
        if (showSideloadUpdates) {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_updates_title),
                subtitle = stringResource(R.string.settings_updates_subtitle)
            )
        }
        ClickableSettingsRow(
            label = stringResource(R.string.settings_app_version),
            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            onClick = {
                if (uiState.developerModeEnabled) return@ClickableSettingsRow
                developerTapCount += 1
                if (developerTapCount >= 7) {
                    developerTapCount = 0
                    showDeveloperPasswordDialog = true
                }
            }
        )
        if (showSideloadUpdates) {
            SwitchSettingsRow(
                label = stringResource(R.string.settings_update_auto_check),
                value = stringResource(
                    if (uiState.autoCheckAppUpdates) R.string.settings_enabled else R.string.settings_disabled
                ),
                checked = uiState.autoCheckAppUpdates,
                onCheckedChange = onSetAutoCheckAppUpdates
            )
            if (uiState.autoCheckAppUpdates) {
                SwitchSettingsRow(
                    label = stringResource(R.string.settings_update_auto_download),
                    value = stringResource(
                        if (uiState.autoDownloadAppUpdates) R.string.settings_enabled else R.string.settings_disabled
                    ),
                    checked = uiState.autoDownloadAppUpdates,
                    onCheckedChange = onSetAutoDownloadAppUpdates
                )
            }
            SettingsRow(
                label = stringResource(R.string.settings_update_latest_release),
                value = formatLatestReleaseLabel(uiState.appUpdate, context)
            )
            SettingsRow(
                label = stringResource(R.string.settings_update_status),
                value = formatUpdateStatusLabel(uiState.appUpdate, context)
            )
            SettingsRow(
                label = stringResource(R.string.settings_update_last_checked),
                value = formatUpdateCheckTimeLabel(uiState.appUpdate.lastCheckedAt, context)
            )
            ClickableSettingsRow(
                label = stringResource(R.string.settings_update_check_now),
                value = stringResource(
                    if (uiState.isCheckingForUpdates) R.string.settings_update_checking else R.string.settings_update_check_action
                ),
                onClick = {
                    if (!uiState.isCheckingForUpdates) {
                        onCheckForUpdates()
                    }
                }
            )
            if (shouldShowUpdateDownloadAction(uiState.appUpdate)) {
                ClickableSettingsRow(
                    label = stringResource(R.string.settings_update_download),
                    value = formatUpdateDownloadLabel(uiState.appUpdate, context),
                    onClick = {
                        if (uiState.appUpdate.downloadStatus == com.afterglowtv.app.update.AppUpdateDownloadStatus.Downloaded) {
                            onInstallDownloadedUpdate()
                        } else if (uiState.appUpdate.downloadStatus != com.afterglowtv.app.update.AppUpdateDownloadStatus.Downloading) {
                            onDownloadLatestUpdate()
                        }
                    }
                )
            }
            if (!uiState.appUpdate.releaseUrl.isNullOrBlank()) {
                ClickableSettingsRow(
                    label = stringResource(R.string.settings_update_view_release),
                    value = uiState.appUpdate.latestVersionName ?: stringResource(R.string.settings_update_release_notes),
                    onClick = { onOpenUri(uiState.appUpdate.releaseUrl.orEmpty()) }
                )
            }
            if (!uiState.appUpdate.errorMessage.isNullOrBlank()) {
                SettingsRow(
                    label = stringResource(R.string.settings_update_error),
                    value = uiState.appUpdate.errorMessage.orEmpty()
                )
            }
        }
    }

    item {
        SettingsSectionHeader(
            title = stringResource(R.string.settings_crash_reports_title),
            subtitle = stringResource(R.string.settings_crash_reports_subtitle)
        )
        if (uiState.crashReport.hasReport) {
            SettingsRow(
                label = stringResource(R.string.settings_crash_report_latest),
                value = uiState.crashReport.timestamp
            )
            SettingsRow(
                label = stringResource(R.string.settings_crash_report_exception),
                value = uiState.crashReport.exception.substringAfterLast('.')
            )
            ClickableSettingsRow(
                label = stringResource(R.string.settings_crash_report_view),
                value = stringResource(R.string.settings_crash_report_available),
                onClick = onViewCrashReport
            )
            ClickableSettingsRow(
                label = stringResource(R.string.settings_crash_report_share),
                value = uiState.crashReport.fileName,
                onClick = onShareCrashReport
            )
            ClickableSettingsRow(
                label = stringResource(R.string.settings_crash_report_delete),
                value = stringResource(R.string.settings_crash_report_delete_value),
                onClick = onDeleteCrashReport
            )
        } else {
            SettingsRow(
                label = stringResource(R.string.settings_crash_report_latest),
                value = stringResource(R.string.settings_crash_report_none)
            )
        }
    }

    item {
        SettingsRow(label = stringResource(R.string.settings_build), value = stringResource(R.string.settings_build_desc))
        SettingsRow(label = stringResource(R.string.settings_build_verification), value = buildVerificationLabel)
        SettingsRow(label = stringResource(R.string.settings_developed_by), value = stringResource(R.string.settings_developer_name))
        if (uiState.developerModeEnabled) {
            SettingsRow(label = "Developer Mode", value = stringResource(R.string.settings_enabled))
        }
        ClickableSettingsRow(
            label = stringResource(R.string.settings_github),
            value = stringResource(R.string.settings_github_url),
            onClick = { onOpenUri(context.getString(R.string.settings_github_url)) }
        )
    }
}
