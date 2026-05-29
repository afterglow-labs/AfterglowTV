package com.afterglowtv.app.ui.screens.provider

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.afterglowtv.app.ui.interaction.mouseClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.afterglowtv.app.R
import com.afterglowtv.app.device.rememberIsTelevisionDevice
import com.afterglowtv.app.ui.components.dialogs.PremiumDialog
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogFooterButton
import com.afterglowtv.app.ui.components.shell.StatusPill
import androidx.compose.ui.draw.clip
import com.afterglowtv.app.ui.design.AppStyles
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.app.ui.design.afterglowButtonShape
import com.afterglowtv.app.ui.screens.settings.BackupImportPreviewDialog
import com.afterglowtv.app.ui.theme.*
import com.afterglowtv.data.util.ProviderInputSanitizer
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderXtreamLiveSyncMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

// Source type

private enum class SourceType { XTREAM, STALKER, M3U_URL, M3U_FILE }

internal fun shouldInlineProviderSetupActions(
    isTelevisionDevice: Boolean,
    viewportHeightDp: Int
): Boolean = !isTelevisionDevice || viewportHeightDp < 520

internal fun shouldUseProviderSetupSideRail(
    isTelevisionDevice: Boolean,
    viewportWidthDp: Int
): Boolean = isTelevisionDevice && viewportWidthDp >= 700

