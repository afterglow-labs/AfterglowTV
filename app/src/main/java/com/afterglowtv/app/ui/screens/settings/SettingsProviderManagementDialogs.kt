package com.afterglowtv.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.dialogs.PremiumDialog
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogFooterButton
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.design.afterglowButtonShape
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.OnBackground
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.app.ui.theme.Secondary
import com.afterglowtv.app.ui.theme.Surface
import com.afterglowtv.app.ui.theme.SurfaceElevated
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AddProviderSourceType {
    M3U_URL,
    M3U_FILE,
    XTREAM,
    PORTAL
}

@Composable
internal fun SettingsProviderManagementDialogs(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    providerState: SettingsProviderSectionState
) {
    val pendingSyncProvider = providerState.pendingSyncProviderId?.let { providerId ->
        uiState.providers.firstOrNull { it.id == providerId }
    }

    if (providerState.showAddProviderDialog) {
        AddProviderSourceDialog(
            uiState = uiState,
            initialKind = providerState.pendingAddProviderKind ?: ProviderM3uPlaylistKind.LIVE,
            initialPlaylistUri = providerState.pendingProviderPlaylistUri,
            onDismiss = {
                providerState.showAddProviderDialog = false
                providerState.pendingAddProviderKind = null
                providerState.pendingProviderPlaylistUri = null
                viewModel.clearAddProviderSourceState()
            },
            onAddM3u = { kind, name, playlistUrl, epgUrl, httpUserAgent, httpHeaders ->
                viewModel.addM3uProviderSource(
                    kind = kind,
                    name = name,
                    playlistUrl = playlistUrl,
                    epgUrl = epgUrl,
                    httpUserAgent = httpUserAgent,
                    httpHeaders = httpHeaders,
                    onSuccess = {
                        providerState.showAddProviderDialog = false
                        providerState.pendingAddProviderKind = null
                        providerState.pendingProviderPlaylistUri = null
                    }
                )
            },
            onAddXtream = { name, serverUrl, username, password, httpUserAgent, httpHeaders ->
                viewModel.addXtreamProviderSource(
                    name = name,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    httpUserAgent = httpUserAgent,
                    httpHeaders = httpHeaders,
                    onSuccess = {
                        providerState.showAddProviderDialog = false
                        providerState.pendingAddProviderKind = null
                        providerState.pendingProviderPlaylistUri = null
                    }
                )
            },
            onAddPortal = { name, portalUrl, macAddress, deviceProfile, timezone, locale ->
                viewModel.addPortalProviderSource(
                    name = name,
                    portalUrl = portalUrl,
                    macAddress = macAddress,
                    deviceProfile = deviceProfile,
                    timezone = timezone,
                    locale = locale,
                    onSuccess = {
                        providerState.showAddProviderDialog = false
                        providerState.pendingAddProviderKind = null
                        providerState.pendingProviderPlaylistUri = null
                    }
                )
            }
        )
    }

    if (providerState.showCreateCombinedDialog) {
        var isCreating by remember { mutableStateOf(false) }
        CreateCombinedM3uDialog(
            providers = uiState.availableM3uProviders,
            isSubmitting = isCreating,
            onDismiss = { providerState.showCreateCombinedDialog = false },
            onCreate = { name, providerIds ->
                isCreating = true
                viewModel.createCombinedProfile(name, providerIds,
                    onSuccess = { providerState.showCreateCombinedDialog = false },
                    onError = { isCreating = false }
                )
            }
        )
    }

    if (providerState.showRenameCombinedDialog) {
        val selectedProfile = uiState.combinedProfiles.firstOrNull { it.id == providerState.selectedCombinedProfileId }
        if (selectedProfile != null) {
            var isRenaming by remember(selectedProfile.id) { mutableStateOf(false) }
            RenameCombinedM3uDialog(
                profile = selectedProfile,
                isSubmitting = isRenaming,
                onDismiss = { providerState.showRenameCombinedDialog = false },
                onRename = { name ->
                    isRenaming = true
                    viewModel.renameCombinedProfile(selectedProfile.id, name,
                        onSuccess = { providerState.showRenameCombinedDialog = false },
                        onError = { isRenaming = false }
                    )
                }
            )
        }
    }

    if (providerState.showAddCombinedMemberDialog) {
        val selectedProfile = uiState.combinedProfiles.firstOrNull { it.id == providerState.selectedCombinedProfileId }
        if (selectedProfile != null) {
            var isAddingMember by remember(selectedProfile.id) { mutableStateOf(false) }
            AddCombinedProviderDialog(
                profile = selectedProfile,
                availableProviders = uiState.availableM3uProviders,
                isSubmitting = isAddingMember,
                onDismiss = { providerState.showAddCombinedMemberDialog = false },
                onAddProvider = { providerId ->
                    isAddingMember = true
                    viewModel.addProviderToCombinedProfile(selectedProfile.id, providerId,
                        onSuccess = { providerState.showAddCombinedMemberDialog = false },
                        onError = { isAddingMember = false }
                    )
                }
            )
        }
    }

    if (providerState.showProviderSyncDialog && pendingSyncProvider != null) {
        ProviderSyncOptionsDialog(
            provider = pendingSyncProvider,
            onDismiss = {
                providerState.showProviderSyncDialog = false
                providerState.pendingSyncProviderId = null
            },
            onSelect = { selection ->
                providerState.showProviderSyncDialog = false
                if (selection == null) {
                    providerState.showCustomProviderSyncDialog = true
                } else {
                    viewModel.syncProviderSection(pendingSyncProvider.id, selection)
                    providerState.pendingSyncProviderId = null
                }
            }
        )
    }

    if (providerState.showCustomProviderSyncDialog && pendingSyncProvider != null) {
        ProviderCustomSyncDialog(
            provider = pendingSyncProvider,
            selected = providerState.customSyncSelections,
            onToggle = { option ->
                providerState.customSyncSelections =
                    if (option in providerState.customSyncSelections) {
                        providerState.customSyncSelections - option
                    } else {
                        providerState.customSyncSelections + option
                    }
            },
            onDismiss = {
                providerState.showCustomProviderSyncDialog = false
                providerState.pendingSyncProviderId = null
            },
            onConfirm = {
                providerState.showCustomProviderSyncDialog = false
                viewModel.syncProviderCustom(pendingSyncProvider.id, providerState.customSyncSelections)
                providerState.pendingSyncProviderId = null
            }
        )
    }

    val pendingDeleteProviderId = providerState.pendingDeleteProviderId
    if (pendingDeleteProviderId != null) {
        val providerToDelete = uiState.providers.firstOrNull { it.id == pendingDeleteProviderId }
        val providerName = providerToDelete?.name ?: "this provider"
        PremiumDialog(
            title = "Delete Provider",
            subtitle = "Delete \"$providerName\"? This will permanently remove all its channels, programs, and sync data.",
            onDismissRequest = { if (!uiState.isDeletingProvider) providerState.pendingDeleteProviderId = null },
            widthFraction = 0.48f,
            content = {},
            footer = {
                PremiumDialogFooterButton(
                    label = stringResource(R.string.settings_cancel),
                    onClick = { providerState.pendingDeleteProviderId = null },
                    enabled = !uiState.isDeletingProvider
                )
                PremiumDialogFooterButton(
                    label = "Delete",
                    onClick = {
                        viewModel.deleteProvider(pendingDeleteProviderId,
                            onSuccess = { providerState.pendingDeleteProviderId = null }
                        )
                    },
                    enabled = !uiState.isDeletingProvider,
                    destructive = true
                )
            }
        )
    }
}

