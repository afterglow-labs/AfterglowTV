package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.afterglowtv.app.backup.BackupFileBridge
import com.afterglowtv.app.diagnostics.CrashReportStore
import com.afterglowtv.app.util.OfficialBuildVerifier
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.theme.*
import com.afterglowtv.domain.model.Provider
import androidx.compose.ui.res.stringResource
import com.afterglowtv.app.R
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.ui.design.requestFocusSafely
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import kotlinx.coroutines.delay


@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onAddProvider: (ProviderM3uPlaylistKind?) -> Unit = {},
    onEditProvider: (Provider) -> Unit = {},
    onNavigateToParentalControl: (Long) -> Unit = {},
    onReturnToPlayer: () -> Unit = {},
    currentRoute: String,
    initialBackupImportUri: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsNavFocusRequester = remember { FocusRequester() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainActivity = context.findMainActivity()
    val officialBuildVerification = remember(context.packageName) { OfficialBuildVerifier.verify(context) }
    val screenLabels = rememberSettingsScreenLabels(
        uiState = uiState,
        context = context,
        officialBuildStatus = officialBuildVerification.status
    )
    val dialogState = rememberSettingsScreenDialogState()
    val providerState = rememberSettingsProviderSectionState(dialogState)
    var handledInitialBackupImportUri by remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.inspectBackup(it.toString()) }
    }

    fun exportBackupToDownloads() {
        val destination = runCatching { BackupFileBridge.createDownloadsExport(context) }.getOrNull()
        if (destination == null) {
            viewModel.showUserMessage(context.getString(R.string.settings_backup_export_prepare_failed))
            return
        }
        viewModel.exportConfig(
            uriString = destination.uri.toString(),
            successMessage = context.getString(
                R.string.settings_backup_export_saved_to,
                destination.displayLocation
            )
        )
    }

    fun shareBackup() {
        val file = runCatching { BackupFileBridge.createExportFile(context) }.getOrNull()
        if (file == null) {
            viewModel.showUserMessage(context.getString(R.string.settings_backup_share_prepare_failed))
            return
        }
        val uri = BackupFileBridge.providerUriForFile(context, file)
        viewModel.exportConfig(uri.toString()) {
            runCatching { context.startActivity(BackupFileBridge.buildShareIntent(uri)) }
                .onFailure { viewModel.showUserMessage(context.getString(R.string.settings_backup_share_failed)) }
        }
    }

    fun shareCrashReport() {
        val file = CrashReportStore.latestReportFile(context)
        if (!file.isFile || file.length() <= 0L) {
            viewModel.showUserMessage(context.getString(R.string.settings_crash_report_missing))
            viewModel.refreshCrashReport()
            return
        }
        val uri = CrashReportStore.providerUriForFile(context, file)
        runCatching { context.startActivity(CrashReportStore.buildShareIntent(uri)) }
            .onFailure { viewModel.showUserMessage(context.getString(R.string.settings_crash_report_share_failed)) }
    }

    val recordingFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val displayName = DocumentFile.fromTreeUri(context, it)?.name
            viewModel.updateRecordingFolder(it.toString(), displayName)
        }
    }

    val localMediaFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = DocumentFile.fromTreeUri(context, it)?.name
            viewModel.addLocalMediaLibrary(it.toString(), displayName)
        }
    }

    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.userMessageShown()
        }
    }

    LaunchedEffect(uiState.recordingItems) {
        dialogState.selectedRecordingId = when {
            uiState.recordingItems.isEmpty() -> null
            dialogState.selectedRecordingId == null -> uiState.recordingItems.first().id
            uiState.recordingItems.any { item -> item.id == dialogState.selectedRecordingId } -> dialogState.selectedRecordingId
            else -> uiState.recordingItems.first().id
        }
    }

    LaunchedEffect(initialBackupImportUri) {
        val uri = initialBackupImportUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (handledInitialBackupImportUri == uri) return@LaunchedEffect
        handledInitialBackupImportUri = uri
        dialogState.selectedCategory = SETTINGS_CATEGORY_BACKUP
        viewModel.inspectBackup(uri)
    }

    LaunchedEffect(uiState.developerModeEnabled, dialogState.selectedCategory) {
        val policy = StorePolicy.currentFor(uiState.developerModeEnabled)
        val effectiveDeveloperModeEnabled = StorePolicy.effectiveDeveloperModeEnabled(uiState.developerModeEnabled)
        if (
            dialogState.selectedCategory !in visibleSettingsCategoryIds(
                policy = policy,
                developerModeEnabled = effectiveDeveloperModeEnabled
            )
        ) {
            dialogState.selectedCategory = SETTINGS_CATEGORY_PROVIDERS
        }
    }

    LaunchedEffect(currentRoute, dialogState.selectedCategory) {
        delay(80)
        settingsNavFocusRequester.requestFocusSafely(tag = "SettingsScreen", target = "Selected settings section")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenScaffold(
            currentRoute = currentRoute,
            onNavigate = { if (!uiState.isSyncing) onNavigate(it) },
            title = stringResource(R.string.settings_title),
            subtitle = "",
            navigationChrome = AppNavigationChrome.TopBar,
            compactHeader = true,
            showScreenHeader = false
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsNavigationRail(
                    selectedCategory = dialogState.selectedCategory,
                    developerModeEnabled = uiState.developerModeEnabled,
                    focusRequester = settingsNavFocusRequester,
                    onCategorySelected = { dialogState.selectedCategory = it },
                    onNavigate = onNavigate,
                )

                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.07f))
                )

                SettingsContentPane(
                    uiState = uiState,
                    viewModel = viewModel,
                    context = context,
                    screenLabels = screenLabels,
                    dialogState = dialogState,
                    providerState = providerState,
                    onAddProvider = onAddProvider,
                    onEditProvider = onEditProvider,
                    onNavigateToParentalControl = onNavigateToParentalControl,
                    onChooseRecordingFolder = { recordingFolderLauncher.launch(null) },
                    onChooseLocalMediaLibrary = { localMediaFolderLauncher.launch(null) },
                    onCreateBackup = ::exportBackupToDownloads,
                    onShareBackup = ::shareBackup,
                    onViewCrashReport = viewModel::viewCrashReport,
                    onShareCrashReport = ::shareCrashReport,
                    onDeleteCrashReport = viewModel::deleteCrashReport,
                    onRestoreBackup = {
                        openDocumentLauncher.launch(
                            arrayOf("application/json", "text/json", "application/x-json", "application/octet-stream", "*/*")
                        )
                    },
                    onOpenUri = uriHandler::openUri,
                    modifier = Modifier.weight(1f)
                )
                SettingsNowPlayingSidecar(
                    onReturnToPlayer = onReturnToPlayer,
                    onEnterPictureInPicture = {
                        mainActivity?.enterPlayerPictureInPictureModeFromPlayer()
                        Unit
                    }
                )
            }
        }

        SettingsScreenOverlays(
            snackbarHostState = snackbarHostState,
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            scope = scope,
            dialogState = dialogState,
            mainActivity = mainActivity,
            currentRoute = currentRoute,
            modifier = Modifier
        )
    }
}