// Screen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderSetupScreen(
    onProviderAdded: () -> Unit,
    onBack: () -> Unit,
    editProviderId: Long? = null,
    initialImportUri: String? = null,
    viewModel: ProviderSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val knownLocalM3uUrls by viewModel.knownLocalM3uUrls.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var appFilesDir by remember { mutableStateOf<java.io.File?>(null) }

    // Local form state
    var selectedTab by rememberSaveable { mutableStateOf(2) }
    var name by rememberSaveable { mutableStateOf("") }
    var m3uUrl by rememberSaveable { mutableStateOf("") }
    var m3uEpgUrl by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var httpUserAgent by rememberSaveable { mutableStateOf("") }
    var httpHeaders by rememberSaveable { mutableStateOf("") }
    var stalkerMacAddress by rememberSaveable { mutableStateOf("") }
    var stalkerDeviceProfile by rememberSaveable { mutableStateOf("") }
    var stalkerDeviceTimezone by rememberSaveable { mutableStateOf("") }
    var stalkerDeviceLocale by rememberSaveable { mutableStateOf("") }
    var fileImportError by rememberSaveable { mutableStateOf<String?>(null) }
    var handledInitialImportUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDraftDialog by rememberSaveable { mutableStateOf(false) }

    // File import helper
    fun importM3uUri(uri: android.net.Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        fileImportError = context.getString(R.string.setup_file_import_failed)
                    }
                    return@launch
                }
                inputStream.use {
                    var fileName = "Local_Playlist"
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) {
                                val displayName = c.getString(idx)
                                fileName = if (displayName.contains(".")) displayName.substringBeforeLast(".") else displayName
                            }
                        }
                    }
                    val ext = if (uri.toString().substringBefore('?').lowercase().endsWith(".m3u8")) "m3u8" else "m3u"
                    val outFile = java.io.File(context.filesDir, "m3u_${System.currentTimeMillis()}.$ext")
                    outFile.outputStream().use { out -> inputStream.copyTo(out) }
                    cleanupOldImportedM3uFiles(
                        filesDir = context.filesDir,
                        protectedFileUris = knownLocalM3uUrls + "file://${outFile.absolutePath}",
                        keepLatest = 20
                    )
                    withContext(Dispatchers.Main) {
                        viewModel.updateM3uTab(1)
                        m3uUrl = "file://${outFile.absolutePath}"
                        if (name.isEmpty()) name = ProviderInputSanitizer.sanitizeProviderNameForEditing(fileName)
                        fileImportError = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { fileImportError = resolveFileImportError(context, e) }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? -> if (uri != null) importM3uUri(uri) }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? -> uri?.let { viewModel.inspectBackup(it.toString()) } }

    // Effects
    LaunchedEffect(context) {
        appFilesDir = withContext(Dispatchers.IO) { context.filesDir }
    }

    LaunchedEffect(appFilesDir, knownLocalM3uUrls) {
        appFilesDir?.let { filesDir ->
            cleanupOldImportedM3uFilesAsync(filesDir, knownLocalM3uUrls, 20)
        }
    }

    LaunchedEffect(initialImportUri) {
        val importUri = initialImportUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (handledInitialImportUri == importUri) return@LaunchedEffect
        handledInitialImportUri = importUri
        selectedTab = 2
        viewModel.updateM3uTab(1)
        runCatching { android.net.Uri.parse(importUri) }.getOrNull()?.let(::importM3uUri)
    }

    LaunchedEffect(uiState.backupImportSuccess) {
        if (uiState.backupImportSuccess) {
            onProviderAdded()
        }
    }
    appFilesDir?.let { filesDir ->
        ProviderSetupCompletionLayer(
            uiState = uiState,
            knownLocalM3uUrls = knownLocalM3uUrls,
            selectedM3uUrl = m3uUrl,
            filesDir = filesDir,
            onProviderAdded = onProviderAdded,
            onAttachCreatedProvider = viewModel::attachCreatedProviderToCombined,
            onSkipCreatedProviderCombinedAttach = viewModel::skipCreatedProviderCombinedAttach
        )
    }

    LaunchedEffect(editProviderId) {
        if (editProviderId != null) viewModel.loadProvider(editProviderId)
    }

    LaunchedEffect(uiState.isEditing, uiState.existingProviderId) {
        if (uiState.isEditing) {
            selectedTab = uiState.selectedTab
            name = uiState.name
            serverUrl = uiState.serverUrl
            username = uiState.username
            password = uiState.password
            m3uUrl = uiState.m3uUrl
            m3uEpgUrl = uiState.m3uEpgUrl
            httpUserAgent = uiState.httpUserAgent
            httpHeaders = uiState.httpHeaders
            stalkerMacAddress = uiState.stalkerMacAddress
            stalkerDeviceProfile = uiState.stalkerDeviceProfile
            stalkerDeviceTimezone = uiState.stalkerDeviceTimezone
            stalkerDeviceLocale = uiState.stalkerDeviceLocale
        }
    }

    LaunchedEffect(uiState.developerModeEnabled, uiState.isEditing) {
        if (!uiState.developerModeEnabled && !uiState.isEditing && selectedTab < 2) {
            selectedTab = 2
            viewModel.updateM3uTab(0)
            viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.M3U)
        }
    }

    // Derived UI source type
    val sourceType = when {
        selectedTab == 0 -> SourceType.XTREAM
        selectedTab == 1 -> SourceType.STALKER
        uiState.m3uTab == 1 -> SourceType.M3U_FILE
        else -> SourceType.M3U_URL
    }

    fun onSourceTypeSelected(type: SourceType) {
        if (uiState.isEditing) return
        if (!uiState.developerModeEnabled && type !in setOf(SourceType.M3U_URL, SourceType.M3U_FILE)) return
        when (type) {
            SourceType.XTREAM  -> {
                selectedTab = 0
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.XTREAM)
            }
            SourceType.STALKER -> {
                selectedTab = 1
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.STALKER)
            }
            SourceType.M3U_URL -> {
                selectedTab = 2
                viewModel.updateM3uTab(0)
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.M3U)
            }
            SourceType.M3U_FILE-> {
                selectedTab = 2
                viewModel.updateM3uTab(1)
                viewModel.applySourceDefaults(ProviderSetupViewModel.SetupSourceType.M3U)
            }
        }
    }

    val hasUnsavedDraft = !uiState.isEditing && (
        name.isNotBlank() ||
            serverUrl.isNotBlank() ||
            username.isNotBlank() ||
            password.isNotBlank() ||
            httpUserAgent.isNotBlank() ||
            httpHeaders.isNotBlank() ||
            stalkerMacAddress.isNotBlank() ||
            stalkerDeviceProfile.isNotBlank() ||
            stalkerDeviceTimezone.isNotBlank() ||
            stalkerDeviceLocale.isNotBlank() ||
            m3uUrl.isNotBlank() ||
            m3uEpgUrl.isNotBlank()
        )

    BackHandler {
        if (hasUnsavedDraft) {
            showDiscardDraftDialog = true
        } else {
            onBack()
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    // Afterglow TV hero treatment: bleed the active palette across the screen
    // (vertical gradient + warm radial in the top-right corner that echoes the
    // logo's melting-sun motif), drop the brand strip on top, then the form.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (com.afterglowtv.app.ui.design.AppColors.backgroundGradientsEnabled) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                com.afterglowtv.app.ui.design.AppColors.TiviSurfaceDeep,
                                com.afterglowtv.app.ui.design.AppColors.TiviSurfaceBase,
                                com.afterglowtv.app.ui.design.AppColors.TiviSurfaceCool,
                            )
                        )
                    )
                } else {
                    Modifier.background(com.afterglowtv.app.ui.design.AppColors.TiviSurfaceDeep)
                }
            )
    ) {
        if (com.afterglowtv.app.ui.design.AppColors.backgroundGradientsEnabled) {
            // Off-screen accent melt in the top-right corner.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                com.afterglowtv.app.ui.design.AppColors.TiviAccent.copy(
                                    alpha = com.afterglowtv.app.ui.design.AppColors.palette.glowAlpha(0.24f),
                                ),
                                com.afterglowtv.app.ui.design.AppColors.TiviAccent.copy(alpha = 0.0f),
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = 2400f, y = -200f),
                            radius = 1400f,
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                com.afterglowtv.app.ui.design.AppColors.EpgNowLine.copy(
                                    alpha = com.afterglowtv.app.ui.design.AppColors.palette.glowAlpha(0.16f),
                                ),
                                com.afterglowtv.app.ui.design.AppColors.EpgNowLine.copy(alpha = 0.0f),
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = 400f, y = 1900f),
                            radius = 1100f,
                        )
                    )
            )
        }
        val isTelevisionDevice = rememberIsTelevisionDevice()
        val isWide = shouldUseProviderSetupSideRail(
            isTelevisionDevice = isTelevisionDevice,
            viewportWidthDp = maxWidth.value.toInt()
        )
        val isShortPhoneLandscape = !isTelevisionDevice && maxHeight < 520.dp
        val hPad = if (isWide) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPad, vertical = if (isShortPhoneLandscape) 8.dp else 14.dp)
        ) {
            // ── Hero brand strip ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isShortPhoneLandscape) 8.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        id = com.afterglowtv.app.R.drawable.afterglow_logo
                    ),
                    contentDescription = "AfterglowTV",
                    modifier = Modifier
                        .size(
                            when {
                                isShortPhoneLandscape -> 40.dp
                                isWide -> 64.dp
                                else -> 48.dp
                            }
                        )
                        .afterglow(
                            specs = listOf(
                                GlowSpec(
                                    color = com.afterglowtv.app.ui.design.AppColors.TiviAccent,
                                    radius = 18.dp,
                                    opacity = 0.55f,
                                ),
                                GlowSpec(
                                    color = com.afterglowtv.app.ui.design.AppColors.EpgNowLine,
                                    radius = 32.dp,
                                    opacity = 0.30f,
                                ),
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                )
                Column {
                    // Single-line "Afterglow TV" wordmark. Mixed weight + color so
                    // the "TV" reads as a sub-product mark without breaking the line.
                    Row(verticalAlignment = Alignment.Bottom) {
                        androidx.tv.material3.Text(
                            text = "Afterglow",
                            style = androidx.tv.material3.MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                            color = com.afterglowtv.app.ui.design.AppColors.TextPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                        androidx.tv.material3.Text(
                            text = "TV",
                            style = androidx.tv.material3.MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                            ),
                            color = com.afterglowtv.app.ui.design.AppColors.TiviAccent,
                            modifier = Modifier.padding(bottom = if (isWide) 4.dp else 2.dp),
                        )
                    }
                    androidx.tv.material3.Text(
                        text = "First-time setup",
                        style = androidx.tv.material3.MaterialTheme.typography.titleSmall.copy(
                            letterSpacing = 0.sp,
                        ),
                        color = com.afterglowtv.app.ui.design.AppColors.TiviAccentLight,
                    )
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SourceTypeSelectorPanel(
                        sourceType = sourceType,
                        isEditing = uiState.isEditing,
                        developerModeEnabled = uiState.developerModeEnabled,
                        isEditLabel = if (uiState.isEditing) androidx.compose.ui.res.stringResource(R.string.setup_edit_provider)
                                      else androidx.compose.ui.res.stringResource(R.string.setup_provider_title),
                        onSelect = ::onSourceTypeSelected,
                        modifier = Modifier.width(240.dp).fillMaxHeight()
                    )
                    ProviderFormContent(
                        sourceType = sourceType,
                        uiState = uiState,
                        name = name, onNameChange = { name = ProviderInputSanitizer.sanitizeProviderNameForEditing(it) },
                        serverUrl = serverUrl, onServerUrlChange = { serverUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                        username = username, onUsernameChange = { username = ProviderInputSanitizer.sanitizeUsernameForEditing(it) },
                        password = password, onPasswordChange = { password = ProviderInputSanitizer.sanitizePasswordForEditing(it) },
                        m3uUrl = m3uUrl, onM3uUrlChange = { m3uUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                        m3uEpgUrl = m3uEpgUrl, onM3uEpgUrlChange = { m3uEpgUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                        httpUserAgent = httpUserAgent, onHttpUserAgentChange = { httpUserAgent = ProviderInputSanitizer.sanitizeHttpUserAgentForEditing(it) },
                        httpHeaders = httpHeaders, onHttpHeadersChange = { httpHeaders = ProviderInputSanitizer.sanitizeHttpHeadersForEditing(it) },
                        stalkerMacAddress = stalkerMacAddress, onStalkerMacAddressChange = { stalkerMacAddress = ProviderInputSanitizer.sanitizeMacAddressForEditing(it) },
                        stalkerDeviceProfile = stalkerDeviceProfile, onStalkerDeviceProfileChange = { stalkerDeviceProfile = ProviderInputSanitizer.sanitizeDeviceProfileForEditing(it) },
                        stalkerDeviceTimezone = stalkerDeviceTimezone, onStalkerDeviceTimezoneChange = { stalkerDeviceTimezone = ProviderInputSanitizer.sanitizeTimezoneForEditing(it) },
                        stalkerDeviceLocale = stalkerDeviceLocale, onStalkerDeviceLocaleChange = { stalkerDeviceLocale = ProviderInputSanitizer.sanitizeLocaleForEditing(it) },
                        fileImportError = fileImportError,
                        onFilePick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onLoginXtream = { viewModel.loginXtream(serverUrl, username, password, name, httpUserAgent, httpHeaders) },
                        onLoginStalker = { viewModel.loginStalker(serverUrl, stalkerMacAddress, name, stalkerDeviceProfile, stalkerDeviceTimezone, stalkerDeviceLocale) },
                        onAddM3u = { viewModel.addM3u(m3uUrl, name, httpUserAgent, httpHeaders, m3uEpgUrl) },
                        onToggleM3uVodClassification = { viewModel.updateM3uVodClassificationEnabled(!uiState.m3uVodClassificationEnabled) },
                        onToggleXtreamFastSync = { viewModel.updateXtreamFastSyncEnabled(!uiState.xtreamFastSyncEnabled) },
                        onSelectEpgSyncMode = viewModel::updateEpgSyncMode,
                        onSelectXtreamLiveSyncMode = viewModel::updateXtreamLiveSyncMode,
                        showImportBackupButton = !uiState.isEditing,
                        isImportingBackup = uiState.isImportingBackup || uiState.syncProgress != null,
                        onImportBackup = { backupImportLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SourceTypeTabRow(
                        sourceType = sourceType,
                        isEditing = uiState.isEditing,
                        developerModeEnabled = uiState.developerModeEnabled,
                        onSelect = ::onSourceTypeSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ProviderFormContent(
                        sourceType = sourceType,
                        uiState = uiState,
                        name = name, onNameChange = { name = ProviderInputSanitizer.sanitizeProviderNameForEditing(it) },
                        serverUrl = serverUrl, onServerUrlChange = { serverUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                        username = username, onUsernameChange = { username = ProviderInputSanitizer.sanitizeUsernameForEditing(it) },
                        password = password, onPasswordChange = { password = ProviderInputSanitizer.sanitizePasswordForEditing(it) },
                        m3uUrl = m3uUrl, onM3uUrlChange = { m3uUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                        m3uEpgUrl = m3uEpgUrl, onM3uEpgUrlChange = { m3uEpgUrl = ProviderInputSanitizer.sanitizeUrlForEditing(it) },
                        httpUserAgent = httpUserAgent, onHttpUserAgentChange = { httpUserAgent = ProviderInputSanitizer.sanitizeHttpUserAgentForEditing(it) },
                        httpHeaders = httpHeaders, onHttpHeadersChange = { httpHeaders = ProviderInputSanitizer.sanitizeHttpHeadersForEditing(it) },
                        stalkerMacAddress = stalkerMacAddress, onStalkerMacAddressChange = { stalkerMacAddress = ProviderInputSanitizer.sanitizeMacAddressForEditing(it) },
                        stalkerDeviceProfile = stalkerDeviceProfile, onStalkerDeviceProfileChange = { stalkerDeviceProfile = ProviderInputSanitizer.sanitizeDeviceProfileForEditing(it) },
                        stalkerDeviceTimezone = stalkerDeviceTimezone, onStalkerDeviceTimezoneChange = { stalkerDeviceTimezone = ProviderInputSanitizer.sanitizeTimezoneForEditing(it) },
                        stalkerDeviceLocale = stalkerDeviceLocale, onStalkerDeviceLocaleChange = { stalkerDeviceLocale = ProviderInputSanitizer.sanitizeLocaleForEditing(it) },
                        fileImportError = fileImportError,
                        onFilePick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onLoginXtream = { viewModel.loginXtream(serverUrl, username, password, name, httpUserAgent, httpHeaders) },
                        onLoginStalker = { viewModel.loginStalker(serverUrl, stalkerMacAddress, name, stalkerDeviceProfile, stalkerDeviceTimezone, stalkerDeviceLocale) },
                        onAddM3u = { viewModel.addM3u(m3uUrl, name, httpUserAgent, httpHeaders, m3uEpgUrl) },
                        onToggleM3uVodClassification = { viewModel.updateM3uVodClassificationEnabled(!uiState.m3uVodClassificationEnabled) },
                        onToggleXtreamFastSync = { viewModel.updateXtreamFastSyncEnabled(!uiState.xtreamFastSyncEnabled) },
                        onSelectEpgSyncMode = viewModel::updateEpgSyncMode,
                        onSelectXtreamLiveSyncMode = viewModel::updateXtreamLiveSyncMode,
                        showImportBackupButton = !uiState.isEditing,
                        isImportingBackup = uiState.isImportingBackup || uiState.syncProgress != null,
                        onImportBackup = { backupImportLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }

    if (uiState.syncProgress != null) {
        SyncProgressDialog(message = uiState.syncProgress!!)
    }

    val backupPreview = uiState.backupPreview
    if (backupPreview != null && uiState.pendingBackupUri != null) {
        BackupImportPreviewDialog(
            preview = backupPreview,
            plan = uiState.backupImportPlan,
            onDismiss = { viewModel.dismissBackupPreview() },
            onStrategySelected = { viewModel.setBackupConflictStrategy(it) },
            onImportPreferencesChanged = { viewModel.setImportPreferences(it) },
            onImportProvidersChanged = { viewModel.setImportProviders(it) },
            onImportSavedLibraryChanged = { viewModel.setImportSavedLibrary(it) },
            onImportPlaybackHistoryChanged = { viewModel.setImportPlaybackHistory(it) },
            onImportMultiViewChanged = { viewModel.setImportMultiViewPresets(it) },
            onImportRecordingSchedulesChanged = { viewModel.setImportRecordingSchedules(it) },
            isImporting = uiState.isImportingBackup,
            onConfirm = { viewModel.confirmBackupImport() }
        )
    }

    if (showDiscardDraftDialog) {
        PremiumDialog(
            title = stringResource(R.string.setup_discard_draft_title),
            subtitle = stringResource(R.string.setup_discard_draft_body),
            onDismissRequest = { showDiscardDraftDialog = false },
            content = {},
            footer = {
                PremiumDialogFooterButton(
                    label = stringResource(R.string.setup_discard_draft_cancel),
                    onClick = { showDiscardDraftDialog = false }
                )
                PremiumDialogFooterButton(
                    label = stringResource(R.string.setup_discard_draft_confirm),
                    onClick = {
                        showDiscardDraftDialog = false
                        onBack()
                    },
                    emphasized = true
                )
            }
        )
    }

}

    @Composable
    internal fun ProviderSetupCompletionLayer(
        uiState: ProviderSetupState,
        knownLocalM3uUrls: Set<String>,
        selectedM3uUrl: String,
        filesDir: java.io.File,
        onProviderAdded: () -> Unit,
        onAttachCreatedProvider: () -> Unit,
        onSkipCreatedProviderCombinedAttach: () -> Unit,
        cleanupImportedFiles: suspend (java.io.File, Set<String>, Int) -> Unit = ::cleanupOldImportedM3uFilesAsync
    ) {
        val context = LocalContext.current
        LaunchedEffect(uiState.onboardingCompletion, uiState.completionWarning, uiState.pendingCombinedAttachProfileId) {
            if (
                uiState.onboardingCompletion != ProviderSetupViewModel.OnboardingCompletion.NONE &&
                uiState.pendingCombinedAttachProfileId == null
            ) {
                if (uiState.completionWarning != null) {
                    Toast.makeText(
                        context,
                        "Provider saved. Sync will resume in background.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                val previousLocal = uiState.m3uUrl.takeIf { it.startsWith("file://") }
                val selectedLocal = selectedM3uUrl.takeIf { it.startsWith("file://") }
                val protectedUris = buildSet {
                    knownLocalM3uUrls.forEach { knownUri ->
                        if (knownUri != previousLocal || previousLocal == selectedLocal) add(knownUri)
                    }
                    selectedLocal?.let(::add)
                }
                cleanupImportedFiles(filesDir, protectedUris, 20)
                onProviderAdded()
            }
        }

        if (uiState.pendingCombinedAttachProfileId != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onSkipCreatedProviderCombinedAttach,
                title = { Text("Add Playlist To Combined M3U?") },
                text = {
                    Text(
                        buildString {
                            append("Add ")
                            append(uiState.createdProviderName ?: "this playlist")
                            append(" to ")
                            append(uiState.pendingCombinedAttachProfileName ?: "the active combined source")
                            append(" and keep that combined source active for Live TV?")
                        },
                        color = OnSurface
                    )
                },
                confirmButton = {
                    TextButton(onClick = onAttachCreatedProvider) {
                        Text("Add To Combined", color = Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onSkipCreatedProviderCombinedAttach) {
                        Text("Not Now", color = OnSurface)
                    }
                }
            )
        }
    }

// Form content

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderFormContent(
    sourceType: SourceType,
    uiState: ProviderSetupState,
    name: String, onNameChange: (String) -> Unit,
    serverUrl: String, onServerUrlChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    m3uUrl: String, onM3uUrlChange: (String) -> Unit,
    m3uEpgUrl: String, onM3uEpgUrlChange: (String) -> Unit,
    httpUserAgent: String, onHttpUserAgentChange: (String) -> Unit,
    httpHeaders: String, onHttpHeadersChange: (String) -> Unit,
    stalkerMacAddress: String, onStalkerMacAddressChange: (String) -> Unit,
    stalkerDeviceProfile: String, onStalkerDeviceProfileChange: (String) -> Unit,
    stalkerDeviceTimezone: String, onStalkerDeviceTimezoneChange: (String) -> Unit,
    stalkerDeviceLocale: String, onStalkerDeviceLocaleChange: (String) -> Unit,
    fileImportError: String?,
    onFilePick: () -> Unit,
    onLoginXtream: () -> Unit,
    onLoginStalker: () -> Unit,
    onAddM3u: () -> Unit,
    onToggleM3uVodClassification: () -> Unit,
    onToggleXtreamFastSync: () -> Unit,
    onSelectEpgSyncMode: (ProviderEpgSyncMode) -> Unit,
    onSelectXtreamLiveSyncMode: (ProviderXtreamLiveSyncMode) -> Unit,
    showImportBackupButton: Boolean,
    isImportingBackup: Boolean,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val primaryActionText = when (sourceType) {
        SourceType.XTREAM,
        SourceType.STALKER -> when {
            uiState.isLoading -> androidx.compose.ui.res.stringResource(R.string.setup_connecting)
            uiState.isEditing -> androidx.compose.ui.res.stringResource(R.string.setup_save)
            else -> androidx.compose.ui.res.stringResource(R.string.setup_login)
        }
        SourceType.M3U_URL,
        SourceType.M3U_FILE -> when {
            uiState.isLoading -> androidx.compose.ui.res.stringResource(R.string.setup_validating)
            uiState.isEditing -> androidx.compose.ui.res.stringResource(R.string.setup_save)
            else -> androidx.compose.ui.res.stringResource(R.string.setup_add)
        }
    }
    val primaryAction = when (sourceType) {
        SourceType.XTREAM -> onLoginXtream
        SourceType.STALKER -> onLoginStalker
        SourceType.M3U_URL,
        SourceType.M3U_FILE -> onAddM3u
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = Border(border = BorderStroke(1.dp, SurfaceHighlight), shape = RoundedCornerShape(20.dp)),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated.copy(alpha = 0.95f))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val inlineActions = shouldInlineProviderSetupActions(
                isTelevisionDevice = isTelevisionDevice,
                viewportHeightDp = maxHeight.value.toInt()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .then(if (inlineActions) Modifier.navigationBarsPadding() else Modifier)
                    .padding(18.dp)
            ) {
                val showScrollHint by remember {
                    derivedStateOf { scrollState.value < scrollState.maxValue }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = if (showScrollHint) 34.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            if (maxWidth >= 560.dp) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SetupSectionBox(modifier = Modifier.weight(0.46f)) {
                                        SetupSummaryStrip(sourceType = sourceType, isEditing = uiState.isEditing)
                                    }
                                    SetupSectionBox(modifier = Modifier.weight(0.54f)) {
                                        ProviderTextField(
                                            value = name,
                                            onValueChange = onNameChange,
                                            placeholder = androidx.compose.ui.res.stringResource(R.string.setup_name_hint),
                                            modifier = Modifier.fillMaxWidth(0.72f)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SetupSectionBox {
                                        SetupSummaryStrip(sourceType = sourceType, isEditing = uiState.isEditing)
                                    }
                                    SetupSectionBox {
                                        ProviderTextField(
                                            value = name,
                                            onValueChange = onNameChange,
                                            placeholder = androidx.compose.ui.res.stringResource(R.string.setup_name_hint)
                                        )
                                    }
                                }
                            }
                        }

                        SetupSectionBox {
                            when (sourceType) {
                                SourceType.XTREAM -> {
                                ProviderTextField(
                                    value = serverUrl, onValueChange = onServerUrlChange,
                                    placeholder = androidx.compose.ui.res.stringResource(R.string.setup_server_hint),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = if (isTelevisionDevice) KeyboardType.Ascii else KeyboardType.Uri,
                                        imeAction = ImeAction.Next
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ProviderTextField(
                                        value = username, onValueChange = onUsernameChange,
                                        placeholder = androidx.compose.ui.res.stringResource(R.string.setup_user_hint),
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.None,
                                            autoCorrectEnabled = false,
                                            keyboardType = KeyboardType.Ascii,
                                            imeAction = ImeAction.Next
                                        )
                                    )
                                    ProviderTextField(
                                        value = password, onValueChange = onPasswordChange,
                                        placeholder = androidx.compose.ui.res.stringResource(R.string.setup_pass_hint),
                                        modifier = Modifier.weight(1f),
                                        isPassword = true,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.None,
                                            autoCorrectEnabled = false,
                                            keyboardType = if (isTelevisionDevice) KeyboardType.Ascii else KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        )
                                    )
                                }
                                AdvancedProviderOptionsSection(
                                    sourceType = sourceType,
                                    uiState = uiState,
                                    httpUserAgent = httpUserAgent,
                                    onHttpUserAgentChange = onHttpUserAgentChange,
                                    httpHeaders = httpHeaders,
                                    onHttpHeadersChange = onHttpHeadersChange,
                                    onToggleM3uVodClassification = onToggleM3uVodClassification,
                                    onToggleXtreamFastSync = onToggleXtreamFastSync,
                                    onSelectEpgSyncMode = onSelectEpgSyncMode,
                                    onSelectXtreamLiveSyncMode = onSelectXtreamLiveSyncMode,
                                    stalkerDeviceProfile = stalkerDeviceProfile,
                                    onStalkerDeviceProfileChange = onStalkerDeviceProfileChange,
                                    stalkerDeviceTimezone = stalkerDeviceTimezone,
                                    onStalkerDeviceTimezoneChange = onStalkerDeviceTimezoneChange,
                                    stalkerDeviceLocale = stalkerDeviceLocale,
                                    onStalkerDeviceLocaleChange = onStalkerDeviceLocaleChange
                                )
                                }

                                SourceType.STALKER -> {
                                ProviderTextField(
                                    value = serverUrl, onValueChange = onServerUrlChange,
                                    placeholder = "Portal URL",
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false,
                                        keyboardType = if (isTelevisionDevice) KeyboardType.Ascii else KeyboardType.Uri,
                                        imeAction = ImeAction.Next
                                    )
                                )
                                ProviderTextField(
                                    value = stalkerMacAddress, onValueChange = onStalkerMacAddressChange,
                                    placeholder = "MAC address",
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Next
                                    )
                                )
                                AdvancedProviderOptionsSection(
                                    sourceType = sourceType,
                                    uiState = uiState,
                                    httpUserAgent = httpUserAgent,
                                    onHttpUserAgentChange = onHttpUserAgentChange,
                                    httpHeaders = httpHeaders,
                                    onHttpHeadersChange = onHttpHeadersChange,
                                    onToggleM3uVodClassification = onToggleM3uVodClassification,
                                    onToggleXtreamFastSync = onToggleXtreamFastSync,
                                    onSelectEpgSyncMode = onSelectEpgSyncMode,
                                    onSelectXtreamLiveSyncMode = onSelectXtreamLiveSyncMode,
                                    stalkerDeviceProfile = stalkerDeviceProfile,
                                    onStalkerDeviceProfileChange = onStalkerDeviceProfileChange,
                                    stalkerDeviceTimezone = stalkerDeviceTimezone,
                                    onStalkerDeviceTimezoneChange = onStalkerDeviceTimezoneChange,
                                    stalkerDeviceLocale = stalkerDeviceLocale,
                                    onStalkerDeviceLocaleChange = onStalkerDeviceLocaleChange
                                )
                                }

                                SourceType.M3U_URL -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    com.afterglowtv.app.ui.components.ClipboardPasteButton(
                                        onPaste = { onM3uUrlChange(it) },
                                        label = "Paste M3U URL",
                                    )
                                    com.afterglowtv.app.ui.components.ClipboardCopyButton(
                                        text = m3uUrl,
                                        label = "Copy"
                                    )
                                    com.afterglowtv.app.ui.components.ClipboardClearButton(
                                        onClear = { onM3uUrlChange("") },
                                        label = "Clear",
                                        enabled = m3uUrl.isNotEmpty()
                                    )
                                }
                                ProviderTextField(
                                    value = m3uUrl, onValueChange = onM3uUrlChange,
                                    placeholder = androidx.compose.ui.res.stringResource(R.string.setup_m3u_hint),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    com.afterglowtv.app.ui.components.ClipboardPasteButton(
                                        onPaste = { onM3uEpgUrlChange(it) },
                                        label = "Paste EPG URL",
                                    )
                                    com.afterglowtv.app.ui.components.ClipboardCopyButton(
                                        text = m3uEpgUrl,
                                        label = "Copy"
                                    )
                                    com.afterglowtv.app.ui.components.ClipboardClearButton(
                                        onClear = { onM3uEpgUrlChange("") },
                                        label = "Clear",
                                        enabled = m3uEpgUrl.isNotEmpty()
                                    )
                                }
                                ProviderTextField(
                                    value = m3uEpgUrl, onValueChange = onM3uEpgUrlChange,
                                    placeholder = androidx.compose.ui.res.stringResource(R.string.setup_epg_url_hint),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
                                )
                                AdvancedProviderOptionsSection(
                                    sourceType = sourceType,
                                    uiState = uiState,
                                    httpUserAgent = httpUserAgent,
                                    onHttpUserAgentChange = onHttpUserAgentChange,
                                    httpHeaders = httpHeaders,
                                    onHttpHeadersChange = onHttpHeadersChange,
                                    onToggleM3uVodClassification = onToggleM3uVodClassification,
                                    onToggleXtreamFastSync = onToggleXtreamFastSync,
                                    onSelectEpgSyncMode = onSelectEpgSyncMode,
                                    onSelectXtreamLiveSyncMode = onSelectXtreamLiveSyncMode,
                                    stalkerDeviceProfile = stalkerDeviceProfile,
                                    onStalkerDeviceProfileChange = onStalkerDeviceProfileChange,
                                    stalkerDeviceTimezone = stalkerDeviceTimezone,
                                    onStalkerDeviceTimezoneChange = onStalkerDeviceTimezoneChange,
                                    stalkerDeviceLocale = stalkerDeviceLocale,
                                    onStalkerDeviceLocaleChange = onStalkerDeviceLocaleChange
                                )
                                }

                                SourceType.M3U_FILE -> {
                                FileSelectorCard(
                                    fileName = if (m3uUrl.startsWith("file://")) m3uUrl.substringAfterLast("/") else null,
                                    fileSelectedHint = androidx.compose.ui.res.stringResource(R.string.setup_file_replace_hint),
                                    emptySelectionTitle = androidx.compose.ui.res.stringResource(R.string.setup_file_select_title),
                                    emptySelectionHint = androidx.compose.ui.res.stringResource(R.string.setup_file_browse_hint),
                                    onClick = onFilePick
                                )
                                fileImportError?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
                                }
                                AdvancedProviderOptionsSection(
                                    sourceType = sourceType,
                                    uiState = uiState,
                                    httpUserAgent = httpUserAgent,
                                    onHttpUserAgentChange = onHttpUserAgentChange,
                                    httpHeaders = httpHeaders,
                                    onHttpHeadersChange = onHttpHeadersChange,
                                    onToggleM3uVodClassification = onToggleM3uVodClassification,
                                    onToggleXtreamFastSync = onToggleXtreamFastSync,
                                    onSelectEpgSyncMode = onSelectEpgSyncMode,
                                    onSelectXtreamLiveSyncMode = onSelectXtreamLiveSyncMode,
                                    stalkerDeviceProfile = stalkerDeviceProfile,
                                    onStalkerDeviceProfileChange = onStalkerDeviceProfileChange,
                                    stalkerDeviceTimezone = stalkerDeviceTimezone,
                                    onStalkerDeviceTimezoneChange = onStalkerDeviceTimezoneChange,
                                    stalkerDeviceLocale = stalkerDeviceLocale,
                                    onStalkerDeviceLocaleChange = onStalkerDeviceLocaleChange
                                )
                                }
                            }
                        }
                        FormErrors(uiState.validationError, uiState.error)
                        if (inlineActions) {
                            Spacer(modifier = Modifier.height(2.dp))
                            ProviderSetupActionButtons(
                                primaryActionText = primaryActionText,
                                isPrimaryLoading = uiState.isLoading,
                                onPrimaryAction = primaryAction,
                                showImportBackupButton = showImportBackupButton,
                                isImportingBackup = isImportingBackup,
                                onImportBackup = onImportBackup
                            )
                        }
                    }

                    if (showScrollHint) {
                        ScrollDownHint(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                }

                if (!inlineActions) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ProviderSetupActionButtons(
                        primaryActionText = primaryActionText,
                        isPrimaryLoading = uiState.isLoading,
                        onPrimaryAction = primaryAction,
                        showImportBackupButton = showImportBackupButton,
                        isImportingBackup = isImportingBackup,
                        onImportBackup = onImportBackup
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSetupActionButtons(
    primaryActionText: String,
    isPrimaryLoading: Boolean,
    onPrimaryAction: () -> Unit,
    showImportBackupButton: Boolean,
    isImportingBackup: Boolean,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (showImportBackupButton) {
            if (maxWidth < 430.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = primaryActionText,
                        isLoading = isPrimaryLoading,
                        onClick = onPrimaryAction
                    )
                    ActionButton(
                        text = androidx.compose.ui.res.stringResource(R.string.setup_import_backup),
                        isLoading = isImportingBackup,
                        onClick = onImportBackup
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = primaryActionText,
                        isLoading = isPrimaryLoading,
                        onClick = onPrimaryAction,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = androidx.compose.ui.res.stringResource(R.string.setup_import_backup),
                        isLoading = isImportingBackup,
                        onClick = onImportBackup,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            ActionButton(
                text = primaryActionText,
                isLoading = isPrimaryLoading,
                onClick = onPrimaryAction
            )
        }
    }
}

@Composable
private fun ScrollDownHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 28.dp)
            .background(com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.96f), RoundedCornerShape(8.dp))
            .border(1.dp, Primary.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 3.dp.toPx()
            drawLine(
                color = Primary,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.35f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.68f),
                strokeWidth = stroke
            )
            drawLine(
                color = Primary,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.35f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.68f),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
private fun SetupSectionBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceHighlight.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun SetupSummaryStrip(
    sourceType: SourceType,
    isEditing: Boolean
) {
    val title = when (sourceType) {
        SourceType.XTREAM -> stringResource(R.string.setup_info_xtream_title)
        SourceType.STALKER -> stringResource(R.string.setup_stalker)
        SourceType.M3U_URL,
        SourceType.M3U_FILE -> stringResource(R.string.setup_info_m3u_title)
    }
    val subtitle = when (sourceType) {
        SourceType.XTREAM -> stringResource(R.string.setup_info_xtream_body)
        SourceType.STALKER -> stringResource(R.string.setup_info_stalker_body)
        SourceType.M3U_URL -> stringResource(R.string.setup_info_m3u_body)
        SourceType.M3U_FILE -> stringResource(R.string.setup_file_browse_hint)
    }
    val badges = when (sourceType) {
        SourceType.XTREAM -> listOf(
            R.string.setup_badge_live_tv,
            R.string.setup_badge_fast_sync,
            R.string.setup_badge_epg
        )
        SourceType.STALKER -> listOf(
            R.string.setup_badge_live_tv,
            R.string.setup_badge_portal,
            R.string.badge_beta
        )
        SourceType.M3U_URL -> listOf(
            R.string.setup_badge_live_tv,
            R.string.setup_badge_vod_sorting,
            R.string.setup_badge_epg
        )
        SourceType.M3U_FILE -> listOf(
            R.string.setup_badge_local_file,
            R.string.setup_badge_vod_sorting,
            R.string.setup_badge_epg
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (isEditing) stringResource(R.string.setup_edit_provider) else title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badges.forEach { labelRes ->
                StatusPill(
                    label = stringResource(labelRes),
                    containerColor = SurfaceHighlight,
                    contentColor = TextPrimary,
                    horizontalPadding = 8.dp,
                    verticalPadding = 3.dp,
                    cornerRadius = 6.dp
                )
            }
        }
    }
}

@Composable
private fun AdvancedProviderOptionsSection(
    sourceType: SourceType,
    uiState: ProviderSetupState,
    httpUserAgent: String,
    onHttpUserAgentChange: (String) -> Unit,
    httpHeaders: String,
    onHttpHeadersChange: (String) -> Unit,
    onToggleM3uVodClassification: () -> Unit,
    onToggleXtreamFastSync: () -> Unit,
    onSelectEpgSyncMode: (ProviderEpgSyncMode) -> Unit,
    onSelectXtreamLiveSyncMode: (ProviderXtreamLiveSyncMode) -> Unit,
    stalkerDeviceProfile: String,
    onStalkerDeviceProfileChange: (String) -> Unit,
    stalkerDeviceTimezone: String,
    onStalkerDeviceTimezoneChange: (String) -> Unit,
    stalkerDeviceLocale: String,
    onStalkerDeviceLocaleChange: (String) -> Unit
) {
    var showAdvancedOptions by rememberSaveable(sourceType) { mutableStateOf(false) }
    val defaultEpgSyncMode = ProviderEpgSyncMode.BACKGROUND

    LaunchedEffect(
        uiState.isEditing,
        uiState.epgSyncMode,
        uiState.xtreamFastSyncEnabled,
        uiState.xtreamLiveSyncMode,
        sourceType
    ) {
        val hasNonDefaultSelection = ((sourceType == SourceType.XTREAM || sourceType == SourceType.STALKER) && uiState.epgSyncMode != defaultEpgSyncMode) ||
            (sourceType == SourceType.XTREAM && !uiState.xtreamFastSyncEnabled) ||
            (sourceType == SourceType.XTREAM && uiState.xtreamLiveSyncMode != ProviderXtreamLiveSyncMode.AUTO) ||
            ((sourceType == SourceType.M3U_URL || sourceType == SourceType.M3U_FILE) && uiState.m3uVodClassificationEnabled) ||
            ((sourceType == SourceType.XTREAM || sourceType == SourceType.M3U_URL || sourceType == SourceType.M3U_FILE) &&
                (httpUserAgent.isNotBlank() || httpHeaders.isNotBlank())) ||
            (sourceType == SourceType.STALKER && (stalkerDeviceProfile.isNotBlank() || stalkerDeviceTimezone.isNotBlank() || stalkerDeviceLocale.isNotBlank()))
        if (uiState.isEditing && hasNonDefaultSelection) {
            showAdvancedOptions = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            onClick = { showAdvancedOptions = !showAdvancedOptions },
            modifier = Modifier
                .fillMaxWidth()
                .mouseClickable(onClick = { showAdvancedOptions = !showAdvancedOptions }),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (showAdvancedOptions) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.95f) else SurfaceElevated,
                focusedContainerColor = com.afterglowtv.app.ui.design.AppColors.SurfaceAccent
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    BorderStroke(
                        1.dp,
                        if (showAdvancedOptions) Primary.copy(alpha = 0.45f) else SurfaceHighlight
                    )
                ),
                focusedBorder = Border(BorderStroke(3.dp, PrimaryLight))
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.setup_advanced_options_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.setup_advanced_options_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim
                    )
                }
                Text(
                    text = if (showAdvancedOptions) {
                        androidx.compose.ui.res.stringResource(R.string.setup_advanced_options_hide)
                    } else {
                        androidx.compose.ui.res.stringResource(R.string.setup_advanced_options_show)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryLight
                )
            }
        }

        AnimatedVisibility(visible = showAdvancedOptions) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sourceType == SourceType.M3U_URL || sourceType == SourceType.M3U_FILE) {
                    AdvancedSwitchOption(
                        title = stringResource(R.string.setup_m3u_vod_classification_label),
                        helper = stringResource(R.string.setup_m3u_vod_classification_helper),
                        checked = uiState.m3uVodClassificationEnabled,
                        onToggle = onToggleM3uVodClassification
                    )
                }

                if (sourceType == SourceType.XTREAM || sourceType == SourceType.M3U_URL || sourceType == SourceType.M3U_FILE) {
                    ProviderTextField(
                        value = httpUserAgent,
                        onValueChange = onHttpUserAgentChange,
                        placeholder = "User-Agent override (optional)",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                    ProviderTextField(
                        value = httpHeaders,
                        onValueChange = onHttpHeadersChange,
                        placeholder = "Custom headers (optional, Header: Value | Header2: Value)",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                if (sourceType == SourceType.XTREAM || sourceType == SourceType.STALKER) {
                    if (sourceType == SourceType.XTREAM) {
                        AdvancedSwitchOption(
                            title = stringResource(R.string.setup_xtream_fast_sync_label),
                            helper = stringResource(R.string.setup_xtream_fast_sync_helper),
                            checked = uiState.xtreamFastSyncEnabled,
                            onToggle = onToggleXtreamFastSync
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface, RoundedCornerShape(12.dp))
                                .border(1.dp, SurfaceHighlight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.setup_xtream_live_sync_mode_label),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.setup_xtream_live_sync_mode_helper),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDim
                            )
                            ProviderXtreamLiveSyncMode.entries.forEach { mode ->
                                XtreamLiveSyncModeOptionRow(
                                    mode = mode,
                                    selected = uiState.xtreamLiveSyncMode == mode,
                                    onSelect = { onSelectXtreamLiveSyncMode(mode) }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceHighlight, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.setup_epg_sync_mode_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                if (sourceType == SourceType.STALKER) {
                                    R.string.setup_stalker_epg_sync_mode_helper
                                } else {
                                    R.string.setup_epg_sync_mode_helper
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                        ProviderEpgSyncMode.entries.forEach { mode ->
                            EpgSyncModeOptionRow(
                                mode = mode,
                                sourceType = sourceType,
                                selected = uiState.epgSyncMode == mode,
                                onSelect = { onSelectEpgSyncMode(mode) }
                            )
                        }
                    }
                }

                if (sourceType == SourceType.STALKER) {
                    ProviderTextField(
                        value = stalkerDeviceProfile,
                        onValueChange = onStalkerDeviceProfileChange,
                        placeholder = "Device profile (optional)"
                    )
                    ProviderTextField(
                        value = stalkerDeviceTimezone,
                        onValueChange = onStalkerDeviceTimezoneChange,
                        placeholder = "Timezone (optional)"
                    )
                    ProviderTextField(
                        value = stalkerDeviceLocale,
                        onValueChange = onStalkerDeviceLocaleChange,
                        placeholder = "Locale (optional)"
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedSwitchOption(
    title: String,
    helper: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .mouseClickable(onClick = onToggle),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(
            containerColor = if (checked) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.95f) else SurfaceElevated,
            focusedContainerColor = com.afterglowtv.app.ui.design.AppColors.SurfaceAccent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(
                    1.dp,
                    if (checked) Primary.copy(alpha = 0.4f) else SurfaceHighlight
                )
            ),
            focusedBorder = Border(BorderStroke(3.dp, PrimaryLight))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun XtreamLiveSyncModeOptionRow(
    mode: ProviderXtreamLiveSyncMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val titleRes = when (mode) {
        ProviderXtreamLiveSyncMode.AUTO -> R.string.setup_xtream_live_sync_mode_auto_title
        ProviderXtreamLiveSyncMode.CATEGORY_BY_CATEGORY -> R.string.setup_xtream_live_sync_mode_category_title
        ProviderXtreamLiveSyncMode.STREAM_ALL -> R.string.setup_xtream_live_sync_mode_stream_all_title
    }
    val descriptionRes = when (mode) {
        ProviderXtreamLiveSyncMode.AUTO -> R.string.setup_xtream_live_sync_mode_auto_description
        ProviderXtreamLiveSyncMode.CATEGORY_BY_CATEGORY -> R.string.setup_xtream_live_sync_mode_category_description
        ProviderXtreamLiveSyncMode.STREAM_ALL -> R.string.setup_xtream_live_sync_mode_stream_all_description
    }
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .mouseClickable(onClick = onSelect),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.95f) else Color.Transparent,
            focusedContainerColor = com.afterglowtv.app.ui.design.AppColors.SurfaceAccent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(
                    1.dp,
                    if (selected) Primary.copy(alpha = 0.45f) else Color.Transparent
                )
            ),
            focusedBorder = Border(BorderStroke(3.dp, PrimaryLight))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }
        }
    }
}

@Composable
private fun EpgSyncModeOptionRow(
    mode: ProviderEpgSyncMode,
    sourceType: SourceType,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val titleRes = when (mode) {
        ProviderEpgSyncMode.UPFRONT -> R.string.setup_epg_sync_mode_upfront_title
        ProviderEpgSyncMode.BACKGROUND -> R.string.setup_epg_sync_mode_background_title
        ProviderEpgSyncMode.SKIP -> R.string.setup_epg_sync_mode_skip_title
    }
    val descriptionRes = when (mode) {
        ProviderEpgSyncMode.UPFRONT -> if (sourceType == SourceType.STALKER) {
            R.string.setup_stalker_epg_sync_mode_upfront_description
        } else {
            R.string.setup_epg_sync_mode_upfront_description
        }
        ProviderEpgSyncMode.BACKGROUND -> if (sourceType == SourceType.STALKER) {
            R.string.setup_stalker_epg_sync_mode_background_description
        } else {
            R.string.setup_epg_sync_mode_background_description
        }
        ProviderEpgSyncMode.SKIP -> if (sourceType == SourceType.STALKER) {
            R.string.setup_stalker_epg_sync_mode_skip_description
        } else {
            R.string.setup_epg_sync_mode_skip_description
        }
    }
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .mouseClickable(onClick = onSelect),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.95f) else Color.Transparent,
            focusedContainerColor = com.afterglowtv.app.ui.design.AppColors.SurfaceAccent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(
                    1.dp,
                    if (selected) Primary.copy(alpha = 0.45f) else Color.Transparent
                )
            ),
            focusedBorder = Border(BorderStroke(3.dp, PrimaryLight))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }
        }
    }
}