@Composable
private fun AddProviderSourceDialog(
    uiState: SettingsUiState,
    initialKind: ProviderM3uPlaylistKind,
    initialPlaylistUri: String?,
    onDismiss: () -> Unit,
    onAddM3u: (ProviderM3uPlaylistKind, String, String, String, String, String) -> Unit,
    onAddXtream: (String, String, String, String, String, String) -> Unit,
    onAddPortal: (String, String, String, String, String, String) -> Unit
) {
    val availableTypes = remember {
        listOf(
            AddProviderSourceType.M3U_URL,
            AddProviderSourceType.M3U_FILE,
            AddProviderSourceType.XTREAM,
            AddProviderSourceType.PORTAL
        )
    }
    var sourceType by rememberSaveable {
        mutableStateOf(if (initialPlaylistUri.isNullOrBlank()) AddProviderSourceType.M3U_URL else AddProviderSourceType.M3U_FILE)
    }
    var playlistKind by rememberSaveable(initialKind) {
        mutableStateOf(initialKind)
    }
    var name by rememberSaveable { mutableStateOf("") }
    var playlistUrl by rememberSaveable(initialPlaylistUri) { mutableStateOf(initialPlaylistUri.orEmpty()) }
    var epgUrl by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var portalUrl by rememberSaveable { mutableStateOf("") }
    var macAddress by rememberSaveable { mutableStateOf("") }
    var deviceProfile by rememberSaveable { mutableStateOf("") }
    var timezone by rememberSaveable { mutableStateOf("") }
    var locale by rememberSaveable { mutableStateOf("") }
    var httpUserAgent by rememberSaveable { mutableStateOf("") }
    var httpHeaders by rememberSaveable { mutableStateOf("") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var pickerError by rememberSaveable { mutableStateOf<String?>(null) }
    var isCopyingPlaylistFile by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val playlistFileLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult()
    ) { result ->
        result.data?.data?.let { selectedUri ->
            pickerError = null
            persistReadPermissionIfAvailable(context, selectedUri)
            scope.launch {
                isCopyingPlaylistFile = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        copyPlaylistUriToInternalFile(context, selectedUri)
                    }
                }.onSuccess { copiedUrl ->
                    playlistUrl = copiedUrl
                    sourceType = AddProviderSourceType.M3U_FILE
                }.onFailure { error ->
                    pickerError = error.message?.takeIf { it.isNotBlank() }
                        ?: "Could not import the selected playlist file."
                }
                isCopyingPlaylistFile = false
            }
        }
    }

    fun launchPlaylistPicker() {
        val mimeTypes = arrayOf("*/*")
        launchDocumentPickerSafely(
            unavailableMessage = "Could not open the file picker. Paste a playlist URL or a file:// path instead.",
            onError = { pickerError = it },
            launchPrimary = { playlistFileLauncher.launch(openDocumentIntent(mimeTypes)) },
            launchFallback = { playlistFileLauncher.launch(getContentIntent(mimeTypes)) }
        )
    }

    PremiumDialog(
        title = "Add Source",
        subtitle = "Choose a source type and enter the details from your provider.",
        onDismissRequest = { if (!uiState.isAddingProviderSource) onDismiss() },
        widthFraction = 0.76f,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier.width(230.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    availableTypes.forEach { type ->
                        AddProviderSourceCard(
                            title = when (type) {
                                AddProviderSourceType.M3U_URL -> "Playlist Link"
                                AddProviderSourceType.M3U_FILE -> "Playlist File"
                                AddProviderSourceType.XTREAM -> "Xtream"
                                AddProviderSourceType.PORTAL -> "Portal"
                            },
                            subtitle = when (type) {
                                AddProviderSourceType.M3U_URL -> "URL from your provider"
                                AddProviderSourceType.M3U_FILE -> "Local storage or USB"
                                AddProviderSourceType.XTREAM -> "Server login"
                                AddProviderSourceType.PORTAL -> "Portal URL and MAC"
                            },
                            selected = sourceType == type,
                            onClick = {
                                sourceType = type
                                if (type == AddProviderSourceType.M3U_FILE) {
                                    launchPlaylistPicker()
                                }
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Surface.copy(alpha = 0.72f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when (sourceType) {
                            AddProviderSourceType.M3U_URL -> "Playlist Link"
                            AddProviderSourceType.M3U_FILE -> "Playlist File"
                            AddProviderSourceType.XTREAM -> "Xtream"
                            AddProviderSourceType.PORTAL -> "Portal"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnBackground
                    )
                    Text(
                        text = when (sourceType) {
                            AddProviderSourceType.M3U_URL -> "Add an M3U playlist URL."
                            AddProviderSourceType.M3U_FILE -> "Choose a local M3U file."
                            AddProviderSourceType.XTREAM -> "Use the server URL, username, and password from your provider."
                            AddProviderSourceType.PORTAL -> "Use the portal URL and registered MAC address from your provider."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )

                    when (sourceType) {
                    AddProviderSourceType.M3U_URL,
                    AddProviderSourceType.M3U_FILE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AddProviderChoiceButton(
                                label = "Live TV",
                                selected = playlistKind == ProviderM3uPlaylistKind.LIVE,
                                onClick = { playlistKind = ProviderM3uPlaylistKind.LIVE }
                            )
                            AddProviderChoiceButton(
                                label = "VOD",
                                selected = playlistKind == ProviderM3uPlaylistKind.VOD,
                                onClick = { playlistKind = ProviderM3uPlaylistKind.VOD }
                            )
                        }
                        EpgSourceTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Source name"
                        )
                        EpgSourceTextField(
                            value = playlistUrl,
                            onValueChange = { playlistUrl = it },
                            placeholder = if (sourceType == AddProviderSourceType.M3U_FILE) "Selected playlist file" else "Playlist URL"
                        )
                        if (sourceType == AddProviderSourceType.M3U_FILE) {
                            AddProviderChoiceButton(
                                label = if (isCopyingPlaylistFile) "Importing..." else "Choose Playlist File",
                                selected = false,
                                onClick = { launchPlaylistPicker() }
                            )
                        }
                        EpgSourceTextField(
                            value = epgUrl,
                            onValueChange = { epgUrl = it },
                            placeholder = "EPG URL (optional)"
                        )
                    }
                    AddProviderSourceType.XTREAM -> {
                        EpgSourceTextField(name, { name = it }, "Source name")
                        EpgSourceTextField(serverUrl, { serverUrl = it }, "Server URL")
                        EpgSourceTextField(username, { username = it }, "Username")
                        EpgSourceTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    AddProviderSourceType.PORTAL -> {
                        EpgSourceTextField(name, { name = it }, "Source name")
                        EpgSourceTextField(portalUrl, { portalUrl = it }, "Portal URL")
                        EpgSourceTextField(macAddress, { macAddress = it }, "MAC address")
                        EpgSourceTextField(deviceProfile, { deviceProfile = it }, "Device profile (optional)")
                        EpgSourceTextField(timezone, { timezone = it }, "Timezone (optional)")
                        EpgSourceTextField(locale, { locale = it }, "Locale (optional)")
                    }
                }

                    if (sourceType != AddProviderSourceType.PORTAL) {
                        AddProviderAdvancedPanel(
                            expanded = showAdvanced,
                            onToggle = { showAdvanced = !showAdvanced },
                            httpUserAgent = httpUserAgent,
                            onHttpUserAgentChange = { httpUserAgent = it },
                            httpHeaders = httpHeaders,
                            onHttpHeadersChange = { httpHeaders = it }
                        )
                    }

                uiState.addProviderSourceProgress?.let { progress ->
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }
                pickerError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.afterglowtv.app.ui.theme.ErrorColor
                    )
                }
                uiState.addProviderSourceError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.afterglowtv.app.ui.theme.ErrorColor
                    )
                }
            }
            }
        },
        footer = {
            PremiumDialogFooterButton(
                label = stringResource(R.string.settings_cancel),
                onClick = onDismiss,
                enabled = !uiState.isAddingProviderSource
            )
            PremiumDialogFooterButton(
                label = if (uiState.isAddingProviderSource) "Adding..." else "Add Source",
                onClick = {
                    when (sourceType) {
                        AddProviderSourceType.M3U_URL,
                        AddProviderSourceType.M3U_FILE -> onAddM3u(playlistKind, name, playlistUrl, epgUrl, httpUserAgent, httpHeaders)
                        AddProviderSourceType.XTREAM -> onAddXtream(name, serverUrl, username, password, httpUserAgent, httpHeaders)
                        AddProviderSourceType.PORTAL -> onAddPortal(name, portalUrl, macAddress, deviceProfile, timezone, locale)
                    }
                },
                enabled = !uiState.isAddingProviderSource && !isCopyingPlaylistFile
            )
        }
    )
}