@Composable
private fun FormErrors(validationError: String?, error: String?) {
    validationError?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
    }
    error?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
    }
}

// Source type selector: wide layout

@Composable
private fun SourceTypeSelectorPanel(
    sourceType: SourceType,
    isEditing: Boolean,
    developerModeEnabled: Boolean,
    isEditLabel: String,
    onSelect: (SourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = Border(border = BorderStroke(1.dp, SurfaceHighlight.copy(alpha = 0.95f)), shape = RoundedCornerShape(20.dp)),
        colors = SurfaceDefaults.colors(containerColor = Surface.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = isEditLabel,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setup_shell_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setup_source_type_label),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            if ((developerModeEnabled && !isEditing) || sourceType == SourceType.XTREAM) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_xtream),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_info_xtream_body),
                    selected = sourceType == SourceType.XTREAM,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.XTREAM) }
                )
            }
            if ((developerModeEnabled && !isEditing) || sourceType == SourceType.STALKER) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_stalker),
                    badge = androidx.compose.ui.res.stringResource(R.string.badge_beta),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_info_stalker_body),
                    selected = sourceType == SourceType.STALKER,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.STALKER) }
                )
            }
            if (!isEditing || sourceType == SourceType.M3U_URL) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_tab_url),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_info_m3u_body),
                    selected = sourceType == SourceType.M3U_URL,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.M3U_URL) }
                )
            }
            if (!isEditing || sourceType == SourceType.M3U_FILE) {
                SourceTypeCard(
                    title = androidx.compose.ui.res.stringResource(R.string.setup_tab_file),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.setup_file_browse_hint),
                    selected = sourceType == SourceType.M3U_FILE,
                    enabled = !isEditing,
                    onClick = { onSelect(SourceType.M3U_FILE) }
                )
            }
        }
    }
}

@Composable
private fun SourceTypeCard(
    title: String,
    badge: String? = null,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .mouseClickable(enabled = enabled, onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor  = if (selected) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.72f) else SurfaceElevated,
            focusedContainerColor = com.afterglowtv.app.ui.design.AppColors.SurfaceAccent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) Primary else Color.Transparent
                )
            ),
            focusedBorder = Border(BorderStroke(2.dp, FocusBorder))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
                badge?.let {
                    StatusPill(
                        label = it,
                        containerColor = AccentAmber,
                        contentColor = Color.Black,
                        horizontalPadding = 6.dp,
                        verticalPadding = 2.dp,
                        cornerRadius = 6.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim, maxLines = 1)
        }
    }
}

// Source type selector: narrow layout

@Composable
private fun SourceTypeTabRow(
    sourceType: SourceType,
    isEditing: Boolean,
    developerModeEnabled: Boolean,
    onSelect: (SourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if ((developerModeEnabled && !isEditing) || sourceType == SourceType.XTREAM) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_xtream),
                isSelected = sourceType == SourceType.XTREAM,
                onClick = { if (!isEditing) onSelect(SourceType.XTREAM) }
            )
        }
        if ((developerModeEnabled && !isEditing) || sourceType == SourceType.STALKER) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_stalker),
                badge = androidx.compose.ui.res.stringResource(R.string.badge_beta),
                isSelected = sourceType == SourceType.STALKER,
                onClick = { if (!isEditing) onSelect(SourceType.STALKER) }
            )
        }
        if (!isEditing || sourceType == SourceType.M3U_URL) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_tab_url),
                isSelected = sourceType == SourceType.M3U_URL,
                onClick = { if (!isEditing) onSelect(SourceType.M3U_URL) }
            )
        }
        if (!isEditing || sourceType == SourceType.M3U_FILE) {
            TabButton(
                text = androidx.compose.ui.res.stringResource(R.string.setup_tab_file),
                isSelected = sourceType == SourceType.M3U_FILE,
                onClick = { if (!isEditing) onSelect(SourceType.M3U_FILE) }
            )
        }
    }
}