@Composable
private fun AddProviderSourceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    TvClickableSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Primary.copy(alpha = 0.2f) else SurfaceElevated.copy(alpha = 0.84f),
            focusedContainerColor = if (selected) Primary.copy(alpha = 0.32f) else SurfaceElevated
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, Color.White),
                shape = shape
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Primary else OnBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
        }
    }
}

@Composable
private fun AddProviderAdvancedPanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    httpUserAgent: String,
    onHttpUserAgentChange: (String) -> Unit,
    httpHeaders: String,
    onHttpHeadersChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated.copy(alpha = 0.78f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TvClickableSurface(
            onClick = onToggle,
            shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Primary.copy(alpha = 0.18f)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Advanced", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                    Text("Custom user-agent and HTTP headers.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                }
                Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = Primary)
            }
        }
        if (expanded) {
            EpgSourceTextField(httpUserAgent, onHttpUserAgentChange, "HTTP user-agent (optional)")
            EpgSourceTextField(httpHeaders, onHttpHeadersChange, "HTTP headers (optional)")
        }
    }
}

@Composable
private fun AddProviderChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = afterglowButtonShape(com.afterglowtv.app.ui.design.AppStyles.value.button)
    TvClickableSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Primary.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f),
            focusedContainerColor = if (selected) Primary.copy(alpha = 0.46f) else Color.White.copy(alpha = 0.14f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, Color.White),
                shape = shape
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Primary else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