// ProviderTextField
//
// Key fix: uses BasicTextField with decorationBox and tracks focus via
// onFocusEvent { it.hasFocus } is true when this node OR any
// descendant (the actual cursor/text composable) has focus. The old approach
// used onFocusChanged { isFocused } on an outer Box, which became false the
// moment the inner BasicTextField took focus, breaking keyboard scroll.

@Composable
private fun ProviderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isTelevisionDevice = rememberIsTelevisionDevice()
    var hasContainerFocus by remember { mutableStateOf(false) }
    var hasInputFocus by remember { mutableStateOf(false) }
    var acceptsInput by remember(isTelevisionDevice) { mutableStateOf(!isTelevisionDevice) }
    var pendingInputActivation by remember { mutableStateOf(false) }
    var editBaselineValue by remember { mutableStateOf(value) }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var isPasswordVisible by rememberSaveable(isPassword) { mutableStateOf(false) }
    var revealedPasswordIndex by remember { mutableStateOf<Int?>(null) }
    var previousValue by remember { mutableStateOf(value) }
    val containerFocusRequester = remember { FocusRequester() }
    val inputFocusRequester = remember { FocusRequester() }
    val visibilityToggleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val isFocused = hasContainerFocus || hasInputFocus
    val passwordVisibilityDescription = if (isPassword) {
        androidx.compose.ui.res.stringResource(
            if (isPasswordVisible) R.string.setup_hide_password else R.string.setup_show_password
        )
    } else {
        null
    }

    fun activateInput() {
        if (!acceptsInput) {
            editBaselineValue = value
        }
        if (!isTelevisionDevice) {
            acceptsInput = true
            inputFocusRequester.requestFocus()
            keyboardController?.show()
            coroutineScope.launch {
                runCatching { bringIntoViewRequester.bringIntoView() }
                delay(180)
                runCatching { bringIntoViewRequester.bringIntoView() }
            }
            return
        }
        acceptsInput = true
        pendingInputActivation = true
        coroutineScope.launch {
            runCatching { bringIntoViewRequester.bringIntoView() }
        }
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            val coercedSelectionStart = fieldValue.selection.start.coerceIn(0, value.length)
            val coercedSelectionEnd = fieldValue.selection.end.coerceIn(0, value.length)
            val coercedComposition = fieldValue.composition?.let { composition ->
                val compositionStart = composition.start.coerceIn(0, value.length)
                val compositionEnd = composition.end.coerceIn(0, value.length)
                if (compositionStart <= compositionEnd) {
                    TextRange(compositionStart, compositionEnd)
                } else {
                    null
                }
            }
            fieldValue = fieldValue.copy(
                text = value,
                selection = TextRange(coercedSelectionStart, coercedSelectionEnd),
                composition = coercedComposition
            )
        }
    }

    LaunchedEffect(acceptsInput, pendingInputActivation) {
        if (!isTelevisionDevice || !acceptsInput || !pendingInputActivation) {
            return@LaunchedEffect
        }
        inputFocusRequester.requestFocus()
        keyboardController?.show()
        coroutineScope.launch {
            delay(120)
            runCatching { bringIntoViewRequester.bringIntoView() }
        }
        pendingInputActivation = false
    }

    LaunchedEffect(isPassword) {
        if (!isPassword) {
            isPasswordVisible = false
        }
    }

    // Show most-recently typed character briefly before masking
    LaunchedEffect(value, isPassword) {
        if (!isPassword) { previousValue = value; revealedPasswordIndex = null; return@LaunchedEffect }
        revealedPasswordIndex = when {
            value.length > previousValue.length && value.isNotEmpty() -> value.lastIndex
            value.isEmpty() -> null
            else -> revealedPasswordIndex?.takeIf { it < value.length }
        }
        previousValue = value
    }
    LaunchedEffect(revealedPasswordIndex, value, isPassword) {
        val idx = revealedPasswordIndex ?: return@LaunchedEffect
        if (!isPassword || idx >= value.length) return@LaunchedEffect
        delay(1500)
        if (revealedPasswordIndex == idx) revealedPasswordIndex = null
    }

    val borderColor by animateColorAsState(
        if (isFocused) com.afterglowtv.app.ui.design.AppColors.Live else Primary.copy(alpha = 0.55f),
        tween(150),
        label = "border"
    )
    val bgColor by animateColorAsState(
        if (isFocused) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent else Surface.copy(alpha = 0.92f),
        tween(150),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .height(64.dp)
            .focusRequester(containerFocusRequester)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent {
                hasContainerFocus = it.hasFocus
                if (!it.hasFocus && isTelevisionDevice) {
                    acceptsInput = false
                    keyboardController?.hide()
                }
            }
            .mouseClickable(focusRequester = containerFocusRequester, onClick = ::activateInput)
            .clickable(onClick = ::activateInput)
            .focusable()
            .padding(0.dp)
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = fieldValue,
            onValueChange = { updatedValue ->
                fieldValue = updatedValue
                if (updatedValue.text != value) {
                    onValueChange(updatedValue.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(inputFocusRequester)
                .focusProperties {
                    canFocus = !isTelevisionDevice || acceptsInput
                    if (isTelevisionDevice && acceptsInput) {
                        left = FocusRequester.Cancel
                        right = if (isPassword) visibilityToggleFocusRequester else FocusRequester.Cancel
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (!isTelevisionDevice || !acceptsInput || event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                        return@onPreviewKeyEvent false
                    }
                    val cursor = fieldValue.selection.end
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            fieldValue = TextFieldValue(
                                text = editBaselineValue,
                                selection = TextRange(editBaselineValue.length)
                            )
                            if (editBaselineValue != value) {
                                onValueChange(editBaselineValue)
                            }
                            acceptsInput = false
                            pendingInputActivation = false
                            keyboardController?.hide()
                            containerFocusRequester.requestFocus()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val nextCursor = (cursor - 1).coerceAtLeast(0)
                            fieldValue = fieldValue.copy(selection = TextRange(nextCursor))
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isPassword && cursor >= fieldValue.text.length) {
                                visibilityToggleFocusRequester.requestFocus()
                                return@onPreviewKeyEvent true
                            }
                            val nextCursor = (cursor + 1).coerceAtMost(fieldValue.text.length)
                            fieldValue = fieldValue.copy(selection = TextRange(nextCursor))
                            true
                        }
                        else -> false
                    }
                }
                .onFocusEvent {
                    hasInputFocus = it.hasFocus
                    if (it.hasFocus) {
                        coroutineScope.launch {
                            delay(120)
                            runCatching { bringIntoViewRequester.bringIntoView() }
                        }
                    } else if (isTelevisionDevice) {
                        keyboardController?.hide()
                    }
                },
            singleLine = true,
            readOnly = isTelevisionDevice && !acceptsInput,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnBackground),
            visualTransformation = when {
                !isPassword || isPasswordVisible -> VisualTransformation.None
                else -> RevealingPasswordVisualTransformation(revealedPasswordIndex)
            },
            cursorBrush = SolidColor(Primary),
            decorationBox = { innerTextField ->
                val fieldShape = RoundedCornerShape(6.dp)
                val stripeWidth: androidx.compose.ui.unit.Dp = if (isFocused) 6.dp else 4.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor, fieldShape)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = borderColor,
                            shape = fieldShape,
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(stripeWidth)
                            .fillMaxHeight()
                            .background(
                                color = if (isFocused) com.afterglowtv.app.ui.design.AppColors.Live else Primary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                            ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = stripeWidth + 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                            }
                            innerTextField()
                        }

                        if (isPassword) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(24.dp)
                                    .focusRequester(visibilityToggleFocusRequester)
                                    .focusProperties {
                                        canFocus = !isTelevisionDevice || acceptsInput
                                        left = inputFocusRequester
                                    }
                                    .onFocusEvent {
                                        if (it.hasFocus) {
                                            coroutineScope.launch {
                                                delay(120)
                                                runCatching { bringIntoViewRequester.bringIntoView() }
                                            }
                                        }
                                    }
                                    .semantics {
                                        contentDescription = passwordVisibilityDescription.orEmpty()
                                    }
                                    .clickable {
                                        isPasswordVisible = !isPasswordVisible
                                    }
                                    .mouseClickable(focusRequester = visibilityToggleFocusRequester) {
                                        isPasswordVisible = !isPasswordVisible
                                    }
                                    .focusable(enabled = !isTelevisionDevice || acceptsInput)
                            ) {
                                PasswordVisibilityGlyph(
                                    isVisible = isPasswordVisible,
                                    tint = if (isFocused) Primary else OnSurfaceDim,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun PasswordVisibilityGlyph(
    isVisible: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.11f)
        drawOval(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(1.6f, size.height * 0.22f),
            size = androidx.compose.ui.geometry.Size(size.width - 3.2f, size.height * 0.56f),
            style = stroke
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.14f,
            center = center
        )
        if (!isVisible) {
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.82f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f),
                strokeWidth = size.minDimension * 0.11f
            )
        }
    }
}

// Sync progress dialog

@Composable
fun SyncProgressDialog(message: String) {
    PremiumDialog(
        title = androidx.compose.ui.res.stringResource(R.string.settings_syncing_title),
        subtitle = androidx.compose.ui.res.stringResource(R.string.settings_syncing_subtitle),
        onDismissRequest = {},
        widthFraction = 0.32f,
        heightFraction = null,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Primary)
                StatusPill(
                    label = androidx.compose.ui.res.stringResource(R.string.settings_syncing_btn),
                    containerColor = PrimaryGlow
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// ActionButton

@Composable
private fun ActionButton(
    text: String,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProviderActionButton(
        text = text,
        height = 52.dp,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun SmallActionButton(
    text: String,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProviderActionButton(
        text = text,
        height = 40.dp,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun ProviderActionButton(
    text: String,
    height: androidx.compose.ui.unit.Dp,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.03f else 1f, tween(150), label = "scale")
    val buttonShape = afterglowButtonShape(AppStyles.value.button)

    Surface(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .afterglow(
                specs = if (isFocused && !isLoading) listOf(
                    GlowSpec(com.afterglowtv.app.ui.design.AppColors.TiviAccent, 14.dp, 0.65f),
                    GlowSpec(com.afterglowtv.app.ui.design.AppColors.EpgNowLine, 28.dp, 0.35f),
                ) else if (!isLoading) listOf(
                    GlowSpec(com.afterglowtv.app.ui.design.AppColors.TiviAccent, 8.dp, 0.30f),
                ) else emptyList(),
                shape = buttonShape,
            )
            .onFocusEvent { isFocused = it.hasFocus }
            .mouseClickable(enabled = !isLoading, onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(buttonShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (!isLoading) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.95f) else SurfaceHighlight,
            focusedContainerColor = if (!isLoading) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent else SurfaceHighlight,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, com.afterglowtv.app.ui.design.AppColors.TiviAccent.copy(alpha = 0.45f)),
                shape = buttonShape,
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, com.afterglowtv.app.ui.design.AppColors.TiviAccent),
                shape = buttonShape,
            ),
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = OnBackground.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                    ),
                    color = com.afterglowtv.app.ui.design.AppColors.TextPrimary,
                )
            }
        }
    }
}

// FileSelectorCard

@Composable
private fun FileSelectorCard(
    fileName: String?,
    fileSelectedHint: String,
    emptySelectionTitle: String,
    emptySelectionHint: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) Primary else SurfaceHighlight
    val bgColor     = if (isFocused) Surface  else SurfaceElevated

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp).onFocusEvent { isFocused = it.hasFocus }.mouseClickable(onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, borderColor)),
            focusedBorder = Border(BorderStroke(2.dp, FocusBorder))
        ),
        colors = ClickableSurfaceDefaults.colors(containerColor = bgColor, focusedContainerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fileName != null) {
                Text(text = fileName, style = MaterialTheme.typography.bodyLarge, color = OnBackground, textAlign = TextAlign.Center)
                Text(text = fileSelectedHint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            } else {
                Text(text = emptySelectionTitle, style = MaterialTheme.typography.bodyLarge, color = OnBackground)
                Text(text = emptySelectionHint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            }
        }
    }
}

// TabButton

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, badge: String? = null) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.onFocusEvent { isFocused = it.hasFocus }.mouseClickable(onClick = onClick),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) com.afterglowtv.app.ui.design.AppColors.SurfaceAccent.copy(alpha = 0.72f) else SurfaceElevated,
            focusedContainerColor = com.afterglowtv.app.ui.design.AppColors.SurfaceAccent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, SurfaceHighlight.copy(alpha = if (isSelected) 0.95f else 0.65f))),
            focusedBorder = Border(BorderStroke(2.dp, FocusBorder))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) TextPrimary else OnSurface
            )
            badge?.let {
                StatusPill(
                    label = it,
                    containerColor = AccentAmber,
                    contentColor = Color.Black,
                    horizontalPadding = 6.dp,
                    verticalPadding = 2.dp,
                    cornerRadius = 6.dp
                )
            }
        }
    }
}

// ─── Revealing password visual transformation ─────────────────────────────────

private class RevealingPasswordVisualTransformation(
    private val revealedIndex: Int?
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = buildString(text.length) {
            text.text.forEachIndexed { index, ch ->
                append(if (revealedIndex != null && index == revealedIndex) ch else '*')
            }
        }
        return TransformedText(AnnotatedString(transformed), OffsetMapping.Identity)
    }
}

// ─── File cleanup helpers ─────────────────────────────────────────────────────

private fun cleanupOldImportedM3uFiles(
    filesDir: java.io.File,
    protectedFileUris: Set<String>,
    keepLatest: Int
) {
    val protectedPaths = protectedFileUris
        .mapNotNull { it.removePrefix("file://").takeIf { p -> p.isNotBlank() } }
        .toSet()

    val importedFiles = filesDir
        .listFiles { file ->
            file.isFile && file.name.startsWith("m3u_") &&
                (file.name.endsWith(".m3u") || file.name.endsWith(".m3u8"))
        }
        ?.sortedByDescending { it.lastModified() }
        ?: return

    importedFiles.drop(keepLatest).forEach { stale ->
        if (stale.absolutePath !in protectedPaths) runCatching { stale.delete() }
    }
}

private suspend fun cleanupOldImportedM3uFilesAsync(
    filesDir: java.io.File,
    protectedFileUris: Set<String>,
    keepLatest: Int
) = withContext(Dispatchers.IO) {
    cleanupOldImportedM3uFiles(filesDir, protectedFileUris, keepLatest)
}

private fun resolveFileImportError(context: android.content.Context, error: Throwable): String {
    val msg = error.message.orEmpty()
    val isStorageFull = msg.contains("ENOSPC", ignoreCase = true) ||
        msg.contains("no space", ignoreCase = true) ||
        msg.contains("space left", ignoreCase = true)
    return if (isStorageFull) context.getString(R.string.setup_file_import_storage_full)
           else context.getString(R.string.setup_file_import_failed)
}
